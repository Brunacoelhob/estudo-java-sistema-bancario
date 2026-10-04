package br.com.estudante.bruna.servico;

import br.com.estudante.bruna.dominio.Conta;
import br.com.estudante.bruna.dominio.OperacaoInvalidaException;
import br.com.estudante.bruna.repositorio.ContaRepositorioEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class BancoServicoTest {

    static final String CPF_A = "529.982.247-25";
    static final String CPF_B = "111.444.777-35";

    /** Relógio que anda só quando o teste manda. */
    static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-01-01T12:00:00Z");
        public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId zone) { return this; }
        public Instant instant() { return agora; }
        void avancar(Duration d) { agora = agora.plus(d); }
    }

    RelogioManual relogio;
    BancoServico banco;

    @BeforeEach
    void preparar() {
        relogio = new RelogioManual();
        banco = new BancoServico(new ContaRepositorioEmMemoria(), relogio);
    }

    private static char[] s(String texto) { return texto.toCharArray(); }

    @Test
    void abreContaENumerosSaoSequenciaisEUnicos() {
        Conta a = banco.abrirConta("Ana", CPF_A, s("senha123"));
        Conta b = banco.abrirConta("Beto", CPF_B, s("senha123"));

        assertNotEquals(a.numero(), b.numero());
    }

    @Test
    void naoPermiteDuasContasParaOMesmoCpf() {
        banco.abrirConta("Ana", CPF_A, s("senha123"));
        assertThrows(OperacaoInvalidaException.class, () -> banco.abrirConta("Outra Ana", "52998224725", s("senha123")));
    }

    @Test
    void recusaCpfInvalidoESenhaFraca() {
        assertThrows(OperacaoInvalidaException.class, () -> banco.abrirConta("Ana", "123.456.789-00", s("senha123")));
        assertThrows(OperacaoInvalidaException.class, () -> banco.abrirConta("Ana", CPF_A, s("123")));
        assertThrows(OperacaoInvalidaException.class, () -> banco.abrirConta("  ", CPF_A, s("senha123")));
    }

    @Test
    void loginCorretoRetornaAConta() {
        Conta criada = banco.abrirConta("Ana", CPF_A, s("senha123"));
        assertSame(criada, banco.entrar(CPF_A, s("senha123")));
    }

    @Test
    void loginErradoNaoRevelaSeOCpfExiste() {
        banco.abrirConta("Ana", CPF_A, s("senha123"));

        var senhaErrada = assertThrows(OperacaoInvalidaException.class, () -> banco.entrar(CPF_A, s("errada!!")));
        var cpfInexistente = assertThrows(OperacaoInvalidaException.class, () -> banco.entrar(CPF_B, s("senha123")));
        var cpfMalformado = assertThrows(OperacaoInvalidaException.class, () -> banco.entrar("abc", s("senha123")));

        assertEquals(senhaErrada.getMessage(), cpfInexistente.getMessage());
        assertEquals(senhaErrada.getMessage(), cpfMalformado.getMessage());
    }

    @Test
    void bloqueiaAposCincoErrosEDesbloqueiaDepoisDoTempo() {
        banco.abrirConta("Ana", CPF_A, s("senha123"));
        for (int i = 0; i < BancoServico.MAXIMO_DE_TENTATIVAS; i++) {
            assertThrows(OperacaoInvalidaException.class, () -> banco.entrar(CPF_A, s("errada!!")));
        }

        // Mesmo a senha CERTA é recusada durante o bloqueio
        var bloqueado = assertThrows(OperacaoInvalidaException.class, () -> banco.entrar(CPF_A, s("senha123")));
        assertTrue(bloqueado.getMessage().contains("Muitas tentativas"));

        relogio.avancar(BancoServico.TEMPO_DE_BLOQUEIO.plusSeconds(1));
        assertNotNull(banco.entrar(CPF_A, s("senha123")));
    }

    @Test
    void acertarZeraOContadorDeErros() {
        banco.abrirConta("Ana", CPF_A, s("senha123"));
        for (int rodada = 0; rodada < 3; rodada++) {
            for (int i = 0; i < BancoServico.MAXIMO_DE_TENTATIVAS - 1; i++) {
                assertThrows(OperacaoInvalidaException.class, () -> banco.entrar(CPF_A, s("errada!!")));
            }
            assertNotNull(banco.entrar(CPF_A, s("senha123")), "rodada " + rodada + ": 4 erros + acerto não bloqueia");
        }
    }

    @Test
    void transferePorCpfEValidaDestinatario() {
        Conta a = banco.abrirConta("Ana", CPF_A, s("senha123"));
        Conta b = banco.abrirConta("Beto", CPF_B, s("senha123"));
        a.depositar(new BigDecimal("100"));

        banco.transferir(a, CPF_B, new BigDecimal("30"));

        assertEquals(new BigDecimal("70.00"), a.saldo());
        assertEquals(new BigDecimal("30.00"), b.saldo());
        assertThrows(OperacaoInvalidaException.class, () -> banco.transferir(a, "390.533.447-05", BigDecimal.ONE));
        assertThrows(OperacaoInvalidaException.class, () -> banco.transferir(a, "000", BigDecimal.ONE));
    }
}
