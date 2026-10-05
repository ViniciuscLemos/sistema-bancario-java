package banco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de Conta Bancária
 *
 * Conceitos de Java aqui:
 * - Encapsulamento: atributos private, acesso via getters
 * - enum: tipo seguro para representar categorias fixas (tipo de conta)
 * - UUID: forma de gerar IDs únicos sem precisar de banco sequencial
 * - LocalDateTime: data e hora imutável (API moderna do Java 8+)
 * - BigDecimal: valores monetários exatos (double tem erros de arredondamento:
 *   0.1 + 0.2 == 0.30000000000000004)
 */
public class Conta {

    // Enum define valores possíveis para tipo de conta
    // Mais seguro que usar String ou int
    public enum TipoConta {
        CORRENTE,
        POUPANCA
    }

    // Atributos private: só acessíveis dentro desta classe
    private final String id;         // final = não pode ser reatribuído após construção
    private final String titular;
    private final String cpf;
    private final TipoConta tipo;
    private BigDecimal saldo;
    private boolean ativa;
    private final LocalDateTime criadoEm;

    /** Abre uma conta nova: gera um ID e registra a data de criação. */
    public Conta(String titular, String cpf, TipoConta tipo, BigDecimal saldoInicial) {
        // UUID.randomUUID() gera um ID único universal; usamos os 8 primeiros caracteres
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             titular, cpf, tipo, saldoInicial, true, LocalDateTime.now());
    }

    /**
     * Reconstrói uma conta que já existe (lida do banco de dados).
     *
     * Sem este construtor, o repositório criava uma conta "nova" a cada leitura,
     * com um ID diferente do salvo — e as atualizações de saldo se perdiam.
     */
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

    // Getters — permitem leitura dos atributos privados
    public String getId()           { return id; }
    public String getTitular()      { return titular; }
    public String getCpf()          { return cpf; }
    public TipoConta getTipo()      { return tipo; }
    public BigDecimal getSaldo()    { return saldo; }
    public boolean isAtiva()        { return ativa; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    // Métodos de negócio — operações na conta

    /**
     * Deposita um valor na conta.
     * Lança IllegalArgumentException para entradas inválidas.
     */
    public void depositar(BigDecimal valor) {
        validarValor(valor, "depósito");
        exigirAtiva();
        this.saldo = this.saldo.add(dinheiro(valor));
    }

    /**
     * Saca um valor da conta.
     * Verifica saldo suficiente antes de deduzir.
     */
    public void sacar(BigDecimal valor) {
        validarValor(valor, "saque");
        exigirAtiva();
        if (valor.compareTo(this.saldo) > 0) {
            throw new IllegalStateException("Saldo insuficiente. Saldo atual: " + Moeda.formatar(saldo));
        }
        this.saldo = this.saldo.subtract(dinheiro(valor));
    }

    /** Encerra a conta. Só é permitido com saldo zerado. */
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

    /** Normaliza para 2 casas decimais (centavos). */
    public static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    // toString: representação textual do objeto (útil para debug e logs)
    @Override
    public String toString() {
        return String.format(
            "Conta[%s] %s | Tipo: %s | Saldo: %s | %s",
            id, titular, tipo, Moeda.formatar(saldo), ativa ? "Ativa" : "Encerrada"
        );
    }
}
