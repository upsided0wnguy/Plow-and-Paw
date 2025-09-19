package main.ui;

import database.LoanDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.models.Loan;
import main.models.User;

import java.util.ArrayList;
import java.util.List;

public class LoanController {

    @FXML private TextField amountField;
    @FXML private Spinner<Integer> installmentsSpinner;
    @FXML private Label gemCostLabel;
    @FXML private Button applyBtn;
    @FXML private Label feedbackLabel;
    @FXML private ListView<String> loansList;
    @FXML private Button payBtn;
    @FXML private Button rewardBtn;

    private User user;
    private Stage dialog;
    private Runnable updateHUD;

    // Authoritative, up-to-date list
    private List<Loan> loans = new ArrayList<>();

    public void setData(User user, Stage dialog, Runnable updateHUD) {
        this.user = user;
        this.dialog = dialog;
        this.updateHUD = updateHUD;
        feedbackLabel.setText("");
        installmentsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 10, 2));
        updateGemCost();

        // Load from DB!
        loadLoans();

        installmentsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateGemCost());
        amountField.textProperty().addListener((obs, oldVal, newVal) -> updateGemCost());

        applyBtn.setOnAction(e -> applyLoan());
        payBtn.setOnAction(e -> payInstallment());
        rewardBtn.setOnAction(e -> getReward());
        updateLoansView();
    }

    private void loadLoans() {
        loans = LoanDAO.getActiveLoans(user.getUsername());
        updateLoansView();
    }

    private int computeGemCost(int amount) {
        if (amount > 0 && amount <= 1000) return 2;
        if (amount > 1000 && amount <= 4000) return 4;
        if (amount > 4000 && amount <= 10000) return 8;
        return -1;
    }

    private void updateGemCost() {
        int amount = parseAmount();
        int gemCost = computeGemCost(amount);
        gemCostLabel.setText(gemCost > 0 ? String.valueOf(gemCost) : "N/A");
    }

    private int parseAmount() {
        try {
            return Integer.parseInt(amountField.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void applyLoan() {
        int amount = parseAmount();
        int inst = installmentsSpinner.getValue();
        int gemsNeeded = computeGemCost(amount);
        if (amount < 100 || amount > 10000) {
            feedbackLabel.setText("Loan amount must be between 100 and 10,000.");
            return;
        }
        if (gemsNeeded < 0) {
            feedbackLabel.setText("Invalid amount.");
            return;
        }
        if (user.getGems() < gemsNeeded) {
            feedbackLabel.setText("Not enough gems (" + gemsNeeded + " needed).");
            return;
        }
        user.setGems(user.getGems() - gemsNeeded);
        user.setCoins(user.getCoins() + amount);

        // Save to DB
        LoanDAO.createLoan(user.getUsername(), amount, inst, (int)(amount*0.2));
        loadLoans(); // Reload all loans

        if (updateHUD != null) updateHUD.run();
        amountField.clear();
    }

    private void updateLoansView() {
        loansList.getItems().clear();
        if (loans == null) return;
        for (Loan loan : loans) {
            String s = String.format(
                    "Loan #%d: %d coins | Installments: %d/%d | Paid: %d/%d | Reward: %s",
                    loan.loanId, loan.amount, loan.paidInstallments, loan.installments,
                    loan.totalPaid, loan.amount,
                    loan.gotReward ? "Claimed" : (loan.eligibleReward ? "Available" : "No")
            );
            loansList.getItems().add(s);
        }
    }

    private Loan getSelectedLoan() {
        int idx = loansList.getSelectionModel().getSelectedIndex();
        if (idx < 0 || loans == null || idx >= loans.size()) return null;
        return loans.get(idx);
    }

    private void payInstallment() {
        Loan loan = getSelectedLoan();
        if (loan == null) {
            feedbackLabel.setText("Select a loan to pay.");
            return;
        }
        if (loan.paidInstallments >= loan.installments) {
            feedbackLabel.setText("Loan already repaid.");
            return;
        }
        int installmentAmt = loan.amount / loan.installments;
        if (user.getCoins() < installmentAmt) {
            feedbackLabel.setText("Not enough coins for installment (" + installmentAmt + " needed).");
            return;
        }
        user.setCoins(user.getCoins() - installmentAmt);
        LoanDAO.payInstallment(loan.loanId, installmentAmt);
        feedbackLabel.setText("Installment paid! Remaining: " + (loan.installments - (loan.paidInstallments + 1)));
        loadLoans(); // Reload all loans

        if (updateHUD != null) updateHUD.run();
    }

    private void getReward() {
        Loan loan = getSelectedLoan();
        if (loan == null) {
            feedbackLabel.setText("Select a loan to claim reward.");
            return;
        }
        if (loan.gotReward) {
            feedbackLabel.setText("Reward already claimed.");
            return;
        }
        if (loan.paidInstallments == loan.installments && loan.eligibleReward) {
            user.setCoins(user.getCoins() + loan.bonusAmount);
            LoanDAO.claimReward(loan.loanId);
            feedbackLabel.setText("Congrats! Bonus " + loan.bonusAmount + " coins added.");
            loadLoans();
            if (updateHUD != null) updateHUD.run();
        } else if (!loan.eligibleReward) {
            feedbackLabel.setText("Installments missed or not on time. No reward.");
        } else {
            feedbackLabel.setText("Repay all installments for reward.");
        }
    }
}