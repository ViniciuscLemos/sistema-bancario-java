package banco;

import banco.model.Conta;
import banco.model.Moeda;
import banco.model.Transacao;
import banco.repository.BancoRepository;
import banco.service.BancoService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Ponto de entrada — Interface de linha de comando (CLI)
 *
 * Conceitos aqui:
 * - JDBC: conexão com banco SQLite via DriverManager
 * - Scanner: leitura de entrada do usuário
 * - switch com "->" (Java 14+): mais elegante que o switch tradicional
 * - Tratamento de exceções com try-catch
 */
public class Main {

    private static BancoService service;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // SQLite: o arquivo banco.db é criado automaticamente.
        // Um caminho diferente pode ser passado como argumento: java -jar ... outro.db
        String arquivo = args.length > 0 ? args[0] : "banco.db";
        String url = "jdbc:sqlite:" + arquivo;

        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Banco de dados conectado: " + arquivo);

            BancoRepository repo = new BancoRepository(conn);
            service = new BancoService(repo);

            // Popula com dados de exemplo na primeira execução
            popularDadosExemplo();

            // Loop principal do menu
            boolean rodando = true;
            while (rodando) {
                exibirMenu();
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1" -> abrirConta();
                    case "2" -> realizarDeposito();
                    case "3" -> realizarSaque();
                    case "4" -> realizarTransferencia();
                    case "5" -> verExtrato();
                    case "6" -> listarContas();
                    case "7" -> encerrarConta();
                    case "0" -> {
                        System.out.println("\nEncerrando o sistema. Até logo!");
                        rodando = false;
                    }
                    default -> System.out.println("Opção inválida. Tente novamente.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro de banco de dados: " + e.getMessage());
        } catch (NoSuchElementException e) {
            // Fim da entrada (Ctrl+D / Ctrl+Z)
            System.out.println("\nEntrada encerrada. Até logo!");
        }
    }

    private static void exibirMenu() {
        System.out.println("\n=============================");
        System.out.println("   SISTEMA BANCÁRIO");
        System.out.println("=============================");
        System.out.println("1. Abrir nova conta");
        System.out.println("2. Depositar");
        System.out.println("3. Sacar");
        System.out.println("4. Transferir");
        System.out.println("5. Ver extrato");
        System.out.println("6. Listar contas");
        System.out.println("7. Encerrar conta");
        System.out.println("0. Sair");
        System.out.print("\nEscolha: ");
    }

    private static void abrirConta() {
        System.out.println("\n--- ABRIR CONTA ---");
        System.out.print("Nome do titular: ");
        String nome = scanner.nextLine();
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Tipo (1-Corrente / 2-Poupança): ");
        String tipoStr = scanner.nextLine().trim();
        Conta.TipoConta tipo = tipoStr.equals("2") ? Conta.TipoConta.POUPANCA : Conta.TipoConta.CORRENTE;
        System.out.print("Depósito inicial (0 para nenhum): R$ ");
        BigDecimal deposito = lerValor();
        if (deposito == null) return;

        try {
            Conta conta = service.abrirConta(nome, cpf, tipo, deposito);
            System.out.println("\nConta aberta com sucesso!");
            System.out.println("ID da sua conta: " + conta.getId());
            System.out.println("ANOTE este ID — você precisará dele para operações.");
        } catch (RuntimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void realizarDeposito() {
        System.out.println("\n--- DEPÓSITO ---");
        String id = lerId("ID da conta: ");
        System.out.print("Valor: R$ ");
        BigDecimal valor = lerValor();
        if (valor == null) return;
        System.out.print("Descrição (Enter para padrão): ");
        String desc = scanner.nextLine().trim();

        try {
            Conta conta = service.depositar(id, valor, desc.isEmpty() ? null : desc);
            System.out.println("\nDepósito realizado! Novo saldo: " + Moeda.formatar(conta.getSaldo()));
        } catch (RuntimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void realizarSaque() {
        System.out.println("\n--- SAQUE ---");
        String id = lerId("ID da conta: ");
        System.out.print("Valor: R$ ");
        BigDecimal valor = lerValor();
        if (valor == null) return;

        try {
            Conta conta = service.sacar(id, valor, "Saque no caixa");
            System.out.println("\nSaque realizado! Novo saldo: " + Moeda.formatar(conta.getSaldo()));
        } catch (RuntimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void realizarTransferencia() {
        System.out.println("\n--- TRANSFERÊNCIA ---");
        String origem = lerId("ID da conta de origem: ");
        String destino = lerId("ID da conta de destino: ");
        System.out.print("Valor: R$ ");
        BigDecimal valor = lerValor();
        if (valor == null) return;

        try {
            service.transferir(origem, destino, valor);
            System.out.println("\nTransferência realizada com sucesso!");
        } catch (RuntimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void verExtrato() {
        System.out.println("\n--- EXTRATO ---");
        String id = lerId("ID da conta: ");

        try {
            List<Transacao> extrato = service.verExtrato(id);
            Conta conta = service.buscarPorId(id).orElseThrow();

            System.out.println("\nExtrato da conta " + conta.getId() + " — " + conta.getTitular());
            System.out.println("-".repeat(90));
            if (extrato.isEmpty()) {
                System.out.println("Nenhuma transação encontrada.");
            } else {
                extrato.forEach(System.out::println);
            }
            System.out.println("-".repeat(90));
            System.out.println("Saldo atual: " + Moeda.formatar(conta.getSaldo()));
        } catch (RuntimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void listarContas() {
        List<Conta> contas = service.listarContas();
        System.out.println("\n--- CONTAS CADASTRADAS ---");
        if (contas.isEmpty()) {
            System.out.println("Nenhuma conta cadastrada.");
        } else {
            contas.forEach(c -> System.out.println("  " + c));
            System.out.println("\nSaldo total em contas ativas: " + Moeda.formatar(service.saldoTotal()));
        }
    }

    private static void encerrarConta() {
        System.out.println("\n--- ENCERRAR CONTA ---");
        String id = lerId("ID da conta: ");
        System.out.print("Confirma o encerramento? (s/N): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println("Operação cancelada.");
            return;
        }
        try {
            service.encerrarConta(id);
            System.out.println("Conta encerrada.");
        } catch (RuntimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void popularDadosExemplo() {
        // Só popula se não houver contas cadastradas
        if (!service.listarContas().isEmpty()) return;

        System.out.println("Criando contas de exemplo...");
        // CPFs fictícios, mas com dígitos verificadores válidos
        Conta c1 = service.abrirConta("Maria Silva", "529.982.247-25", Conta.TipoConta.CORRENTE, new BigDecimal("1500.00"));
        Conta c2 = service.abrirConta("João Santos", "111.444.777-35", Conta.TipoConta.POUPANCA, new BigDecimal("800.00"));
        service.transferir(c1.getId(), c2.getId(), new BigDecimal("300.00"));
        System.out.println("Contas de exemplo criadas! IDs: " + c1.getId() + ", " + c2.getId());
    }

    private static String lerId(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim().toUpperCase();
    }

    /** Lê um valor em reais. Retorna null (e avisa) se o texto não for um número. */
    private static BigDecimal lerValor() {
        String texto = scanner.nextLine();
        try {
            return Moeda.parse(texto);
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido: \"" + texto.trim() + "\". Operação cancelada.");
            return null;
        }
    }
}
