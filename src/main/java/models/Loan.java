package main.models;

public class Loan {
    public int loanId;
    public int amount;
    public int installments;
    public int paidInstallments;
    public int totalPaid;
    public boolean isPaid;
    public boolean gotReward;
    public boolean eligibleReward;
    public int bonusAmount;

    public Loan() { }
    public Loan(int loanId, int amount, int installments, int paidInstallments, int totalPaid, boolean isPaid, int bonusAmount) {
        this.loanId = loanId;
        this.amount = amount;
        this.installments = installments;
        this.paidInstallments = paidInstallments;
        this.totalPaid = totalPaid;
        this.isPaid = isPaid;
        this.gotReward = false;
        this.eligibleReward = true;
        this.bonusAmount = bonusAmount;
    }
}