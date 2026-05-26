package banco.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de Conta Bancária
 *
 * Conceitos de Java aqui:
 * - Encapsulamento: atributos private, acesso via getters/setters
 * - enum: tipo seguro para representar categorias fixas (tipo de conta)
 * - UUID: forma de gerar IDs únicos sem precisar de banco sequencial
 * - LocalDateTime: data e hora imutável (API moderna do Java 8+)
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
    private double saldo;
    private boolean ativa;
    private final LocalDateTime criadoEm;

    // Construtor: inicializa o objeto
    public Conta(String titular, String cpf, TipoConta tipo, double saldoInicial) {
        // UUID.randomUUID() gera um ID único universal
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.titular = titular;
        this.cpf = cpf;
        this.tipo = tipo;
        this.saldo = saldoInicial;
        this.ativa = true;
        this.criadoEm = LocalDateTime.now();
    }

    // Getters — permitem leitura dos atributos privados
    public String getId()           { return id; }
    public String getTitular()      { return titular; }
    public String getCpf()          { return cpf; }
    public TipoConta getTipo()      { return tipo; }
    public double getSaldo()        { return saldo; }
    public boolean isAtiva()        { return ativa; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    // Métodos de negócio — operações na conta

    /**
     * Deposita um valor na conta.
     * Lança IllegalArgumentException para entradas inválidas.
     */
    public void depositar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor de depósito deve ser positivo");
        }
        if (!ativa) {
            throw new IllegalStateException("Conta inativa");
        }
        this.saldo += valor;
    }

    /**
     * Saca um valor da conta.
     * Verifica saldo suficiente antes de deduzir.
     */
    public void sacar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor de saque deve ser positivo");
        }
        if (!ativa) {
            throw new IllegalStateException("Conta inativa");
        }
        if (valor > this.saldo) {
            throw new IllegalStateException("Saldo insuficiente. Saldo atual: R$ " + String.format("%.2f", saldo));
        }
        this.saldo -= valor;
    }

    public void desativar() {
        this.ativa = false;
    }

    // toString: representação textual do objeto (útil para debug e logs)
    @Override
    public String toString() {
        return String.format(
            "Conta[%s] %s | Tipo: %s | Saldo: R$ %.2f | %s",
            id, titular, tipo, saldo, ativa ? "Ativa" : "Inativa"
        );
    }
}
