package com.befapress.repository;

import com.befapress.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByStatusOrderByDisplayOrderAsc(String status);

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
