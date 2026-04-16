package com.example.blogapp.service.impl;

import com.example.blogapp.dto.BlogRequest;
import com.example.blogapp.dto.BlogResponse;
import com.example.blogapp.dto.PagedResponse;
import com.example.blogapp.entity.Blog;
import com.example.blogapp.entity.User;
import com.example.blogapp.exception.ResourceNotFoundException;
import com.example.blogapp.repository.BlogRepository;
import com.example.blogapp.repository.UserRepository;
import com.example.blogapp.service.BlogService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BlogServiceImpl implements BlogService {
    private static final Path BLOG_IMAGE_DIR = Paths.get("uploads", "blog-images");

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    public BlogServiceImpl(BlogRepository blogRepository, UserRepository userRepository) {
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BlogResponse createBlog(BlogRequest request, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setCreatedAt(LocalDateTime.now());
        blog.setAuthor(author);
        blog.setImagePath(storeImageIfPresent(request.getImage()));

        Blog saved = blogRepository.save(blog);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BlogResponse> getAllBlogs(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String title,
            String authorUsername
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        boolean hasTitle = title != null && !title.isBlank();
        boolean hasAuthor = authorUsername != null && !authorUsername.isBlank();

        Page<Blog> blogPage;
        if (hasTitle && hasAuthor) {
            blogPage = blogRepository.findByTitleContainingIgnoreCaseAndAuthorUsernameContainingIgnoreCase(
                    title,
                    authorUsername,
                    pageable
            );
        } else if (hasTitle) {
            blogPage = blogRepository.findByTitleContainingIgnoreCase(title, pageable);
        } else if (hasAuthor) {
            blogPage = blogRepository.findByAuthorUsernameContainingIgnoreCase(authorUsername, pageable);
        } else {
            blogPage = blogRepository.findAll(pageable);
        }

        List<BlogResponse> content = blogPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PagedResponse<BlogResponse> response = new PagedResponse<>();
        response.setContent(content);
        response.setPage(blogPage.getNumber());
        response.setSize(blogPage.getSize());
        response.setTotalElements(blogPage.getTotalElements());
        response.setTotalPages(blogPage.getTotalPages());
        response.setLast(blogPage.isLast());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with id: " + id));
        return mapToResponse(blog);
    }

    @Override
    @Transactional
    public void deleteBlog(Long id, String currentUsername) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found with id: " + id));

        if (!blog.getAuthor().getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You can delete only your own blogs");
        }

        blogRepository.delete(blog);
    }

    private BlogResponse mapToResponse(Blog blog) {
        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTitle(blog.getTitle());
        response.setContent(blog.getContent());
        response.setImageUrl(blog.getImagePath() == null ? null : "/api/blogs/images/" + blog.getImagePath());
        response.setCreatedAt(blog.getCreatedAt());
        response.setAuthorUsername(blog.getAuthor().getUsername());
        return response;
    }

    private String storeImageIfPresent(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        try {
            Files.createDirectories(BLOG_IMAGE_DIR);
            String originalName = image.getOriginalFilename() == null ? "image.jpg" : image.getOriginalFilename();
            String extension = "";
            int extensionIndex = originalName.lastIndexOf('.');
            if (extensionIndex >= 0) {
                extension = originalName.substring(extensionIndex);
            }
            String fileName = UUID.randomUUID() + extension;
            Path destination = BLOG_IMAGE_DIR.resolve(fileName);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to store blog image");
        }
    }
}
