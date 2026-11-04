package ro.chaonius.pcgarage.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
@Transactional
class AuthService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    AuthService(AccountRepository accounts, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    AccountResponse register(AuthRequest request) {
        if (bcryptInputTooLong(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parola trebuie să aibă cel mult 72 bytes UTF-8");
        }
        if (accounts.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Adresa de email este deja înregistrată");
        }
        Account account = new Account(
                request.email(), passwordEncoder.encode(request.password()), UserRole.CLIENT);
        return AccountResponse.from(accounts.save(account));
    }

    TokenResponse login(AuthRequest request) {
        if (bcryptInputTooLong(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Date de autentificare invalide");
        }
        Account account = accounts.findByEmailIgnoreCase(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Date de autentificare invalide"));
        return tokens.issueFor(account);
    }

    private boolean bcryptInputTooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > 72;
    }
}
