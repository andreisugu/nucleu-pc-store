package ro.chaonius.pcgarage.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Produse", description = "Catalogul public și administrarea produselor")
class ProductController {

    private final ProductCatalogService catalog;

    ProductController(ProductCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "Caută și filtrează produsele din catalog")
    List<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId) {
        return catalog.list(q, categoryId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Afișează un produs după identificator")
    ProductResponse get(@PathVariable Long id) {
        return catalog.get(id);
    }

    @PostMapping
    @Operation(summary = "Adaugă un produs", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = catalog.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editează un produs", security = @SecurityRequirement(name = "bearerAuth"))
    ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return catalog.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Șterge un produs", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        catalog.delete(id);
    }
}
