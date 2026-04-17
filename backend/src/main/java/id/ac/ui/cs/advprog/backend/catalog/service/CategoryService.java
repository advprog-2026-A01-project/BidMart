package id.ac.ui.cs.advprog.backend.catalog.service;

import id.ac.ui.cs.advprog.backend.catalog.model.Category;
import id.ac.ui.cs.advprog.backend.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> getAllRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}