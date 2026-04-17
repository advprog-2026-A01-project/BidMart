package id.ac.ui.cs.advprog.backend.catalog.controller;

import id.ac.ui.cs.advprog.backend.catalog.model.Category;
import id.ac.ui.cs.advprog.backend.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> response = categoryService.getAllCategories().stream()
                .map(cat -> new CategoryResponse(
                        cat.getId(),
                        cat.getName(),
                        cat.getParent() != null ? cat.getParent().getId() : null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // DTO sederhana untuk response API
    @lombok.Value
    public static class CategoryResponse {
        Long id;
        String name;
        Long parentId;
    }
}