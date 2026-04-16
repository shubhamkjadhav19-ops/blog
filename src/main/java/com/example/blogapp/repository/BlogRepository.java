package com.example.blogapp.repository;

import com.example.blogapp.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Blog> findByAuthorUsernameContainingIgnoreCase(String authorUsername, Pageable pageable);

    Page<Blog> findByTitleContainingIgnoreCaseAndAuthorUsernameContainingIgnoreCase(
            String title,
            String authorUsername,
            Pageable pageable
    );
}
