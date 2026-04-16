package com.example.blogapp.service;

import com.example.blogapp.dto.BlogRequest;
import com.example.blogapp.dto.BlogResponse;
import com.example.blogapp.dto.PagedResponse;

public interface BlogService {

    BlogResponse createBlog(BlogRequest request, String username);

    PagedResponse<BlogResponse> getAllBlogs(int page, int size, String sortBy, String sortDir, String title, String authorUsername);

    BlogResponse getBlogById(Long id);

    void deleteBlog(Long id, String currentUsername);
}
