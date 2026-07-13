package com.likelionknu.notdesign.product.data.repository;

import com.likelionknu.notdesign.product.data.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
