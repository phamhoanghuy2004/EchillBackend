package com.echill.service;

import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.BlogRequest;
import com.echill.dto.response.BlogResponse;
import com.echill.entity.Blog;
import com.echill.entity.User;
import com.echill.exception.AppException;
import com.echill.exception.ErrorEnum;
import com.echill.exception.TeacherErrorEnum;
import com.echill.mapper.BlogMapper;
import com.echill.repository.BlogRepository;
import com.echill.repository.UserRepository;
import com.echill.service.persistence.BlogPersistenceService;
import com.echill.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogService {

    BlogRepository blogRepository;
    UserRepository userRepository;
    CloudinaryService cloudinaryService;
    BlogPersistenceService blogPersistenceService;
    BlogMapper blogMapper;

    public BlogResponse createBlog(BlogRequest request, MultipartFile file) {
        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorEnum.USER_NOTFOUND));

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(file, CloudinaryFolder.BLOG_IMAGE);
        }

        return blogMapper.toResponse(blogPersistenceService.saveBlog(request, user, imageUrl));
    }

    public BlogResponse updateBlog(Long id, BlogRequest request, MultipartFile file) {
        Blog blog = blogRepository.findByIdWithUser(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));

        SecurityUtils.validateOwnership(blog.getUser().getId());

        String oldImageUrl = blog.getImageUrl();
        String newImageUrl = null;

        if (file != null && !file.isEmpty()) {
            newImageUrl = cloudinaryService.uploadImage(file, CloudinaryFolder.BLOG_IMAGE);
        }

        blog = blogPersistenceService.updateBlog(request, blog, newImageUrl);

        if (newImageUrl != null && oldImageUrl != null) {
            cloudinaryService.deleteImage(oldImageUrl);
        }

        return blogMapper.toResponse(blog);
    }

    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findByIdWithUser(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));

        SecurityUtils.validateOwnership(blog.getUser().getId());

        String deleteImageUrl = blog.getImageUrl();

        blogPersistenceService.deleteBlog(blog);

        cloudinaryService.deleteImage(deleteImageUrl);
    }

    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findByIdWithUser(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));
        return blogMapper.toResponse(blog);
    }

    public List<BlogResponse> getMyBlogs() {
        List<Blog> blogs = blogRepository.findAllWithUserByUserId(SecurityUtils.getCurrentUserId());
        return  blogs.stream().map(blogMapper::toResponse).toList();
    }

}
