package ro.chaonius.pcgarage.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository products;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JwtEncoder jwtEncoder;

    private String adminToken;

    @BeforeEach
    void clearCatalog() {
        products.deleteAll();
        categories.deleteAll();
        adminToken = TestJwtToken.admin(jwtEncoder);
    }

    @Test
    void createsAndListsCategories() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Plăci video" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plăci video"));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plăci video"));
    }

    @Test
    void updatesAndDeletesACategory() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Plăci video" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        URI categoryUri = URI.create(created.getResponse().getHeader("Location"));

        mockMvc.perform(put(categoryUri)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Plăci grafice" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plăci grafice"));

        mockMvc.perform(delete(categoryUri).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsCategoryNamesThatDifferOnlyByCase() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Procesoare\" }"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"PROCESOARE\" }"))
                .andExpect(status().isConflict());
    }

    @Test
    void doesNotDeleteCategoryThatStillHasProducts() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Procesoare\" }"))
                .andExpect(status().isCreated())
                .andReturn();
        URI categoryUri = URI.create(created.getResponse().getHeader("Location"));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ryzen 7",
                                  "description": "Procesor de test",
                                  "specifications": "8 nuclee",
                                  "price": 300.00,
                                  "stock": 5,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "Procesoare"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(categoryUri).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }
}
