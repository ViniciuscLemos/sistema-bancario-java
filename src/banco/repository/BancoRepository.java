package banco.repository;

import banco.model.Conta;
import banco.model.Transacao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository — camada de acesso a dados
 *
 * O padrão Repository separa a lógica de negócio do acesso ao banco.
 * O service não sabe se os dados vêm de MySQL, SQLite ou outro lugar.
 *
 * Conceitos aqui:
 * - JDBC: API Java para banco de dados relacional
 * - PreparedStatement: evita SQL Injection (parametrizado)
 * - Optional<T>: forma moderna de tratar ausência de valor (evita NullPointerException)
 * - try-with-resources: garante fechamento automático de recursos
 */
public class BancoRepository {

    private final Connection conn;

    public BancoRepository(Connection conn) {
        this.conn = conn;
        criarTabelas();
    }

    private void criarTabelas() {
        // try-with-resources: Statement é fechado automaticamente ao sair do bloco
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS contas (
                    id         TEXT PRIMARY KEY,
                    titular    TEXT NOT NULL,
                    cpf        TEXT UNIQUE NOT NULL,
                    tipo       TEXT NOT NULL,
                    saldo      REAL NOT NULL DEFAULT 0,
                    ativa      INTEGER NOT NULL DEFAULT 1,
                    criado_em  TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transacoes (
                    id              TEXT PRIMARY KEY,
                    conta_origem    TEXT NOT NULL,
                    conta_destino   TEXT,
                    tipo            TEXT NOT NULL,
                    valor           REAL NOT NULL,
                    saldo_apos      REAL NOT NULL,
                    descricao       TEXT,
                    realizado_em    TEXT NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar tabelas: " + e.getMessage(), e);
        }
    }

    /** Salva uma conta nova no banco */
    public void salvarConta(Conta conta) {
        String sql = """
            INSERT INTO contas (id, titular, cpf, tipo, saldo, ativa, criado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        // PreparedStatement com ? parametrizado — nunca concatene strings SQL!
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, conta.getId());
            ps.setString(2, conta.getTitular());
            ps.setString(3, conta.getCpf());
            ps.setString(4, conta.getTipo().name());
            ps.setDouble(5, conta.getSaldo());
            ps.setInt(6, conta.isAtiva() ? 1 : 0);
            ps.setString(7, conta.getCriadoEm().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar conta: " + e.getMessage(), e);
        }
    }

    /** Atualiza o saldo e o status de uma conta */
    public void atualizarConta(Conta conta) {
        String sql = "UPDATE contas SET saldo = ?, ativa = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, conta.getSaldo());
            ps.setInt(2, conta.isAtiva() ? 1 : 0);
            ps.setString(3, conta.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar conta: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma conta pelo ID.
     * Retorna Optional.empty() se não encontrar (evita retornar null).
     */
    public Optional<Conta> buscarPorId(String id) {
        String sql = "SELECT * FROM contas WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapearConta(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta: " + e.getMessage(), e);
        }
    }

    public Optional<Conta> buscarPorCpf(String cpf) {
        String sql = "SELECT * FROM contas WHERE cpf = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cpf);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapearConta(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta por CPF: " + e.getMessage(), e);
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
            ps.setString(2, t.getContaOrigemId());
            ps.setString(3, t.getContaDestinoId());
            ps.setString(4, t.getTipo().name());
            ps.setDouble(5, t.getValor());
            ps.setDouble(6, t.getSaldoApos());
            ps.setString(7, t.getDescricao());
            ps.setString(8, t.getRealizadoEm().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar transação: " + e.getMessage(), e);
        }
    }

    public List<Transacao> listarExtrato(String contaId) {
        List<Transacao> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM transacoes
            WHERE conta_origem = ? OR conta_destino = ?
            ORDER BY realizado_em DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contaId);
            ps.setString(2, contaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearTransacao(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar extrato: " + e.getMessage(), e);
        }
        return lista;
    }

    // Métodos privados que convertem ResultSet em objetos Java
    private Conta mapearConta(ResultSet rs) throws SQLException {
        Conta c = new Conta(
            rs.getString("titular"),
            rs.getString("cpf"),
            Conta.TipoConta.valueOf(rs.getString("tipo")),
            rs.getDouble("saldo")
        );
        if (rs.getInt("ativa") == 0) c.desativar();
        return c;
    }

    private Transacao mapearTransacao(ResultSet rs) throws SQLException {
        return new Transacao(
            rs.getString("conta_origem"),
            rs.getString("conta_destino"),
            Transacao.TipoTransacao.valueOf(rs.getString("tipo")),
            rs.getDouble("valor"),
            rs.getDouble("saldo_apos"),
            rs.getString("descricao")
        );
    }
}
