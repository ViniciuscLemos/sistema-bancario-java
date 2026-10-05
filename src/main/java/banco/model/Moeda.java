package banco.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitários para valores em reais.
 *
 * Locale "pt-BR" usa vírgula decimal e ponto de milhar: R$ 1.234,56
 */
public final class Moeda {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Moeda() {}

    public static String formatar(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(PT_BR);
        // O Java usa um espaço especial (não separável) depois do "R$"; trocamos por um comum
        return formato.format(valor).replace(' ', ' ');
    }

    /**
     * Converte o texto digitado pelo usuário em BigDecimal.
     * Aceita "1500", "1500,50", "1.500,50" e "1500.50".
     *
     * @throws NumberFormatException se o texto não for um número
     */
    public static BigDecimal parse(String texto) {
        String limpo = texto.trim().replace("R$", "").replace(" ", "");
        if (limpo.contains(",")) {
            // Formato brasileiro: ponto é milhar, vírgula é decimal
            limpo = limpo.replace(".", "").replace(",", ".");
        } else if (limpo.matches("\\d{1,3}(\\.\\d{3})+")) {
            // "1.500" ou "1.000.000": pontos só como separador de milhar
            limpo = limpo.replace(".", "");
        }
        return new BigDecimal(limpo);
    }
}
