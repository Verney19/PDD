package com.pdd.product.controller;

import com.pdd.common.api.Result;
import com.pdd.product.dto.CreateProductRequest;
import com.pdd.product.dto.ProductResponse;
import com.pdd.product.dto.UpdateProductRequest;
import com.pdd.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品查询与管理接口。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    /**
     * 注入商品服务。
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 查询商品列表，可按分类过滤。
     */
    @GetMapping
    public Result<List<ProductResponse>> listProducts(
            @RequestParam(required = false) String category) {
        return Result.ok(productService.listProducts(category));
    }

    /**
     * 查询单个商品详情。
     */
    @GetMapping("/{id}")
    public Result<ProductResponse> getProduct(@PathVariable Long id) {
        return Result.ok(productService.getProduct(id));
    }

    /**
     * 创建商品（仅管理员）。
     */
    @PostMapping
    public Result<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.createProduct(request));
    }

    /**
     * 更新商品（仅管理员）。
     */
    @PutMapping("/{id}")
    public Result<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return Result.ok(productService.updateProduct(id, request));
    }

    /**
     * 删除商品（仅管理员）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.ok();
    }
}
