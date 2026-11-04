package ro.chaonius.pcgarage.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    CategoryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    CategoryResponse create(CategoryRequest request) {
        ensureNameAvailable(request.name(), null);
        return CategoryResponse.from(categories.save(new Category(request.name())));
    }

    @Transactional(readOnly = true)
    List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    CategoryResponse rename(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        ensureNameAvailable(request.name(), id);
        category.rename(request.name());
        return CategoryResponse.from(categories.save(category));
    }

    void delete(Long id) {
        Category category = findCategory(id);
        if (products.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Categoria conține produse și nu poate fi ștearsă");
        }
        categories.delete(category);
    }

    Category findByName(String name) {
        return categories.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Categoria indicată nu există"));
    }

    private Category findCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nu există"));
    }

    private void ensureNameAvailable(String name, Long currentId) {
        boolean duplicate = currentId == null
                ? categories.existsByNameIgnoreCase(name.trim())
                : categories.existsByNameIgnoreCaseAndIdNot(name.trim(), currentId);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Numele categoriei este deja folosit");
        }
    }
}
