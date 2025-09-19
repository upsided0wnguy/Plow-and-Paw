package database;

import main.models.Loan;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    public static void createLoan(String username, int amount, int installments, int bonusAmount) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO user_loans (username, amount, installments, paid_installments, total_paid, eligible_reward, got_reward, bonus_amount) VALUES (?, ?, ?, 0, 0, TRUE, FALSE, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setInt(2, amount);
            ps.setInt(3, installments);
            ps.setInt(4, bonusAmount);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // >>> FIXED: Returns List<Loan>
    public static List<Loan> getActiveLoans(String username) {
        List<Loan> result = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT * FROM user_loans WHERE username=? AND got_reward=FALSE";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Loan loan = new Loan();
                loan.loanId = rs.getInt("loan_id");
                loan.amount = rs.getInt("amount");
                loan.installments = rs.getInt("installments");
                loan.paidInstallments = rs.getInt("paid_installments");
                loan.totalPaid = rs.getInt("total_paid");
                loan.eligibleReward = rs.getBoolean("eligible_reward");
                loan.gotReward = rs.getBoolean("got_reward");
                loan.bonusAmount = rs.getInt("bonus_amount");
                result.add(loan);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static void payInstallment(int loanId, int installmentAmount) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sel = "SELECT paid_installments, total_paid, installments, amount, eligible_reward FROM user_loans WHERE loan_id=?";
            PreparedStatement ps = conn.prepareStatement(sel);
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            int paidInstallments = rs.getInt("paid_installments") + 1;
            int totalPaid = rs.getInt("total_paid") + installmentAmount;
            int installments = rs.getInt("installments");
            int amount = rs.getInt("amount");
            boolean eligibleReward = rs.getBoolean("eligible_reward");

            if (paidInstallments > installments || totalPaid > amount) {
                eligibleReward = false;
            }

            String upd = "UPDATE user_loans SET paid_installments=?, total_paid=?, eligible_reward=? WHERE loan_id=?";
            PreparedStatement ps2 = conn.prepareStatement(upd);
            ps2.setInt(1, paidInstallments);
            ps2.setInt(2, totalPaid);
            ps2.setBoolean(3, eligibleReward);
            ps2.setInt(4, loanId);
            ps2.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void claimReward(int loanId) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String upd = "UPDATE user_loans SET got_reward=TRUE WHERE loan_id=?";
            PreparedStatement ps = conn.prepareStatement(upd);
            ps.setInt(1, loanId);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void deleteAllLoansForUser(String username) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "DELETE FROM user_loans WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}