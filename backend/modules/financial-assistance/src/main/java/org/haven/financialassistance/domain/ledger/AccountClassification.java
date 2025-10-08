package org.haven.financialassistance.domain.ledger;

public enum AccountClassification {
    // Asset accounts (debit increases)
    CASH_ASSET("1000", "Cash and Cash Equivalents"),
    SECURITY_DEPOSIT_ASSET("1100", "Security Deposits Paid"),
    ACCOUNTS_RECEIVABLE("1200", "Accounts Receivable"),

    // Liability accounts (credit increases)
    FUNDING_LIABILITY("2000", "Grant Funding Received"),
    ACCOUNTS_PAYABLE("2100", "Accounts Payable"),
    ACCRUED_EXPENSES("2200", "Accrued Expenses"),

    // Expense accounts (debit increases)
    RENT_EXPENSE("5000", "Rent Payments"),
    UTILITY_EXPENSE("5100", "Utility Payments"),
    MOVING_EXPENSE("5200", "Moving and Relocation Costs"),
    OTHER_EXPENSE("5900", "Other Assistance Expenses"),

    // Revenue accounts (credit increases)
    GRANT_REVENUE("4000", "Grant Revenue"),
    OTHER_REVENUE("4900", "Other Revenue");

    private final String accountCode;
    private final String description;

    AccountClassification(String accountCode, String description) {
        this.accountCode = accountCode;
        this.description = description;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAsset() {
        return accountCode.startsWith("1");
    }

    public boolean isLiability() {
        return accountCode.startsWith("2");
    }

    public boolean isRevenue() {
        return accountCode.startsWith("4");
    }

    public boolean isExpense() {
        return accountCode.startsWith("5");
    }

    public boolean normallyIncreasesByDebit() {
        return isAsset() || isExpense();
    }

    public boolean normallyIncreasesByCredit() {
        return isLiability() || isRevenue();
    }
}