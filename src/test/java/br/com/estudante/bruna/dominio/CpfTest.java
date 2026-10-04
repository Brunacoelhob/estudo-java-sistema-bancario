package br.com.estudante.bruna.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfTest {

    @Test
    void aceitaComESemPontuacao() {
        assertEquals(Cpf.de("529.982.247-25"), Cpf.de("52998224725"));
    }

    @Test
    void preservaZeroAEsquerda() {
        // Com long (como era antes) este CPF viraria 1234567890 e perderia o zero inicial.
        assertEquals("01234567890", Cpf.de("012.345.678-90").numero());
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertThrows(OperacaoInvalidaException.class, () -> Cpf.de("529.982.247-26"));
    }

    @Test
    void rejeitaSequenciasRepetidasTamanhoErradoELetras() {
        for (String ruim : new String[]{"111.111.111-11", "123", "abcdefghijk", "", "   ", "5299822472555"}) {
            assertThrows(OperacaoInvalidaException.class, () -> Cpf.de(ruim), ruim);
        }
        assertThrows(OperacaoInvalidaException.class, () -> Cpf.de(null));
    }

    @Test
    void naoExibeOCpfCompletoEmLogs() {
        Cpf cpf = Cpf.de("52998224725");
        assertEquals("***.982.247-**", cpf.mascarado());
        assertFalse(cpf.toString().contains("52998224725"));
    }
}
