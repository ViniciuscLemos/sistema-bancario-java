package banco.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo de Transação
 *
 * Representa qualquer movimentação financeira:
 * depósito, saque ou transferência.
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
    private final String contaOrigemId;
    private final String contaDestinoId;  // null para depósito/saque
    private final TipoTransacao tipo;
    private final double valor;
    private final double saldoApos;
    private final String descricao;
    private final LocalDateTime realizadoEm;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Contador estático: compartilhado por todas as instâncias
    // Gera IDs sequenciais simples (TXN-001, TXN-002...)
    private static int contador = 0;

    public Transacao(String contaOrigemId, String contaDestinoId,
                     TipoTransacao tipo, double valor, double saldoApos, String descricao) {
        this.id = String.format("TXN-%03d", ++contador);
        this.contaOrigemId = contaOrigemId;
        this.contaDestinoId = contaDestinoId;
        this.tipo = tipo;
        this.valor = valor;
        this.saldoApos = saldoApos;
        this.descricao = descricao;
        this.realizadoEm = LocalDateTime.now();
    }

    // Getters
    public String getId()             { return id; }
    public String getContaOrigemId()  { return contaOrigemId; }
    public String getContaDestinoId() { return contaDestinoId; }
    public TipoTransacao getTipo()    { return tipo; }
    public double getValor()          { return valor; }
    public double getSaldoApos()      { return saldoApos; }
    public String getDescricao()      { return descricao; }
    public LocalDateTime getRealizadoEm() { return realizadoEm; }

    @Override
    public String toString() {
        // Sinal negativo para saques e transferências enviadas
        String sinal = (tipo == TipoTransacao.DEPOSITO || tipo == TipoTransacao.TRANSFERENCIA_RECEBIDA)
                       ? "+" : "-";

        return String.format(
            "[%s] %s  %sR$ %8.2f  Saldo: R$ %.2f  | %s | %s",
            id,
            realizadoEm.format(FORMATTER),
            sinal,
            valor,
            saldoApos,
            tipo,
            descricao
        );
    }
}
