package banco.repository;

import banco.model.Conta;
import banco.model.Transacao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Todo o SQL do sistema fica aqui. */
public class BancoRepository {

    private final Connection conn;

    public BancoRepository(Connection conn) {
        this.conn = conn;
        criarTabelas();
    }

    private void criarTabelas() {
        try (Statement stmt = conn.createStatement()) {
            // dinheiro vai como TEXT ("1500.00"); REAL é ponto flutuante e perde precisão
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contas (
                    id         TEXT PRIMARY KEY,
                    titular    TEXT NOT NULL,
                    cpf        TEXT UNIQUE NOT NULL,
                    tipo       TEXT NOT NULL,
                    saldo      TEXT NOT NULL DEFAULT '0.00',
                    ativa      INTEGER NOT NULL DEFAULT 1,
                    criado_em  TEXT NOT NULL
                )
            """);

            // conta_origem é a conta dona da linha no extrato;
            // conta_destino é a outra conta numa transferência (ou NULL)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transacoes (
                    id              TEXT PRIMARY KEY,
                    conta_origem    TEXT NOT NULL REFERENCES contas(id),
                    conta_destino   TEXT,
                    tipo            TEXT NOT NULL,
                    valor           TEXT NOT NULL,
                    saldo_apos      TEXT NOT NULL,
                    descricao       TEXT,
                    realizado_em    TEXT NOT NULL
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transacoes_conta ON transacoes(conta_origem)");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar tabelas: " + e.getMessage(), e);
        }
    }

    /** Roda tudo numa transação: se der exceção no meio, faz rollback. */
    public <T> T emTransacao(Supplier<T> operacao) {
        try {
            boolean autoCommitAnterior = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T resultado = operacao.get();
                conn.commit();
                return resultado;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitAnterior);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro na transação: " + e.getMessage(), e);
        }
    }

    public void salvarConta(Conta conta) {
        String sql = """
            INSERT INTO contas (id, titular, cpf, tipo, saldo, ativa, criado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, conta.getId());
            ps.setString(2, conta.getTitular());
            ps.setString(3, conta.getCpf());
            ps.setString(4, conta.getTipo().name());
            ps.setString(5, conta.getSaldo().toPlainString());
            ps.setInt(6, conta.isAtiva() ? 1 : 0);
            ps.setString(7, conta.getCriadoEm().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar conta: " + e.getMessage(), e);
        }
    }

    public void atualizarConta(Conta conta) {
        String sql = "UPDATE contas SET saldo = ?, ativa = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, conta.getSaldo().toPlainString());
            ps.setInt(2, conta.isAtiva() ? 1 : 0);
            ps.setString(3, conta.getId());
            if (ps.executeUpdate() != 1) {
                // se nada mudou, melhor dar erro do que sumir com o dinheiro em silêncio
                throw new IllegalStateException("Conta não encontrada para atualizar: " + conta.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar conta: " + e.getMessage(), e);
        }
    }

    public Optional<Conta> buscarPorId(String id) {
        return buscarUma("SELECT * FROM contas WHERE id = ?", id);
    }

    public Optional<Conta> buscarPorCpf(String cpf) {
        return buscarUma("SELECT * FROM contas WHERE cpf = ?", cpf);
    }

    private Optional<Conta> buscarUma(String sql, String parametro) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearConta(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta: " + e.getMessage(), e);
        }
    }

    public List<Conta> listarTodas() {
        List<Conta> contas = new ArrayList<>();
        String sql = "SELECT * FROM contas ORDER BY titular";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                contas.add(mapearConta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar contas: " + e.getMessage(), e);
        }
        return contas;
    }

    public void salvarTransacao(Transacao t) {
        String sql = """
            INSERT INTO transacoes (id, conta_origem, conta_destino, tipo, valor, saldo_apos, descricao, realizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getContaId());
            ps.setString(3, t.getContaContraparteId());
            ps.setString(4, t.getTipo().name());
            ps.setString(5, t.getValor().toPlainString());
            ps.setString(6, t.getSaldoApos().toPlainString());
            ps.setString(7, t.getDescricao());
            ps.setString(8, t.getRealizadoEm().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar transação: " + e.getMessage(), e);
        }
    }

    /** Extrato da conta, do mais recente pro mais antigo. */
    public List<Transacao> listarExtrato(String contaId) {
        List<Transacao> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM transacoes
            WHERE conta_origem = ?
            ORDER BY realizado_em DESC, rowid DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearTransacao(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar extrato: " + e.getMessage(), e);
        }
        return lista;
    }

    private Conta mapearConta(ResultSet rs) throws SQLException {
        return new Conta(
            rs.getString("id"),
            rs.getString("titular"),
            rs.getString("cpf"),
            Conta.TipoConta.valueOf(rs.getString("tipo")),
            new BigDecimal(rs.getString("saldo")),
            rs.getInt("ativa") == 1,
            LocalDateTime.parse(rs.getString("criado_em"))
        );
    }

    private Transacao mapearTransacao(ResultSet rs) throws SQLException {
        return new Transacao(
            rs.getString("id"),
            rs.getString("conta_origem"),
            rs.getString("conta_destino"),
            Transacao.TipoTransacao.valueOf(rs.getString("tipo")),
            new BigDecimal(rs.getString("valor")),
            new BigDecimal(rs.getString("saldo_apos")),
            rs.getString("descricao"),
            LocalDateTime.parse(rs.getString("realizado_em"))
        );
    }
}
