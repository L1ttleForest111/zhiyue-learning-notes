package com.learningnotes.controller;

import com.learningnotes.common.ApiResponse;
import com.learningnotes.dto.NameRequest;
import com.learningnotes.entity.Category;
import com.learningnotes.entity.Tag;
import com.learningnotes.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() { return ApiResponse.success(catalogService.listCategories()); }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody NameRequest request) { return ApiResponse.success(catalogService.createCategory(request)); }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) { return ApiResponse.success(catalogService.updateCategory(id, request)); }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) { catalogService.deleteCategory(id); return ApiResponse.success(); }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags() { return ApiResponse.success(catalogService.listTags()); }

    @PostMapping("/tags")
    public ApiResponse<Tag> createTag(@Valid @RequestBody NameRequest request) { return ApiResponse.success(catalogService.createTag(request)); }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) { catalogService.deleteTag(id); return ApiResponse.success(); }
}
