package com.echill.service;

import com.echill.dto.request.BlogRequest;
import com.echill.dto.response.BlogResponse;
import com.echill.entity.Blog;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.repository.BlogRepository;
import com.echill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public BlogResponse createBlog(BlogRequest request, MultipartFile file) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadImage(file);
            } catch (IOException e) {
                log.error("Failed to upload image to Cloudinary", e);
                throw new AppException(TeacherErrorEnum.UPLOAD_IMAGE_FAILED);
            }
        }

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(imageUrl)
                .user(user)
                .build();

        blog = blogRepository.save(blog);
        return mapToResponse(blog);
    }

    public BlogResponse updateBlog(Long id, BlogRequest request, MultipartFile file) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));

        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());

        // Nếu có upload file mới thì cập nhật imageUrl, nếu không thì giữ nguyên
        if (file != null && !file.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(file);
                blog.setImageUrl(imageUrl);
            } catch (IOException e) {
                log.error("Failed to upload image to Cloudinary", e);
                throw new AppException(TeacherErrorEnum.BLOG_NOT_FOUND);
            }
        }

        blog = blogRepository.save(blog);
        return mapToResponse(blog);
    }

    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));
        blogRepository.delete(blog);
    }

    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));
        return mapToResponse(blog);
    }

    public List<BlogResponse> getAllBlogs() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return blogRepository.findByUsername(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BlogResponse mapToResponse(Blog blog) {
        String excerpt = blog.getContent() != null && blog.getContent().length() > 100 
                ? blog.getContent().substring(0, 100) + "..." 
                : blog.getContent();
                
        return BlogResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .content(blog.getContent())
                .imageUrl(blog.getImageUrl())
                .excerpt(excerpt)
                .authorName(blog.getUser() != null ? blog.getUser().getFullName() : "Unknown")
                .createdAt(
                        blog.getCreatedAt() != null
                                ? blog.getCreatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                : null
                )
                .build();
    }
}
