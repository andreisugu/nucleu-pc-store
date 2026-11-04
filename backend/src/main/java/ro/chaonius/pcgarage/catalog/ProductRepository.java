package ro.chaonius.pcgarage.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByNameAsc();

    @Query("""
            select product from Product product
            where (lower(product.name) like lower(concat('%', :query, '%'))
                or lower(product.description) like lower(concat('%', :query, '%'))
                or lower(product.specifications) like lower(concat('%', :query, '%')))
                and (:categoryId is null or product.category.id = :categoryId)
            order by product.name asc
            """)
    List<Product> search(@Param("query") String query, @Param("categoryId") Long categoryId);

    boolean existsByCategory_Id(Long categoryId);
}
