package banco.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Modelo de Transação
 *
 * Representa qualquer movimentação financeira:
 * depósito, saque ou transferência.
 *
 * Cada transação pertence a UMA conta (contaId). Uma transferência gera duas
 * transações: TRANSFERENCIA_ENVIADA na conta de origem e TRANSFERENCIA_RECEBIDA
 * na de destino; contaContraparteId aponta para a outra conta envolvida.
 *
 * Conceitos aqui:
 * - enum com múltiplos valores
 * - Objeto imutável (todos os campos são final)
 * - Formatação de datas com DateTimeFormatter
 */
public class Transacao {

    public enum TipoTransacao {
        DEPOSITO,
        SAQUE,
        TRANSFERENCIA_ENVIADA,
        TRANSFERENCIA_RECEBIDA
    }

    // Objeto imutável: todos os campos são final
    // Uma transação registrada nunca deve ser alterada
    private final String id;
    private final String contaId;
    private final String contaContraparteId;  // null para depósito/saque
    private final TipoTransacao tipo;
    private final BigDecimal valor;
    private final BigDecimal saldoApos;
    private final String descricao;
    private final LocalDateTime realizadoEm;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Registra uma transação nova. */
    public Transacao(String contaId, String contaContraparteId,
                     TipoTransacao tipo, BigDecimal valor, BigDecimal saldoApos, String descricao) {
        // Antes, o ID vinha de um contador estático que voltava a 1 a cada execução
        // do programa e colidia com os IDs já salvos (PRIMARY KEY duplicada).
        // Um UUID é único mesmo entre execuções.
        this("TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(),
             contaId, contaContraparteId, tipo, valor, saldoApos, descricao, LocalDateTime.now());
    }

    /** Reconstrói uma transação lida do banco, preservando ID e data originais. */
    public Transacao(String id, String contaId, String contaContraparteId, TipoTransacao tipo,
                     BigDecimal valor, BigDecimal saldoApos, String descricao, LocalDateTime realizadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.contaContraparteId = contaContraparteId;
        this.tipo = tipo;
        this.valor = Conta.dinheiro(valor);
        this.saldoApos = Conta.dinheiro(saldoApos);
        this.descricao = descricao;
        this.realizadoEm = realizadoEm;
    }

    // Getters
    public String getId()                 { return id; }
    public String getContaId()            { return contaId; }
    public String getContaContraparteId() { return contaContraparteId; }
    public TipoTransacao getTipo()        { return tipo; }
    public BigDecimal getValor()          { return valor; }
    public BigDecimal getSaldoApos()      { return saldoApos; }
    public String getDescricao()          { return descricao; }
    public LocalDateTime getRealizadoEm() { return realizadoEm; }

    public boolean isCredito() {
        return tipo == TipoTransacao.DEPOSITO || tipo == TipoTransacao.TRANSFERENCIA_RECEBIDA;
    }

    @Override
    public String toString() {
        // Sinal negativo para saques e transferências enviadas
        String sinal = isCredito() ? "+" : "-";

        return String.format(
            "%s  %s%14s  Saldo: %14s  | %s",
            realizadoEm.format(FORMATTER),
            sinal,
            Moeda.formatar(valor),
            Moeda.formatar(saldoApos),
            descricao
        );
    }
}
