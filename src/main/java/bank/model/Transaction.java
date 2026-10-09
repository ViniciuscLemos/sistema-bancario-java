package bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * One entry in an account statement.
 * A transfer creates two: TRANSFER_OUT on the source and TRANSFER_IN on the target,
 * and otherAccountId keeps the other account.
 */
public class Transaction {

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_OUT,
        TRANSFER_IN
    }

    private final String id;
    private final String accountId;
    private final String otherAccountId;  // null for deposits and withdrawals
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime createdAt;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Transaction(String accountId, String otherAccountId,
                       TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description) {
        this("TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(),
             accountId, otherAccountId, type, amount, balanceAfter, description, LocalDateTime.now());
    }

    /** Transaction read from the database. */
    public Transaction(String id, String accountId, String otherAccountId, TransactionType type,
                       BigDecimal amount, BigDecimal balanceAfter, String description, LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.otherAccountId = otherAccountId;
        this.type = type;
        this.amount = Account.money(amount);
        this.balanceAfter = Account.money(balanceAfter);
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId()               { return id; }
    public String getAccountId()        { return accountId; }
    public String getOtherAccountId()   { return otherAccountId; }
    public TransactionType getType()    { return type; }
    public BigDecimal getAmount()       { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription()      { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isCredit() {
        return type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN;
    }

    @Override
    public String toString() {
        return String.format(
            "%s  %s%12s  Balance: %12s  | %s",
            createdAt.format(FORMATTER),
            isCredit() ? "+" : "-",
            Money.format(amount),
            Money.format(balanceAfter),
            description
        );
    }
}
