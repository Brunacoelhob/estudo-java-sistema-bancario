package br.com.estudante.bruna.repositorio;

import br.com.estudante.bruna.dominio.Conta;
import br.com.estudante.bruna.dominio.Cpf;

import java.util.Optional;

/** Onde as contas ficam guardadas. Hoje em memória; amanhã poderia ser um banco de dados. */
public interface ContaRepositorio {

    /** Reserva o próximo número de conta, sempre único (antes era Math.random e podia repetir). */
    int proximoNumero();

    /** @throws IllegalStateException se já existir conta para o CPF. */
    void salvar(Conta conta);

    Optional<Conta> porCpf(Cpf cpf);

    Optional<Conta> porNumero(int numero);
}
