package banco.service;

import banco.model.Conta;
import banco.model.Moeda;
import banco.model.Transacao;
import banco.repository.BancoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Regras do banco (validações, transferência...). O SQL fica no repository. */
public class BancoService {

    private final BancoRepository repo;

    public BancoService(BancoRepository repo) {
        this.repo = repo;
    }

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

        if (repo.buscarPorCpf(cpfLimpo).isPresent()) {
            throw new IllegalStateException("CPF já possui conta cadastrada");
        }

        Conta conta = new Conta(titular.trim(), cpfLimpo, tipo, deposito);

        return repo.emTransacao(() -> {
            repo.salvarConta(conta);

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

    public Conta sacar(String contaId, BigDecimal valor, String descricao) {
        Conta conta = buscarOuLancar(contaId);
        conta.sacar(valor);

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

    /** As duas contas e as duas linhas do extrato são gravadas juntas, numa transação só. */
    public void transferir(String origemId, String destinoId, BigDecimal valor) {
        if (origemId.equals(destinoId)) {
            throw new IllegalArgumentException("Conta de origem e destino não podem ser iguais");
        }

        Conta origem = buscarOuLancar(origemId);
        Conta destino = buscarOuLancar(destinoId);

        if (!destino.isAtiva()) {
            throw new IllegalStateException("Conta de destino está encerrada");
        }

        origem.sacar(valor);
        destino.depositar(valor);

        repo.emTransacao(() -> {
            repo.atualizarConta(origem);
            repo.atualizarConta(destino);

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

    public Conta encerrarConta(String contaId) {
        Conta conta = buscarOuLancar(contaId);
        conta.encerrar();
        repo.atualizarConta(conta);
        return conta;
    }

    public List<Transacao> verExtrato(String contaId) {
        buscarOuLancar(contaId);
        return repo.listarExtrato(contaId);
    }

    public List<Conta> listarContas() {
        return repo.listarTodas();
    }

    public Optional<Conta> buscarPorId(String id) {
        return repo.buscarPorId(id);
    }

    public BigDecimal saldoTotal() {
        return listarContas().stream()
            .filter(Conta::isAtiva)
            .map(Conta::getSaldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Conta buscarOuLancar(String id) {
        return repo.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + id));
    }

    public static String formatar(BigDecimal valor) {
        return Moeda.formatar(valor);
    }
}
