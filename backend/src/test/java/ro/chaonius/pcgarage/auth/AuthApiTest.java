package ro.chaonius.pcgarage.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    private static final String ADMIN_EMAIL = "admin@pcgarage.local";
    private static final String ADMIN_PASSWORD = "AdminPass123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureAdminAccount() {
        accounts.deleteAll();
        accounts.save(new Account(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), UserRole.ADMIN));
    }

    @Test
    void registrationAlwaysCreatesAClientAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.test",
                                  "password": "ClientPass123!",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void anonymousUsersMayReadButCannotChangeCatalog() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Periferice\" }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientTokenCannotWriteButAdminTokenCan() throws Exception {
        registerClient();
        String clientToken = login("client@example.test", "ClientPass123!");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Componente client test\" }"))
                .andExpect(status().isForbidden());

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Componente admin test\" }"))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidCredentialsDoNotIssueAToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRejectsPasswordsOverBcryptByteLimit() throws Exception {
        String password = "🔒".repeat(24);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unicode@example.test",
                                  "password": "%s"
                                }
                                """.formatted(password)))
                .andExpect(status().isBadRequest());
    }

    private void registerClient() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.test",
                                  "password": "ClientPass123!"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
