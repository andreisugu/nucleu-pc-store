package ro.chaonius.pcgarage.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 2000)
    private String specifications;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 120)
    private String supplier;

    @Column(nullable = false, length = 80)
    private String deliveryMethod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected Product() {
    }

    Product(ProductRequest request, Category category) {
        update(request, category);
    }

    void update(ProductRequest request, Category category) {
        this.name = request.name().trim();
        this.description = request.description().trim();
        this.specifications = request.specifications().trim();
        this.price = request.price();
        this.stock = request.stock();
        this.supplier = request.supplier().trim();
        this.deliveryMethod = request.deliveryMethod().trim();
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public Category getCategory() {
        return category;
    }
}
