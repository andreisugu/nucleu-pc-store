package ro.chaonius.pcgarage.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    protected Category() {
    }

    Category(String name) {
        this.name = name.trim();
    }

    void rename(String name) {
        this.name = name.trim();
    }

    Long getId() {
        return id;
    }

    String getName() {
        return name;
    }
}
