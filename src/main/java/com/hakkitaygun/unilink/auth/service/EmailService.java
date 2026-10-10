package com.hakkitaygun.unilink.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.verification-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(String to, String rawToken) {
        String text = """
                UniLink hesabınızı doğrulamak için aşağıdaki bağlantıyı kullanın:

                %s/api/auth/verify-email?token=%s

                Bu bağlantı 24 saat geçerlidir.
                """.formatted(baseUrl, rawToken);

        send(to, "UniLink e-posta doğrulama", text);
    }

    public void sendPasswordResetEmail(String to, String rawToken) {
        String text = """
                UniLink şifrenizi sıfırlamak için aşağıdaki bağlantıyı kullanın:

                %s/reset-password?token=%s

                Bu bağlantı 1 saat geçerlidir. Bu isteği siz yapmadıysanız bu e-postayı yok sayabilirsiniz.
                """.formatted(frontendUrl, rawToken);

        send(to, "UniLink şifre sıfırlama", text);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}