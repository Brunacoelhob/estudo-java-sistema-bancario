package br.com.estudante.bruna.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ContaTest {

    private static final SenhaHash SENHA = SenhaHash.criar("segredo123".toCharArray());

    private static Conta conta(int numero, String cpf) {
        return new Conta(numero, "Titular " + numero, Cpf.de(cpf), SENHA, Clock.systemUTC());
    }

    private static BigDecimal v(String s) { return new BigDecimal(s); }

    @Test
    void depositoESaqueAtualizamSaldoEExtrato() {
        Conta c = conta(1, "52998224725");
        c.depositar(v("100.50"));
        c.sacar(v("40.25"));

        assertEquals(v("60.25"), c.saldo());
        assertEquals(2, c.extrato().size());
        assertEquals(Transacao.Tipo.SAQUE, c.extrato().get(1).tipo());
        assertEquals(v("60.25"), c.extrato().get(1).saldoApos());
    }

    @Test
    void naoHaErroDeArredondamento() {
        Conta c = conta(1, "52998224725");
        c.depositar(v("0.10"));
        c.depositar(v("0.20"));
        assertEquals(v("0.30"), c.saldo(), "com double isso daria 0.30000000000000004");
    }

    @Test
    void recusaValoresInvalidos() {
        Conta c = conta(1, "52998224725");
        for (String ruim : new String[]{"0", "-5", "10.001"}) {
            assertThrows(OperacaoInvalidaException.class, () -> c.depositar(v(ruim)), "depositar " + ruim);
            assertThrows(OperacaoInvalidaException.class, () -> c.sacar(v(ruim)), "sacar " + ruim);
        }
        assertThrows(OperacaoInvalidaException.class, () -> c.depositar(null));
        assertEquals(v("0.00"), c.saldo());
    }

    @Test
    void saldoNuncaFicaNegativo() {
        Conta c = conta(1, "52998224725");
        c.depositar(v("50"));
        var erro = assertThrows(OperacaoInvalidaException.class, () -> c.sacar(v("50.01")));
        assertTrue(erro.getMessage().contains("insuficiente"));
        assertEquals(v("50.00"), c.saldo());
    }

    @Test
    void transferenciaMoveOValorEAmbosOsExtratosRegistram() {
        Conta a = conta(1, "52998224725");
        Conta b = conta(2, "11144477735");
        a.depositar(v("200"));

        Conta.transferir(a, b, v("75.5"));

        assertEquals(v("124.50"), a.saldo());
        assertEquals(v("75.50"), b.saldo());
        assertEquals(Transacao.Tipo.TRANSFERENCIA_ENVIADA, a.extrato().get(1).tipo());
        assertEquals(Transacao.Tipo.TRANSFERENCIA_RECEBIDA, b.extrato().get(0).tipo());
    }

    @Test
    void transferenciaInvalidaNaoAlteraNada() {
        Conta a = conta(1, "52998224725");
        Conta b = conta(2, "11144477735");
        a.depositar(v("10"));

        assertThrows(OperacaoInvalidaException.class, () -> Conta.transferir(a, b, v("11")));
        assertThrows(OperacaoInvalidaException.class, () -> Conta.transferir(a, a, v("1")), "para si mesma");
        assertEquals(v("10.00"), a.saldo());
        assertEquals(v("0.00"), b.saldo());
        assertTrue(b.extrato().isEmpty());
    }

    @Test
    void extratoRetornadoNaoPodeSerAlteradoDeFora() {
        Conta c = conta(1, "52998224725");
        c.depositar(v("1"));
        assertThrows(UnsupportedOperationException.class, () -> c.extrato().clear());
    }

    @Test
    void transferenciasCruzadasConcorrentesNaoTravamENemCriamDinheiro() throws Exception {
        Conta a = conta(1, "52998224725");
        Conta b = conta(2, "11144477735");
        a.depositar(v("1000"));
        b.depositar(v("1000"));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> tarefas = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            final boolean ida = i % 2 == 0;
            tarefas.add(pool.submit(() -> {
                try {
                    if (ida) Conta.transferir(a, b, v("3")); else Conta.transferir(b, a, v("2"));
                } catch (OperacaoInvalidaException ignorada) {
                    // saldo insuficiente em algum momento é legítimo
                }
            }));
        }
        for (Future<?> t : tarefas) {
            t.get(10, TimeUnit.SECONDS); // se houvesse deadlock, estouraria o tempo aqui
        }
        pool.shutdownNow();

        assertEquals(v("2000.00"), a.saldo().add(b.saldo()), "o dinheiro total se conserva");
        assertTrue(a.saldo().signum() >= 0 && b.saldo().signum() >= 0);
    }
}
