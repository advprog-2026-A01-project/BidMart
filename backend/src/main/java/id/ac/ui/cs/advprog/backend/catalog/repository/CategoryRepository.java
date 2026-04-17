package id.ac.ui.cs.advprog.backend.catalog.repository;

import id.ac.ui.cs.advprog.backend.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // mencari kategori utama (tanpa parent)
    List<Category> findByParentIsNull();
}