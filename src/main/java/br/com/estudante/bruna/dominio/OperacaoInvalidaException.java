package br.com.estudante.bruna.dominio;

/** Operação bancária recusada por violar uma regra (valor inválido, saldo insuficiente, dado incorreto...). */
public class OperacaoInvalidaException extends RuntimeException {

    public OperacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
