package banco;

import banco.model.Conta;
import banco.model.Moeda;
import banco.service.Cpf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Testes unitários das classes de modelo e utilitários (sem banco). */
class ModelTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "11144477735"})
    void cpfsValidos(String cpf) {
        assertTrue(Cpf.valido(cpf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "11111111111", "5299822472", "529982247250", "abc", ""})
    void cpfsInvalidos(String cpf) {
        assertFalse(Cpf.valido(cpf));
    }

    @Test
    void formataEmReais() {
        assertEquals("R$ 1.234,56", Moeda.formatar(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Moeda.formatar(BigDecimal.ZERO));
    }

    @Test
    void leValoresNosFormatosBrasileiroEAmericano() {
        assertEquals(new BigDecimal("1500.50"), Moeda.parse("1.500,50"));
        assertEquals(new BigDecimal("1500.50"), Moeda.parse("1500.50"));
        assertEquals(new BigDecimal("1500.50"), Moeda.parse("R$ 1500,50"));
        assertEquals(new BigDecimal("1500"), Moeda.parse("1.500"));
        assertEquals(new BigDecimal("1.5"), Moeda.parse("1.5"));
        assertEquals(new BigDecimal("10"), Moeda.parse(" 10 "));
        assertThrows(NumberFormatException.class, () -> Moeda.parse("dez"));
    }

    @Test
    void contaRejeitaValoresInvalidos() {
        Conta conta = new Conta("Maria", "52998224725", Conta.TipoConta.CORRENTE, new BigDecimal("10"));
        assertThrows(IllegalArgumentException.class, () -> conta.depositar(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> conta.depositar(new BigDecimal("0.001")));
        assertThrows(IllegalStateException.class, () -> conta.sacar(new BigDecimal("10.01")));
        conta.depositar(new BigDecimal("0.50"));
        assertEquals(new BigDecimal("10.50"), conta.getSaldo());
    }
}
