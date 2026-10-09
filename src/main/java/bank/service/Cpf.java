package bank.service;

/** CPF (the Brazilian taxpayer ID) validation using its two check digits. */
public final class Cpf {

    private Cpf() {}

    public static String clean(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean isValid(String cpf) {
        String digits = clean(cpf);
        if (digits.length() != 11) return false;
        // 111.111.111-11 and the like pass the math but aren't valid
        if (digits.chars().distinct().count() == 1) return false;

        return checkDigit(digits, 9) == digits.charAt(9) - '0'
            && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    private static int checkDigit(String digits, int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += (digits.charAt(i) - '0') * (count + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
