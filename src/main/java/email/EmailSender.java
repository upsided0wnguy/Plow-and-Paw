package email;

import javax.mail.*;

import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {
    public static boolean sendOTP(String toEmail, String otp) {
        final String fromEmail = "workspace.net.edu@gmail.com"; // Replace with your Gmail
        final String password = "rbykqjelaciqxrxz"; // Generate and use an App Password

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your Plow and Paw OTP");
            message.setText("Your OTP is: " + otp);

            Transport.send(message);
            System.out.println("OTP sent to email: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace(); // Keep this
            return false;
        }
    }

}


