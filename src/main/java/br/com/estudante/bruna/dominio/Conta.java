package br.com.estudante.bruna.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Conta corrente. Regras garantidas aqui (e não no menu): valor sempre positivo e com no máximo
 * 2 casas, saldo nunca negativo, dinheiro em BigDecimal (double acumula erro: 0.1 + 0.2 != 0.3).
 */
public class Conta {

    public static final String AGENCIA = "2808";

    private final int numero;
    private final String nomeTitular;
    private final Cpf cpf;
    private final SenhaHash senha;
    private final Clock relogio;
    private BigDecimal saldo = BigDecimal.ZERO.setScale(2);
    private final List<Transacao> extrato = new ArrayList<>();

    public Conta(int numero, String nomeTitular, Cpf cpf, SenhaHash senha, Clock relogio) {
        if (nomeTitular == null || nomeTitular.isBlank()) {
            throw new OperacaoInvalidaException("O nome do titular é obrigatório.");
        }
        this.numero = numero;
        this.nomeTitular = nomeTitular.trim();
        this.cpf = cpf;
        this.senha = senha;
        this.relogio = relogio;
    }

    public int numero() { return numero; }
    public String nomeTitular() { return nomeTitular; }
    public Cpf cpf() { return cpf; }
    public boolean senhaConfere(char[] tentativa) { return senha.confere(tentativa); }

    public synchronized BigDecimal saldo() {
        return saldo;
    }

    public synchronized List<Transacao> extrato() {
        return Collections.unmodifiableList(new ArrayList<>(extrato));
    }

    public synchronized void depositar(BigDecimal valor) {
        valor = validar(valor);
        saldo = saldo.add(valor);
        registrar(Transacao.Tipo.DEPOSITO, valor);
    }

    public synchronized void sacar(BigDecimal valor) {
        valor = validar(valor);
        debitar(valor);
        registrar(Transacao.Tipo.SAQUE, valor);
    }

    /**
     * Transferência atômica: ou debita e credita, ou não faz nada. Trava as duas contas sempre na mesma
     * ordem (pelo número) para que duas transferências cruzadas (A→B e B→A) nunca travem uma à outra.
     */
    public static void transferir(Conta origem, Conta destino, BigDecimal valor) {
        if (origem == destino || origem.numero == destino.numero) {
            throw new OperacaoInvalidaException("Não é possível transferir para a própria conta.");
        }
        Conta primeira = origem.numero < destino.numero ? origem : destino;
        Conta segunda = primeira == origem ? destino : origem;
        synchronized (primeira) {
            synchronized (segunda) {
                BigDecimal v = origem.validar(valor);
                origem.debitar(v);
                destino.saldo = destino.saldo.add(v);
                origem.registrar(Transacao.Tipo.TRANSFERENCIA_ENVIADA, v);
                destino.registrar(Transacao.Tipo.TRANSFERENCIA_RECEBIDA, v);
            }
        }
    }

    private void debitar(BigDecimal valor) {
        if (saldo.compareTo(valor) < 0) {
            throw new OperacaoInvalidaException("Saldo insuficiente.");
        }
        saldo = saldo.subtract(valor);
    }

    private BigDecimal validar(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new OperacaoInvalidaException("O valor deve ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new OperacaoInvalidaException("O valor deve ter no máximo 2 casas decimais.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void registrar(Transacao.Tipo tipo, BigDecimal valor) {
        extrato.add(new Transacao(tipo, valor, Instant.now(relogio), saldo));
    }
}
