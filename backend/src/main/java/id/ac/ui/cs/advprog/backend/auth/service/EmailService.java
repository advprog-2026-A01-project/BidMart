package id.ac.ui.cs.advprog.backend.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Kosongkan dependency jika tidak pakai Gmail SMTP sama sekali
    public EmailService() {}

    public void sendVerificationToken(final String to, final String token) {
        log.info("\n" +
                "╔==========================================================╗\n" +
                "   📧 [{}] VERIFIKASI AKUN\n" +
                "   Copy Token di bawah ini:\n" +
                "   --> {} <-\n" +
                "╚==========================================================╝", to, token);
    }

    public void sendMfaOtp(final String to, final String otp) {
        log.info("\n" +
                "╔==========================================================╗\n" +
                "   📧 [{}] KODE 2FA (OTP)\n" +
                "   Copy Kode di bawah ini:\n" +
                "   --> {} <-\n" +
                "╚==========================================================╝", to, otp);
    }
}
