package ro.chaonius.pcgarage.catalog;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String specifications,
        BigDecimal price,
        int stock,
        String supplier,
        String deliveryMethod,
        String category) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSpecifications(),
                product.getPrice(),
                product.getStock(),
                product.getSupplier(),
                product.getDeliveryMethod(),
                product.getCategory().getName());
    }
}
