package banco.service;

/**
 * Validação de CPF pelos dígitos verificadores.
 *
 * Os dois últimos dígitos do CPF são calculados a partir dos nove primeiros:
 * cada dígito é multiplicado por um peso decrescente, soma-se tudo e o resto
 * da divisão por 11 define o verificador.
 */
public final class Cpf {

    private Cpf() {}

    /** Remove pontos, traços e espaços: "123.456.789-09" → "12345678909" */
    public static String limpar(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String digitos = limpar(cpf);
        if (digitos.length() != 11) return false;
        // 000.000.000-00, 111.111.111-11... passam na conta, mas não são CPFs válidos
        if (digitos.chars().distinct().count() == 1) return false;

        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
            && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
