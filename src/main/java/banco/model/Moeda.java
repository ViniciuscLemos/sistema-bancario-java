package banco.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatação e leitura de valores em reais (R$ 1.234,56). */
public final class Moeda {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Moeda() {}

    public static String formatar(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(PT_BR);
        // o Java coloca um espaço não separável depois do R$
        return formato.format(valor).replace(' ', ' ');
    }

    /** Aceita "1500", "1500,50", "1.500,50" e "1500.50". */
    public static BigDecimal parse(String texto) {
        String limpo = texto.trim().replace("R$", "").replace(" ", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        } else if (limpo.matches("\\d{1,3}(\\.\\d{3})+")) {
            limpo = limpo.replace(".", "");  // "1.500" = mil e quinhentos
        }
        return new BigDecimal(limpo);
    }
}
