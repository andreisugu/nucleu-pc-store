package ro.chaonius.pcgarage.auth;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
class AdminAccountBootstrap {

    @Bean
    ApplicationRunner bootstrapAdmin(
            AccountRepository accounts,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        return arguments -> {
            String email = environment.getProperty("ADMIN_EMAIL", "").trim();
            String password = environment.getProperty("ADMIN_PASSWORD", "");
            if (email.isEmpty() && password.isEmpty()) {
                return;
            }
            if (email.isEmpty() || password.isBlank() || password.length() < 12
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(
                        "Configurează ADMIN_EMAIL și ADMIN_PASSWORD (12–72 caractere și maximum 72 bytes UTF-8)");
            }

            Account existing = accounts.findByEmailIgnoreCase(email).orElse(null);
            if (existing == null) {
                accounts.save(new Account(email, passwordEncoder.encode(password), UserRole.ADMIN));
            } else if (existing.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("ADMIN_EMAIL aparține deja unui cont de client");
            }
        };
    }
}
