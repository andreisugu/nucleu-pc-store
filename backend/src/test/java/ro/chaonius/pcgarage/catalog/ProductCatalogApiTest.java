package ro.chaonius.pcgarage.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCatalogApiTest {

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
        categories.saveAll(List.of(new Category("Procesoare"), new Category("Plăci video")));
        adminToken = TestJwtToken.admin(jwtEncoder);
    }

    @Test
    void rejectsProductWithNegativePrice() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Procesor de test",
                                  "description": "Produs pentru validarea catalogului",
                                  "specifications": "8 nuclee",
                                  "price": -1.00,
                                  "stock": 4,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "Procesoare"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsProductWithNegativeStock() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Procesor de test",
                                  "description": "Produs pentru validarea catalogului",
                                  "specifications": "8 nuclee",
                                  "price": 100.00,
                                  "stock": -1,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "Procesoare"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsProductWhenStockIsMissing() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Procesor de test",
                                  "description": "Produs pentru validarea catalogului",
                                  "specifications": "8 nuclee",
                                  "price": 100.00,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "Procesoare"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsAndListsAValidProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "GeForce RTX 5080",
                                  "description": "Placă video pentru gaming",
                                  "specifications": "16 GB GDDR7",
                                  "price": 5999.99,
                                  "stock": 3,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "Plăci video"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("GeForce RTX 5080"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("GeForce RTX 5080"))
                .andExpect(jsonPath("$[0].price").value(5999.99));
    }

    @Test
    void searchesAcrossProductNamesAndSpecificationsIgnoringCase() throws Exception {
        createProduct("Ryzen 7 9800X3D", "Procesor pentru gaming", "8 nuclee", "Procesoare");
        createProduct("Memorie DDR5", "Kit de memorie", "32 GB DDR5", "Plăci video");

        mockMvc.perform(get("/api/products").param("q", "RYZEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ryzen 7 9800X3D"))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/products").param("q", "32 gb ddr5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Memorie DDR5"));

        Long processorCategoryId = categories.findByNameIgnoreCase("Procesoare").orElseThrow().getId();
        mockMvc.perform(get("/api/products").param("categoryId", processorCategoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ryzen 7 9800X3D"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void productCanBeUpdatedAndDeletedByItsResourceUrl() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ryzen 7",
                                  "description": "Procesor inițial",
                                  "specifications": "8 nuclee",
                                  "price": 1200.00,
                                  "stock": 4,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "Procesoare"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        URI productUri = URI.create(created.getResponse().getHeader("Location"));

        mockMvc.perform(put(productUri)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ryzen 7 9800X3D",
                                  "description": "Procesor actualizat",
                                  "specifications": "8 nuclee, 3D V-Cache",
                                  "price": 2199.99,
                                  "stock": 2,
                                  "supplier": "AMD",
                                  "deliveryMethod": "Curier rapid",
                                  "category": "Plăci video"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ryzen 7 9800X3D"))
                .andExpect(jsonPath("$.price").value(2199.99))
                .andExpect(jsonPath("$.stock").value(2))
                .andExpect(jsonPath("$.category").value("Plăci video"));

        mockMvc.perform(get(productUri))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ryzen 7 9800X3D"));

        mockMvc.perform(delete(productUri).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private void createProduct(String name, String description, String specifications, String category)
            throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "%s",
                                  "specifications": "%s",
                                  "price": 100.00,
                                  "stock": 3,
                                  "supplier": "Demo Parts",
                                  "deliveryMethod": "Curier",
                                  "category": "%s"
                                }
                                """.formatted(name, description, specifications, category)))
                .andExpect(status().isCreated());
    }
}
