package banco.service;

import banco.model.Conta;
import banco.model.Moeda;
import banco.model.Transacao;
import banco.repository.BancoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service — lógica de negócio do sistema bancário
 *
 * O Service orquestra as operações de negócio usando o Repository.
 * Aqui ficam as regras: "pode sacar?", "saldo suficiente?", etc.
 *
 * Padrão de camadas:
 *   Main (UI) → Service (regras) → Repository (banco de dados) → Model (dados)
 */
public class BancoService {

    private final BancoRepository repo;

    public BancoService(BancoRepository repo) {
        this.repo = repo;
    }

    /** Abre uma nova conta após validações */
    public Conta abrirConta(String titular, String cpf, Conta.TipoConta tipo, BigDecimal deposito) {
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("Nome do titular é obrigatório");
        }
        String cpfLimpo = Cpf.limpar(cpf);
        if (!Cpf.valido(cpfLimpo)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (deposito == null || deposito.signum() < 0) {
            throw new IllegalArgumentException("Depósito inicial não pode ser negativo");
        }

        // Verifica se CPF já está cadastrado
        if (repo.buscarPorCpf(cpfLimpo).isPresent()) {
            throw new IllegalStateException("CPF já possui conta cadastrada");
        }

        Conta conta = new Conta(titular.trim(), cpfLimpo, tipo, deposito);

        return repo.emTransacao(() -> {
            repo.salvarConta(conta);

            // Registra o depósito inicial como transação
            if (deposito.signum() > 0) {
                repo.salvarTransacao(new Transacao(
                    conta.getId(), null,
                    Transacao.TipoTransacao.DEPOSITO,
                    deposito, conta.getSaldo(), "Depósito inicial na abertura da conta"
                ));
            }
            return conta;
        });
    }

    /** Deposita valor em uma conta pelo ID */
    public Conta depositar(String contaId, BigDecimal valor, String descricao) {
        Conta conta = buscarOuLancar(contaId);
        conta.depositar(valor);

        return repo.emTransacao(() -> {
            repo.atualizarConta(conta);
            repo.salvarTransacao(new Transacao(
                conta.getId(), null,
                Transacao.TipoTransacao.DEPOSITO,
                valor, conta.getSaldo(),
                descricao != null ? descricao : "Depósito"
            ));
            return conta;
        });
    }

    /** Saca valor de uma conta */
    public Conta sacar(String contaId, BigDecimal valor, String descricao) {
        Conta conta = buscarOuLancar(contaId);
        conta.sacar(valor);  // lança exceção se saldo insuficiente

        return repo.emTransacao(() -> {
            repo.atualizarConta(conta);
            repo.salvarTransacao(new Transacao(
                conta.getId(), null,
                Transacao.TipoTransacao.SAQUE,
                valor, conta.getSaldo(),
                descricao != null ? descricao : "Saque"
            ));
            return conta;
        });
    }

    /**
     * Realiza transferência entre contas.
     *
     * Operação atômica: as quatro gravações (duas contas e duas transações)
     * acontecem dentro de uma transação do banco — ou todas, ou nenhuma.
     */
    public void transferir(String origemId, String destinoId, BigDecimal valor) {
        if (origemId.equals(destinoId)) {
            throw new IllegalArgumentException("Conta de origem e destino não podem ser iguais");
        }

        Conta origem = buscarOuLancar(origemId);
        Conta destino = buscarOuLancar(destinoId);

        if (!destino.isAtiva()) {
            throw new IllegalStateException("Conta de destino está encerrada");
        }

        // Valida e aplica nos objetos antes de gravar qualquer coisa
        origem.sacar(valor);
        destino.depositar(valor);

        repo.emTransacao(() -> {
            repo.atualizarConta(origem);
            repo.atualizarConta(destino);

            // Registra duas transações: uma no extrato de cada conta
            repo.salvarTransacao(new Transacao(
                origem.getId(), destino.getId(),
                Transacao.TipoTransacao.TRANSFERENCIA_ENVIADA,
                valor, origem.getSaldo(),
                "Transferência para " + destino.getId() + " (" + destino.getTitular() + ")"
            ));
            repo.salvarTransacao(new Transacao(
                destino.getId(), origem.getId(),
                Transacao.TipoTransacao.TRANSFERENCIA_RECEBIDA,
                valor, destino.getSaldo(),
                "Transferência de " + origem.getId() + " (" + origem.getTitular() + ")"
            ));
            return null;
        });
    }

    /** Encerra uma conta com saldo zerado */
    public Conta encerrarConta(String contaId) {
        Conta conta = buscarOuLancar(contaId);
        conta.encerrar();
        repo.atualizarConta(conta);
        return conta;
    }

    public List<Transacao> verExtrato(String contaId) {
        buscarOuLancar(contaId);  // garante que a conta existe
        return repo.listarExtrato(contaId);
    }

    public List<Conta> listarContas() {
        return repo.listarTodas();
    }

    public Optional<Conta> buscarPorId(String id) {
        return repo.buscarPorId(id);
    }

    /** Soma dos saldos de todas as contas ativas */
    public BigDecimal saldoTotal() {
        return listarContas().stream()
            .filter(Conta::isAtiva)
            .map(Conta::getSaldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Método auxiliar: busca a conta ou lança exceção com mensagem clara */
    private Conta buscarOuLancar(String id) {
        return repo.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + id));
    }

    /** Formata um valor para exibição — atalho usado pela interface */
    public static String formatar(BigDecimal valor) {
        return Moeda.formatar(valor);
    }
}
