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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categorii", description = "Categorii pentru organizarea produselor")
class CategoryController {

    private final CategoryService categories;

    CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    @Operation(summary = "Listează categoriile")
    List<CategoryResponse> list() {
        return categories.list();
    }

    @PostMapping
    @Operation(summary = "Adaugă o categorie", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categories.create(request);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Redenumește o categorie", security = @SecurityRequirement(name = "bearerAuth"))
    CategoryResponse rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categories.rename(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Șterge o categorie fără produse", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        categories.delete(id);
    }
}
