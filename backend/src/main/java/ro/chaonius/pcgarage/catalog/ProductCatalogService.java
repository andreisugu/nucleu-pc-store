package ro.chaonius.pcgarage.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
class ProductCatalogService {

    private final ProductRepository products;
    private final CategoryService categories;

    ProductCatalogService(ProductRepository products, CategoryService categories) {
        this.products = products;
        this.categories = categories;
    }

    ProductResponse create(ProductRequest request) {
        Category category = categories.findByName(request.category());
        return ProductResponse.from(products.save(new Product(request, category)));
    }

    ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        Category category = categories.findByName(request.category());
        product.update(request, category);
        return ProductResponse.from(products.save(product));
    }

    void delete(Long id) {
        products.delete(findProduct(id));
    }

    @Transactional(readOnly = true)
    List<ProductResponse> list(String query, Long categoryId) {
        // Un termen vid produce LIKE '%%', deci întoarce tot catalogul. Un parametru null ar fi
        // tipat de PostgreSQL ca bytea, iar lower(bytea) nu există.
        String normalizedQuery = query == null ? "" : query.trim();
        return products.search(normalizedQuery, categoryId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    private Product findProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produsul nu există"));
    }
}
