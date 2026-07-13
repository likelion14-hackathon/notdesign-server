package com.likelionknu.notdesign.product.service;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.product.data.dto.ProductRequestDto;
import com.likelionknu.notdesign.product.data.dto.ProductResponseDto;
import com.likelionknu.notdesign.product.data.entity.Product;
import com.likelionknu.notdesign.product.data.exception.ProductNotFoundException;
import com.likelionknu.notdesign.product.data.repository.ProductRepository;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final AuthService authService;

    private Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);
    }

    @Transactional
    public GlobalResponse<Void> createProduct(String email, ProductRequestDto productRequestDto) {
        User user = authService.getUserEntity(email);

        productRepository.save(
                Product.builder()
                        .name(productRequestDto.getName())
                        .description(productRequestDto.getDescription())
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return GlobalResponse.ok();
    }

    @Transactional(readOnly = true)
    public GlobalResponse<ProductResponseDto> getProduct(Long id) {
        Product product = getProductEntity(id);

        return GlobalResponse.ok(
                ProductResponseDto.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .createdAt(product.getCreatedAt())
                        .build()
        );
    }
}
