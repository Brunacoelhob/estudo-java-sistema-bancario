package br.com.estudante.bruna.servico;

import br.com.estudante.bruna.dominio.Conta;
import br.com.estudante.bruna.dominio.Cpf;
import br.com.estudante.bruna.dominio.OperacaoInvalidaException;
import br.com.estudante.bruna.dominio.SenhaHash;
import br.com.estudante.bruna.repositorio.ContaRepositorio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Casos de uso do banco: abrir conta, entrar, transferir. */
public class BancoServico {

    static final int MAXIMO_DE_TENTATIVAS = 5;
    static final Duration TEMPO_DE_BLOQUEIO = Duration.ofMinutes(15);
    // Mesma mensagem para CPF inexistente e senha errada: não revela quais CPFs têm conta.
    static final String LOGIN_INVALIDO = "CPF ou senha incorretos.";

    private record Tentativas(int falhas, Instant bloqueadoAte) {}

    private final ContaRepositorio repositorio;
    private final Clock relogio;
    private final Map<Cpf, Tentativas> tentativas = new ConcurrentHashMap<>();

    public BancoServico(ContaRepositorio repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public Conta abrirConta(String nome, String cpfTexto, char[] senha) {
        Cpf cpf = Cpf.de(cpfTexto);
        if (repositorio.porCpf(cpf).isPresent()) {
            throw new OperacaoInvalidaException("Já existe uma conta para este CPF.");
        }
        Conta conta = new Conta(repositorio.proximoNumero(), nome, cpf, SenhaHash.criar(senha), relogio);
        try {
            repositorio.salvar(conta);
        } catch (IllegalStateException e) {
            throw new OperacaoInvalidaException("Já existe uma conta para este CPF.");
        }
        return conta;
    }

    /** Autentica; após 5 erros seguidos, bloqueia o CPF por 15 minutos (freia tentativas de adivinhar a senha). */
    public Conta entrar(String cpfTexto, char[] senha) {
        Cpf cpf;
        try {
            cpf = Cpf.de(cpfTexto);
        } catch (OperacaoInvalidaException e) {
            throw new OperacaoInvalidaException(LOGIN_INVALIDO);
        }
        Instant agora = relogio.instant();
        Tentativas t = tentativas.get(cpf);
        if (t != null && t.bloqueadoAte() != null && agora.isBefore(t.bloqueadoAte())) {
            throw new OperacaoInvalidaException("Muitas tentativas incorretas. Tente novamente mais tarde.");
        }

        Conta conta = repositorio.porCpf(cpf).orElse(null);
        // Mesmo sem conta, calcula um hash para o tempo de resposta não revelar se o CPF existe.
        boolean ok = conta != null ? conta.senhaConfere(senha) : SenhaFalsa.confere(senha);
        if (!ok) {
            registrarFalha(cpf, t, agora);
            throw new OperacaoInvalidaException(LOGIN_INVALIDO);
        }
        tentativas.remove(cpf);
        return conta;
    }

    public void transferir(Conta origem, String cpfDestino, BigDecimal valor) {
        Conta destino = repositorio.porCpf(Cpf.de(cpfDestino))
                .orElseThrow(() -> new OperacaoInvalidaException("Destinatário não encontrado."));
        Conta.transferir(origem, destino, valor);
    }

    private void registrarFalha(Cpf cpf, Tentativas anterior, Instant agora) {
        int falhas = (anterior == null ? 0 : anterior.falhas()) + 1;
        Instant ate = falhas >= MAXIMO_DE_TENTATIVAS ? agora.plus(TEMPO_DE_BLOQUEIO) : null;
        tentativas.put(cpf, new Tentativas(ate != null ? 0 : falhas, ate));
    }

    /** Hash descartável usado só para gastar o mesmo tempo quando o CPF não existe. */
    private static final class SenhaFalsa {
        private static final SenhaHash FALSA = SenhaHash.criar("senha-descartavel".toCharArray());

        static boolean confere(char[] tentativa) {
            FALSA.confere(tentativa);
            return false;
        }
    }
}
