package com.echill.controller;

import com.echill.dto.request.BlogRequest;
import com.echill.dto.response.ApiResponse;
import com.echill.dto.response.BlogResponse;
import com.echill.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BlogResponse> createBlog(
            @Valid @ModelAttribute BlogRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return ApiResponse.<BlogResponse>builder()
                .data(blogService.createBlog(request, file))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BlogResponse> updateBlog(
            @PathVariable Long id,
            @Valid @ModelAttribute BlogRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return ApiResponse.<BlogResponse>builder()
                .data(blogService.updateBlog(id, request, file))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ApiResponse.<Void>builder()
                .message("Blog deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BlogResponse> getBlogById(@PathVariable Long id) {
        return ApiResponse.<BlogResponse>builder()
                .data(blogService.getBlogById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<BlogResponse>> getAllBlogs() {
        return ApiResponse.<List<BlogResponse>>builder()
                .data(blogService.getMyBlogs())
                .build();
    }
}
