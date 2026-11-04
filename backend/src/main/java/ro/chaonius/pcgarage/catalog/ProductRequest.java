package ro.chaonius.pcgarage.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank @Size(max = 2000) String specifications,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotBlank @Size(max = 120) String supplier,
        @NotBlank @Size(max = 80) String deliveryMethod,
        @NotBlank @Size(max = 100) String category) {
}
