package banco;

import banco.model.Conta;
import banco.model.Transacao;
import banco.repository.BancoRepository;
import banco.service.BancoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração: service + repository com um SQLite de verdade.
 */
class BancoServiceTest {

    // CPFs fictícios com dígitos verificadores válidos
    static final String CPF_MARIA = "52998224725";
    static final String CPF_JOAO = "11144477735";

    @TempDir
    Path pasta;

    Connection conn;
    BancoService service;

    @BeforeEach
    void abrir() throws SQLException {
        conn = conectar();
        service = new BancoService(new BancoRepository(conn));
    }

    @AfterEach
    void fechar() throws SQLException {
        conn.close();
    }

    Connection conectar() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + pasta.resolve("teste.db"));
    }

    static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void abreContaComDepositoInicial() {
        Conta conta = service.abrirConta("Maria", "529.982.247-25", Conta.TipoConta.CORRENTE, reais("100"));

        Conta salva = service.buscarPorId(conta.getId()).orElseThrow();
        assertEquals(conta.getId(), salva.getId());
        assertEquals(CPF_MARIA, salva.getCpf());  // CPF salvo sem pontuação
        assertEquals(reais("100.00"), salva.getSaldo());
        assertEquals(1, service.verExtrato(conta.getId()).size());
    }

    @Test
    void validaDadosDeAbertura() {
        assertThrows(IllegalArgumentException.class,
            () -> service.abrirConta(" ", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("0")));
        assertThrows(IllegalArgumentException.class,
            () -> service.abrirConta("Maria", "12345678901", Conta.TipoConta.CORRENTE, reais("0")));
        assertThrows(IllegalArgumentException.class,
            () -> service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("-1")));

        service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("0"));
        assertThrows(IllegalStateException.class,
            () -> service.abrirConta("Outra", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("0")));
    }

    /** Bug antigo: ao ler do banco a conta ganhava um ID novo e o saldo nunca era atualizado. */
    @Test
    void depositoESaquePersistemOSaldo() {
        Conta conta = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("100"));

        service.depositar(conta.getId(), reais("50.25"), null);
        service.sacar(conta.getId(), reais("20.10"), null);

        assertEquals(reais("130.15"), service.buscarPorId(conta.getId()).orElseThrow().getSaldo());
    }

    @Test
    void valoresSaoExatos() {
        // Com double, 0.1 + 0.2 daria 0.30000000000000004
        Conta conta = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("0.10"));
        service.depositar(conta.getId(), reais("0.20"), null);
        assertEquals(reais("0.30"), service.buscarPorId(conta.getId()).orElseThrow().getSaldo());
    }

    @Test
    void naoPermiteSacarMaisQueOSaldo() {
        Conta conta = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("10"));
        assertThrows(IllegalStateException.class, () -> service.sacar(conta.getId(), reais("10.01"), null));
        assertThrows(IllegalArgumentException.class, () -> service.depositar(conta.getId(), reais("0"), null));
        assertEquals(reais("10.00"), service.buscarPorId(conta.getId()).orElseThrow().getSaldo());
    }

    @Test
    void transferenciaMoveSaldoERegistraUmaLinhaPorConta() {
        Conta maria = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("1500"));
        Conta joao = service.abrirConta("João", CPF_JOAO, Conta.TipoConta.POUPANCA, reais("800"));

        service.transferir(maria.getId(), joao.getId(), reais("300"));

        assertEquals(reais("1200.00"), service.buscarPorId(maria.getId()).orElseThrow().getSaldo());
        assertEquals(reais("1100.00"), service.buscarPorId(joao.getId()).orElseThrow().getSaldo());

        // Bug antigo: cada transferência aparecia duas vezes no extrato
        List<Transacao> extratoMaria = service.verExtrato(maria.getId());
        assertEquals(2, extratoMaria.size());
        assertEquals(Transacao.TipoTransacao.TRANSFERENCIA_ENVIADA, extratoMaria.get(0).getTipo());
        assertEquals(joao.getId(), extratoMaria.get(0).getContaContraparteId());

        List<Transacao> extratoJoao = service.verExtrato(joao.getId());
        assertEquals(2, extratoJoao.size());
        assertEquals(Transacao.TipoTransacao.TRANSFERENCIA_RECEBIDA, extratoJoao.get(0).getTipo());
    }

    @Test
    void transferenciaInvalidaNaoAlteraNada() {
        Conta maria = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("100"));
        Conta joao = service.abrirConta("João", CPF_JOAO, Conta.TipoConta.CORRENTE, reais("0"));

        assertThrows(IllegalStateException.class, () -> service.transferir(maria.getId(), joao.getId(), reais("500")));
        assertThrows(IllegalArgumentException.class, () -> service.transferir(maria.getId(), maria.getId(), reais("1")));
        assertThrows(IllegalArgumentException.class, () -> service.transferir(maria.getId(), "NAOEXISTE", reais("1")));

        assertEquals(reais("100.00"), service.buscarPorId(maria.getId()).orElseThrow().getSaldo());
        assertEquals(1, service.verExtrato(maria.getId()).size());
    }

    @Test
    void encerraContaSoComSaldoZero() {
        Conta conta = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("10"));
        assertThrows(IllegalStateException.class, () -> service.encerrarConta(conta.getId()));

        service.sacar(conta.getId(), reais("10"), null);
        service.encerrarConta(conta.getId());

        assertFalse(service.buscarPorId(conta.getId()).orElseThrow().isAtiva());
        assertThrows(IllegalStateException.class, () -> service.depositar(conta.getId(), reais("1"), null));
    }

    /** Bug antigo: o contador de transações voltava a TXN-001 e a 2ª execução quebrava com PRIMARY KEY duplicada. */
    @Test
    void dadosSobrevivemAReaberturaDoPrograma() throws SQLException {
        Conta conta = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("100"));
        service.depositar(conta.getId(), reais("1"), null);
        conn.close();

        // Simula fechar e abrir o programa de novo
        conn = conectar();
        service = new BancoService(new BancoRepository(conn));

        service.depositar(conta.getId(), reais("2"), null);
        service.depositar(conta.getId(), reais("3"), null);

        Conta reaberta = service.buscarPorId(conta.getId()).orElseThrow();
        assertEquals(reais("106.00"), reaberta.getSaldo());
        assertEquals(conta.getCriadoEm(), reaberta.getCriadoEm());
        assertEquals(4, service.verExtrato(conta.getId()).size());
    }

    @Test
    void saldoTotalSomaContasAtivas() {
        Conta maria = service.abrirConta("Maria", CPF_MARIA, Conta.TipoConta.CORRENTE, reais("10.50"));
        service.abrirConta("João", CPF_JOAO, Conta.TipoConta.CORRENTE, reais("4.50"));
        assertEquals(reais("15.00"), service.saldoTotal());

        service.sacar(maria.getId(), reais("10.50"), null);
        service.encerrarConta(maria.getId());
        assertEquals(reais("4.50"), service.saldoTotal());
    }
}
