package br.com.estudante.bruna.dominio;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Senha guardada apenas como hash PBKDF2-HMAC-SHA256 com sal aleatório. A senha original nunca é
 * armazenada (antes ficava em texto puro na memória e era comparada com equals).
 */
public final class SenhaHash {

    private static final int ITERACOES = 210_000; // recomendação OWASP para PBKDF2-SHA256
    private static final int BITS_DO_HASH = 256;
    private static final int TAMANHO_DO_SAL = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final byte[] sal;
    private final byte[] hash;

    private SenhaHash(byte[] sal, byte[] hash) {
        this.sal = sal;
        this.hash = hash;
    }

    /** Atenção: o array recebido é zerado após o uso (limpeza de memória); não o reutilize. */
    public static SenhaHash criar(char[] senha) {
        validarForca(senha);
        byte[] sal = new byte[TAMANHO_DO_SAL];
        ALEATORIO.nextBytes(sal);
        return new SenhaHash(sal, derivar(senha, sal));
    }

    /** Atenção: o array recebido é zerado após o uso (limpeza de memória); não o reutilize. */
    public boolean confere(char[] tentativa) {
        // MessageDigest.isEqual compara em tempo constante (não revela quantos bytes acertaram).
        return tentativa != null && MessageDigest.isEqual(hash, derivar(tentativa, sal));
    }

    static void validarForca(char[] senha) {
        if (senha == null || senha.length < 6) {
            throw new OperacaoInvalidaException("A senha deve ter pelo menos 6 caracteres.");
        }
    }

    private static byte[] derivar(char[] senha, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(senha, sal, ITERACOES, BITS_DO_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível nesta JVM", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(senha, '\0'); // limpa a senha da memória assim que possível
        }
    }
}
