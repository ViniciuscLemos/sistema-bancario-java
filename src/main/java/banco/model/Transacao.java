package banco.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Uma movimentação no extrato de uma conta.
 * Transferência gera duas: ENVIADA na origem e RECEBIDA no destino,
 * e contaContraparteId guarda a outra conta.
 */
public class Transacao {

    public enum TipoTransacao {
        DEPOSITO,
        SAQUE,
        TRANSFERENCIA_ENVIADA,
        TRANSFERENCIA_RECEBIDA
    }

    private final String id;
    private final String contaId;
    private final String contaContraparteId;  // null em depósito e saque
    private final TipoTransacao tipo;
    private final BigDecimal valor;
    private final BigDecimal saldoApos;
    private final String descricao;
    private final LocalDateTime realizadoEm;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Transacao(String contaId, String contaContraparteId,
                     TipoTransacao tipo, BigDecimal valor, BigDecimal saldoApos, String descricao) {
        this("TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(),
             contaId, contaContraparteId, tipo, valor, saldoApos, descricao, LocalDateTime.now());
    }

    /** Transação lida do banco de dados. */
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
        return String.format(
            "%s  %s%14s  Saldo: %14s  | %s",
            realizadoEm.format(FORMATTER),
            isCredito() ? "+" : "-",
            Moeda.formatar(valor),
            Moeda.formatar(saldoApos),
            descricao
        );
    }
}
