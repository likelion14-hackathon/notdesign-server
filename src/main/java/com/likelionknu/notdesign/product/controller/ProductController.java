package com.likelionknu.notdesign.product.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.product.data.dto.ProductRequestDto;
import com.likelionknu.notdesign.product.data.dto.ProductResponseDto;
import com.likelionknu.notdesign.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public GlobalResponse<Void> createProduct(@Valid @RequestBody ProductRequestDto productRequestDto) {
        return productService.createProduct(SecurityUtil.getUsername(), productRequestDto);
    }

    @GetMapping
    public GlobalResponse<ProductResponseDto> getProduct(@RequestParam Long id) {
        return productService.getProduct(id);
    }
}
