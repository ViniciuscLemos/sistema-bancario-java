package banco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public class Conta {

    public enum TipoConta {
        CORRENTE,
        POUPANCA
    }

    private final String id;
    private final String titular;
    private final String cpf;
    private final TipoConta tipo;
    private BigDecimal saldo;
    private boolean ativa;
    private final LocalDateTime criadoEm;

    /** Conta nova: gera o id (8 primeiros caracteres de um UUID). */
    public Conta(String titular, String cpf, TipoConta tipo, BigDecimal saldoInicial) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             titular, cpf, tipo, saldoInicial, true, LocalDateTime.now());
    }

    /** Conta que já existe no banco de dados. */
    public Conta(String id, String titular, String cpf, TipoConta tipo,
                 BigDecimal saldo, boolean ativa, LocalDateTime criadoEm) {
        this.id = id;
        this.titular = titular;
        this.cpf = cpf;
        this.tipo = tipo;
        this.saldo = dinheiro(saldo);
        this.ativa = ativa;
        this.criadoEm = criadoEm;
    }

    public String getId()              { return id; }
    public String getTitular()         { return titular; }
    public String getCpf()             { return cpf; }
    public TipoConta getTipo()         { return tipo; }
    public BigDecimal getSaldo()       { return saldo; }
    public boolean isAtiva()           { return ativa; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void depositar(BigDecimal valor) {
        validarValor(valor, "depósito");
        exigirAtiva();
        this.saldo = this.saldo.add(dinheiro(valor));
    }

    public void sacar(BigDecimal valor) {
        validarValor(valor, "saque");
        exigirAtiva();
        if (valor.compareTo(this.saldo) > 0) {
            throw new IllegalStateException("Saldo insuficiente. Saldo atual: " + Moeda.formatar(saldo));
        }
        this.saldo = this.saldo.subtract(dinheiro(valor));
    }

    /** Só dá pra encerrar com o saldo zerado. */
    public void encerrar() {
        exigirAtiva();
        if (saldo.signum() != 0) {
            throw new IllegalStateException("Saque ou transfira o saldo de " + Moeda.formatar(saldo)
                    + " antes de encerrar a conta");
        }
        this.ativa = false;
    }

    private void exigirAtiva() {
        if (!ativa) {
            throw new IllegalStateException("Conta inativa");
        }
    }

    private static void validarValor(BigDecimal valor, String operacao) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor de " + operacao + " deve ser positivo");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Use no máximo 2 casas decimais");
        }
    }

    /** Arredonda pra centavos. */
    public static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String toString() {
        return String.format(
            "Conta[%s] %s | Tipo: %s | Saldo: %s | %s",
            id, titular, tipo, Moeda.formatar(saldo), ativa ? "Ativa" : "Encerrada"
        );
    }
}
