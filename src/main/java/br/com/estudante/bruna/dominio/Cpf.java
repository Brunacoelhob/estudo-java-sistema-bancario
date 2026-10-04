package br.com.estudante.bruna.dominio;

/**
 * CPF válido (11 dígitos com dígitos verificadores corretos). Guardado como texto: como número
 * (long, como era antes) o CPF 01234567890 perderia o zero à esquerda.
 */
public record Cpf(String numero) {

    public Cpf {
        if (numero == null || !numero.matches("\\d{11}") || !digitosVerificadoresCorretos(numero)) {
            throw new OperacaoInvalidaException("CPF inválido.");
        }
    }

    /** Aceita com ou sem pontuação ("123.456.789-09" ou "12345678909"). */
    public static Cpf de(String texto) {
        if (texto == null) {
            throw new OperacaoInvalidaException("CPF inválido.");
        }
        return new Cpf(texto.replaceAll("[.\\-\\s]", ""));
    }

    /** Mostra só parte do número, para não expor o documento completo na tela ou em logs. */
    public String mascarado() {
        return "***." + numero.substring(3, 6) + "." + numero.substring(6, 9) + "-**";
    }

    @Override
    public String toString() {
        return mascarado();
    }

    private static boolean digitosVerificadoresCorretos(String n) {
        if (n.chars().distinct().count() == 1) {
            return false; // 111.111.111-11 etc. passam na conta, mas não são válidos
        }
        return digito(n, 9) == n.charAt(9) - '0' && digito(n, 10) == n.charAt(10) - '0';
    }

    private static int digito(String n, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (n.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
