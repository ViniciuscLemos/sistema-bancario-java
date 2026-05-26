package banco.service;

import banco.model.Conta;
import banco.model.Transacao;
import banco.repository.BancoRepository;

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
    public Conta abrirConta(String titular, String cpf, Conta.TipoConta tipo, double deposito) {
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("Nome do titular é obrigatório");
        }
        if (cpf == null || cpf.length() < 11) {
            throw new IllegalArgumentException("CPF inválido");
        }

        // Verifica se CPF já está cadastrado
        if (repo.buscarPorCpf(cpf).isPresent()) {
            throw new IllegalStateException("CPF já possui conta cadastrada");
        }

        if (deposito < 0) {
            throw new IllegalArgumentException("Depósito inicial não pode ser negativo");
        }

        Conta conta = new Conta(titular, cpf, tipo, deposito);
        repo.salvarConta(conta);

        // Registra o depósito inicial como transação
        if (deposito > 0) {
            Transacao t = new Transacao(
                conta.getId(), null,
                Transacao.TipoTransacao.DEPOSITO,
                deposito, deposito, "Depósito inicial na abertura da conta"
            );
            repo.salvarTransacao(t);
        }

        return conta;
    }

    /** Deposita valor em uma conta pelo ID */
    public Conta depositar(String contaId, double valor, String descricao) {
        Conta conta = buscarOuLancar(contaId);
        conta.depositar(valor);
        repo.atualizarConta(conta);

        Transacao t = new Transacao(
            contaId, null,
            Transacao.TipoTransacao.DEPOSITO,
            valor, conta.getSaldo(),
            descricao != null ? descricao : "Depósito"
        );
        repo.salvarTransacao(t);

        return conta;
    }

    /** Saca valor de uma conta */
    public Conta sacar(String contaId, double valor, String descricao) {
        Conta conta = buscarOuLancar(contaId);
        conta.sacar(valor);  // lança exceção se saldo insuficiente
        repo.atualizarConta(conta);

        Transacao t = new Transacao(
            contaId, null,
            Transacao.TipoTransacao.SAQUE,
            valor, conta.getSaldo(),
            descricao != null ? descricao : "Saque"
        );
        repo.salvarTransacao(t);

        return conta;
    }

    /**
     * Realiza transferência entre contas.
     *
     * Operação atômica: os dois saques/depósitos devem ser tratados juntos.
     * Em produção, usaríamos uma transação de banco de dados aqui.
     */
    public void transferir(String origemId, String destinoId, double valor) {
        if (origemId.equals(destinoId)) {
            throw new IllegalArgumentException("Conta de origem e destino não podem ser iguais");
        }

        Conta origem = buscarOuLancar(origemId);
        Conta destino = buscarOuLancar(destinoId);

        // Valida antes de modificar qualquer conta
        if (valor <= 0) throw new IllegalArgumentException("Valor deve ser positivo");
        if (origem.getSaldo() < valor) throw new IllegalStateException("Saldo insuficiente para transferência");

        // Executa a operação nas duas contas
        origem.sacar(valor);
        destino.depositar(valor);

        repo.atualizarConta(origem);
        repo.atualizarConta(destino);

        // Registra duas transações: uma para cada lado
        repo.salvarTransacao(new Transacao(
            origemId, destinoId,
            Transacao.TipoTransacao.TRANSFERENCIA_ENVIADA,
            valor, origem.getSaldo(),
            "Transferência para conta " + destinoId + " (" + destino.getTitular() + ")"
        ));

        repo.salvarTransacao(new Transacao(
            destinoId, origemId,
            Transacao.TipoTransacao.TRANSFERENCIA_RECEBIDA,
            valor, destino.getSaldo(),
            "Transferência recebida de " + origemId + " (" + origem.getTitular() + ")"
        ));
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

    /** Método auxiliar: busca a conta ou lança exceção com mensagem clara */
    private Conta buscarOuLancar(String id) {
        return repo.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + id));
    }
}
