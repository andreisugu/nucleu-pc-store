package ro.chaonius.pcgarage.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autentificare", description = "Înregistrarea clienților și emiterea token-urilor JWT")
class AuthController {

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @Operation(summary = "Înregistrează un cont de client")
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse register(@Valid @RequestBody AuthRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentifică un utilizator și emite un JWT")
    TokenResponse login(@Valid @RequestBody AuthRequest request) {
        return auth.login(request);
    }
}
