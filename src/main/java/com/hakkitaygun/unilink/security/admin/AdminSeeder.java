package com.hakkitaygun.unilink.security.admin;

import com.hakkitaygun.unilink.user.entity.User;
import com.hakkitaygun.unilink.user.enums.Role;
import com.hakkitaygun.unilink.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${app.security.admin.seed-enabled:false}") boolean enabled,
                       @Value("${app.security.admin.seed-email:}") String email,
                       @Value("${app.security.admin.seed-password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (email.isBlank() || password.isBlank()) {
            log.warn("Admin seed açık ama seed-email/seed-password boş; atlanıyor.");
            return;
        }
        String norm = User.normalizeEmail(email);
        users.findByEmail(norm).ifPresentOrElse(u -> {
            if (u.getRole() != Role.ADMIN) {
                u.setRole(Role.ADMIN);
                u.setEmailVerified(true);
                users.save(u);
                log.info("Mevcut kullanıcı admin yapıldı: {}", norm);
            }
        }, () -> {
            User admin = User.builder()
                    .email(norm)                        // builder normalize etmez; normalize edilmiş veriyoruz
                    .passwordHash(encoder.encode(password))
                    .role(Role.ADMIN)
                    .emailVerified(true)
                    .build();
            users.save(admin);
            log.info("Admin seed oluşturuldu: {}", norm);
        });
    }
}