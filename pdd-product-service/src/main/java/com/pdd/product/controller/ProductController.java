package com.pdd.product.controller;

import com.pdd.common.api.Result;
import com.pdd.product.dto.ProductResponse;
import com.pdd.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品查询接口。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    /**
     * 注入商品活动服务。
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 查询商品列表。
     */
    @GetMapping
    public Result<List<ProductResponse>> listProducts() {
        return Result.ok(productService.listProducts());
    }
}
