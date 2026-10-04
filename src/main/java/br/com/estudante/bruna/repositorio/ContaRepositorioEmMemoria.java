package br.com.estudante.bruna.repositorio;

import br.com.estudante.bruna.dominio.Conta;
import br.com.estudante.bruna.dominio.Cpf;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ContaRepositorioEmMemoria implements ContaRepositorio {

    private final AtomicInteger sequencia = new AtomicInteger(1000);
    private final Map<Cpf, Conta> porCpf = new ConcurrentHashMap<>();
    private final Map<Integer, Conta> porNumero = new ConcurrentHashMap<>();

    @Override
    public int proximoNumero() {
        return sequencia.incrementAndGet();
    }

    @Override
    public void salvar(Conta conta) {
        // putIfAbsent é atômico: dois cadastros simultâneos do mesmo CPF não passam os dois.
        if (porCpf.putIfAbsent(conta.cpf(), conta) != null) {
            throw new IllegalStateException("CPF já possui conta.");
        }
        porNumero.put(conta.numero(), conta);
    }

    @Override
    public Optional<Conta> porCpf(Cpf cpf) {
        return Optional.ofNullable(porCpf.get(cpf));
    }

    @Override
    public Optional<Conta> porNumero(int numero) {
        return Optional.ofNullable(porNumero.get(numero));
    }
}
