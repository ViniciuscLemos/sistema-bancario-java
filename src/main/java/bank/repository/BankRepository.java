package bank.repository;

import bank.model.Account;
import bank.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** All the SQL in the system lives here. */
public class BankRepository {

    private final Connection conn;

    public BankRepository(Connection conn) {
        this.conn = conn;
        createTables();
    }

    private void createTables() {
        try (Statement stmt = conn.createStatement()) {
            // money goes in as TEXT ("1500.00"); REAL is floating point and loses precision
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id          TEXT PRIMARY KEY,
                    holder      TEXT NOT NULL,
                    cpf         TEXT UNIQUE NOT NULL,
                    type        TEXT NOT NULL,
                    balance     TEXT NOT NULL DEFAULT '0.00',
                    active      INTEGER NOT NULL DEFAULT 1,
                    created_at  TEXT NOT NULL
                )
            """);

            // account_id is the account that owns the statement line;
            // other_account_id is the other account in a transfer (or NULL)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id                TEXT PRIMARY KEY,
                    account_id        TEXT NOT NULL REFERENCES accounts(id),
                    other_account_id  TEXT,
                    type              TEXT NOT NULL,
                    amount            TEXT NOT NULL,
                    balance_after     TEXT NOT NULL,
                    description       TEXT,
                    created_at        TEXT NOT NULL
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_id)");
        } catch (SQLException e) {
            throw new RuntimeException("Error creating tables: " + e.getMessage(), e);
        }
    }

    /** Runs everything in one transaction: if an exception happens halfway, it rolls back. */
    public <T> T inTransaction(Supplier<T> operation) {
        try {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = operation.get();
                conn.commit();
                return result;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction error: " + e.getMessage(), e);
        }
    }

    public void saveAccount(Account account) {
        String sql = """
            INSERT INTO accounts (id, holder, cpf, type, balance, active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getId());
            ps.setString(2, account.getHolder());
            ps.setString(3, account.getCpf());
            ps.setString(4, account.getType().name());
            ps.setString(5, account.getBalance().toPlainString());
            ps.setInt(6, account.isActive() ? 1 : 0);
            ps.setString(7, account.getCreatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving account: " + e.getMessage(), e);
        }
    }

    public void updateAccount(Account account) {
        String sql = "UPDATE accounts SET balance = ?, active = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getBalance().toPlainString());
            ps.setInt(2, account.isActive() ? 1 : 0);
            ps.setString(3, account.getId());
            if (ps.executeUpdate() != 1) {
                // if nothing changed, better to fail than to make money disappear silently
                throw new IllegalStateException("Account not found to update: " + account.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating account: " + e.getMessage(), e);
        }
    }

    public Optional<Account> findById(String id) {
        return findOne("SELECT * FROM accounts WHERE id = ?", id);
    }

    public Optional<Account> findByCpf(String cpf) {
        return findOne("SELECT * FROM accounts WHERE cpf = ?", cpf);
    }

    private Optional<Account> findOne(String sql, String param) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up account: " + e.getMessage(), e);
        }
    }

    public List<Account> listAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY holder";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                accounts.add(mapAccount(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing accounts: " + e.getMessage(), e);
        }
        return accounts;
    }

    public void saveTransaction(Transaction t) {
        String sql = """
            INSERT INTO transactions (id, account_id, other_account_id, type, amount, balance_after, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getAccountId());
            ps.setString(3, t.getOtherAccountId());
            ps.setString(4, t.getType().name());
            ps.setString(5, t.getAmount().toPlainString());
            ps.setString(6, t.getBalanceAfter().toPlainString());
            ps.setString(7, t.getDescription());
            ps.setString(8, t.getCreatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving transaction: " + e.getMessage(), e);
        }
    }

    /** Account statement, newest first. */
    public List<Transaction> listStatement(String accountId) {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT * FROM transactions
            WHERE account_id = ?
            ORDER BY created_at DESC, rowid DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading statement: " + e.getMessage(), e);
        }
        return list;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        return new Account(
            rs.getString("id"),
            rs.getString("holder"),
            rs.getString("cpf"),
            Account.AccountType.valueOf(rs.getString("type")),
            new BigDecimal(rs.getString("balance")),
            rs.getInt("active") == 1,
            LocalDateTime.parse(rs.getString("created_at"))
        );
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getString("id"),
            rs.getString("account_id"),
            rs.getString("other_account_id"),
            Transaction.TransactionType.valueOf(rs.getString("type")),
            new BigDecimal(rs.getString("amount")),
            new BigDecimal(rs.getString("balance_after")),
            rs.getString("description"),
            LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
