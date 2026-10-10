package bank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bank.model.Transaction;
import bank.model.Transaction.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ColorsTest {

    @Test
    void paintsOnlyWhenEnabled() {
        assertEquals("\u001B[32m+ $10.00\u001B[0m", Colors.paint("+ $10.00", "\u001B[32m", true));
        assertEquals("+ $10.00", Colors.paint("+ $10.00", "\u001B[32m", false));
    }

    @Test
    void statementLineKeepsTheTextWhenColorsAreOff() {
        // tests run with the output redirected, so there's no console and the colors are off
        Transaction t = new Transaction("ACC1", null, TransactionType.DEPOSIT,
            new BigDecimal("25.00"), new BigDecimal("125.00"), "Deposit");
        assertEquals(Colors.ENABLED ? Main.colored(t) : t.toString(), Main.colored(t));
    }
}
