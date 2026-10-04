package br.com.estudante.bruna.console;

import br.com.estudante.bruna.repositorio.ContaRepositorioEmMemoria;
import br.com.estudante.bruna.servico.BancoServico;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MenuTest {

    private String rodar(String entrada) {
        var out = new ByteArrayOutputStream();
        var saida = new PrintStream(out, true, StandardCharsets.UTF_8);
        var banco = new BancoServico(new ContaRepositorioEmMemoria(), Clock.systemUTC());
        new Menu(banco, new Scanner(entrada), saida, null).executar();
        return out.toString(StandardCharsets.UTF_8);
    }

    private static final String ABRIR = "2\nAna Souza\n529.982.247-25\nsenha123\nsenha123\n";
    private static final String ENTRAR = "1\n52998224725\nsenha123\n";

    @Test
    void fluxoCompletoAbrirContaDepositarSacarEVerExtrato() {
        String saida = rodar(ABRIR + ENTRAR + "4\n100,50\n3\n20.25\n1\n2\n6\n3\n");

        assertTrue(saida.contains("Conta aberta"));
        assertTrue(saida.contains("Saldo: R$ 80.25"));
        assertTrue(saida.contains("DEPOSITO"));
        assertTrue(saida.contains("SAQUE"));
    }

    @Test
    void valorNaoNumericoNaoDerrubaOPrograma() {
        // O original quebrava com InputMismatchException.
        String saida = rodar(ABRIR + ENTRAR + "4\nabc\n1\n6\n3\n");

        assertTrue(saida.contains("Valor inválido"));
        assertTrue(saida.contains("Saldo: R$ 0.00"));
    }

    @Test
    void saqueAcimaDoSaldoEhRecusadoComMensagem() {
        String saida = rodar(ABRIR + ENTRAR + "3\n10\n6\n3\n");
        assertTrue(saida.contains("Saldo insuficiente"));
    }

    @Test
    void depositoNegativoEhRecusado() {
        String saida = rodar(ABRIR + ENTRAR + "4\n-50\n1\n6\n3\n");
        assertTrue(saida.contains("maior que zero"));
        assertTrue(saida.contains("Saldo: R$ 0.00"));
    }

    @Test
    void senhasDiferentesNaoCriamConta() {
        String saida = rodar("2\nAna\n52998224725\nsenha123\noutra1234\n3\n");
        assertTrue(saida.contains("não coincidem"));
        assertFalse(saida.contains("Conta aberta"));
    }

    @Test
    void loginInvalidoMostraMensagemGenerica() {
        String saida = rodar("1\n52998224725\nqualquer1\n3\n");
        assertTrue(saida.contains("CPF ou senha incorretos"));
    }

    @Test
    void fimDaEntradaEncerraSemTravar() {
        assertDoesNotThrow(() -> rodar(""));
        assertDoesNotThrow(() -> rodar("1\n"));
    }
}
