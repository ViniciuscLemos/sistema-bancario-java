package banco;

import banco.model.Conta;
import banco.model.Transacao;
import banco.repository.BancoRepository;
import banco.service.BancoService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Ponto de entrada — Interface de linha de comando (CLI)
 *
 * Conceitos aqui:
 * - JDBC: conexão com banco SQLite via DriverManager
 * - Scanner: leitura de entrada do usuário
 * - switch expression (Java 14+): mais elegante que switch statement
 * - Tratamento de exceções com try-catch
 */
public class Main {

    private static BancoService service;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // SQLite: o arquivo banco.db é criado automaticamente
        String url = "jdbc:sqlite:banco.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Banco de dados conectado!");

            BancoRepository repo = new BancoRepository(conn);
            service = new BancoService(repo);

            // Popula com dados de exemplo na primeira execução
            popularDadosExemplo();

            // Loop principal do menu
            boolean rodando = true;
            while (rodando) {
                exibirMenu();
                String opcao = scanner.nextLine().trim();

                // Switch expression — Java 14+
                switch (opcao) {
                    case "1" -> abrirConta();
                    case "2" -> realizarDeposito();
                    case "3" -> realizarSaque();
                    case "4" -> realizarTransferencia();
                    case "5" -> verExtrato();
                    case "6" -> listarContas();
                    case "0" -> {
                        System.out.println("\nEncerrando o sistema. Até logo!");
                        rodando = false;
                    }
                    default -> System.out.println("Opção inválida. Tente novamente.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro de banco de dados: " + e.getMessage());
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
        System.out.println("0. Sair");
        System.out.print("\nEscolha: ");
    }

    private static void abrirConta() {
        System.out.println("\n--- ABRIR CONTA ---");
        System.out.print("Nome do titular: ");
        String nome = scanner.nextLine();
        System.out.print("CPF (somente números): ");
        String cpf = scanner.nextLine();
        System.out.print("Tipo (1-Corrente / 2-Poupança): ");
        String tipoStr = scanner.nextLine();
        Conta.TipoConta tipo = tipoStr.equals("2") ? Conta.TipoConta.POUPANCA : Conta.TipoConta.CORRENTE;
        System.out.print("Depósito inicial (0 para nenhum): R$ ");
        double deposito = lerDouble();

        try {
            Conta conta = service.abrirConta(nome, cpf, tipo, deposito);
            System.out.println("\nConta aberta com sucesso!");
            System.out.println("ID da sua conta: " + conta.getId());
            System.out.println("ANOTE este ID — você precisará dele para operações.");
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void realizarDeposito() {
        System.out.println("\n--- DEPÓSITO ---");
        System.out.print("ID da conta: ");
        String id = scanner.nextLine().trim().toUpperCase();
        System.out.print("Valor: R$ ");
        double valor = lerDouble();
        System.out.print("Descrição (Enter para padrão): ");
        String desc = scanner.nextLine();

        try {
            Conta conta = service.depositar(id, valor, desc.isEmpty() ? null : desc);
            System.out.printf("\nDepósito realizado! Novo saldo: R$ %.2f%n", conta.getSaldo());
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void realizarSaque() {
        System.out.println("\n--- SAQUE ---");
        System.out.print("ID da conta: ");
        String id = scanner.nextLine().trim().toUpperCase();
        System.out.print("Valor: R$ ");
        double valor = lerDouble();

        try {
            Conta conta = service.sacar(id, valor, "Saque no caixa");
            System.out.printf("\nSaque realizado! Novo saldo: R$ %.2f%n", conta.getSaldo());
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void realizarTransferencia() {
        System.out.println("\n--- TRANSFERÊNCIA ---");
        System.out.print("ID da conta de origem: ");
        String origem = scanner.nextLine().trim().toUpperCase();
        System.out.print("ID da conta de destino: ");
        String destino = scanner.nextLine().trim().toUpperCase();
        System.out.print("Valor: R$ ");
        double valor = lerDouble();

        try {
            service.transferir(origem, destino, valor);
            System.out.println("\nTransferência realizada com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void verExtrato() {
        System.out.println("\n--- EXTRATO ---");
        System.out.print("ID da conta: ");
        String id = scanner.nextLine().trim().toUpperCase();

        try {
            List<Transacao> extrato = service.verExtrato(id);
            Conta conta = service.buscarPorId(id).get();

            System.out.println("\nExtrato da conta: " + conta.getTitular());
            System.out.println("-".repeat(80));
            if (extrato.isEmpty()) {
                System.out.println("Nenhuma transação encontrada.");
            } else {
                extrato.forEach(t -> System.out.println(t));
            }
            System.out.printf("%nSaldo atual: R$ %.2f%n", conta.getSaldo());
        } catch (Exception e) {
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
        }
    }

    private static void popularDadosExemplo() {
        // Só popula se não houver contas cadastradas
        if (!service.listarContas().isEmpty()) return;

        System.out.println("Criando contas de exemplo...");
        try {
            Conta c1 = service.abrirConta("Maria Silva", "12345678901", Conta.TipoConta.CORRENTE, 1500.0);
            Conta c2 = service.abrirConta("João Santos", "98765432100", Conta.TipoConta.POUPANCA, 800.0);
            service.transferir(c1.getId(), c2.getId(), 300.0);
            System.out.println("Contas de exemplo criadas! IDs: " + c1.getId() + ", " + c2.getId());
        } catch (Exception e) {
            System.out.println("Dados de exemplo já existentes.");
        }
    }

    private static double lerDouble() {
        try {
            return Double.parseDouble(scanner.nextLine().replace(",", ".").trim());
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido. Usando 0.");
            return 0;
        }
    }
}
