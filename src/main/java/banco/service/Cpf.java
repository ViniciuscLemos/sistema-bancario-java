package banco.service;

/** Validação de CPF pelos dois dígitos verificadores. */
public final class Cpf {

    private Cpf() {}

    public static String limpar(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String digitos = limpar(cpf);
        if (digitos.length() != 11) return false;
        // 111.111.111-11 e parecidos passam na conta mas não valem
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
