package br.com.estudante.bruna.dominio;

import java.math.BigDecimal;
import java.time.Instant;

/** Linha do extrato: o que aconteceu, quanto, quando e qual ficou o saldo. */
public record Transacao(Tipo tipo, BigDecimal valor, Instant data, BigDecimal saldoApos) {

    public enum Tipo { DEPOSITO, SAQUE, TRANSFERENCIA_ENVIADA, TRANSFERENCIA_RECEBIDA }
}
