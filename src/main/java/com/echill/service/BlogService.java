package com.echill.service;

import com.echill.constant.CacheNames;
import com.echill.constant.CloudinaryFolder;
import com.echill.dto.request.BlogRequest;
import com.echill.dto.response.BlogResponse;
import com.echill.dto.response.PageResponse;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
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
        String imagePublicId = null;
        if (file != null && !file.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolder.BLOG_IMAGE);
            imageUrl = uploadResult.get("url");
            imagePublicId = uploadResult.get("publicId");
        }

        return blogMapper.toResponse(blogPersistenceService.saveBlog(request, user, imageUrl, imagePublicId));
    }

    public BlogResponse updateBlog(Long id, BlogRequest request, MultipartFile file) {
        Blog blog = blogRepository.findByIdWithUser(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));

        SecurityUtils.validateOwnership(blog.getUser().getId());

        String newImageUrl = null;
        String newImagePublicId = null;

        if (file != null && !file.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadImage(file, CloudinaryFolder.BLOG_IMAGE);
            newImageUrl = uploadResult.get("url");
            newImagePublicId = uploadResult.get("publicId");
        }

        blog = blogPersistenceService.updateBlog(request, blog, newImageUrl, newImagePublicId);

        return blogMapper.toResponse(blog);
    }

    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findByIdWithUser(id)
                .orElseThrow(() -> new AppException(TeacherErrorEnum.BLOG_NOT_FOUND));

        SecurityUtils.validateOwnership(blog.getUser().getId());

        blogPersistenceService.deleteBlog(blog);

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

    public PageResponse<BlogResponse> getAllBlogs(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        org.springframework.data.domain.Page<Blog> blogPage = blogRepository.findAllWithUser(pageRequest);

        return PageResponse.of(blogPage.map(blogMapper::toResponse));
    }

    @Cacheable(cacheNames = CacheNames.LATEST_BLOGS, sync = true)
    public List<BlogResponse> getLatestBlogs() {
        log.info("⚡ CHẠY VÀO DB ĐỂ LẤY 3 BLOG MỚI NHẤT (CACHE MISS)");
        List<Blog> blogs = blogRepository.findLatestBlogs(PageRequest.of(0, 3));
        return blogs.stream().map(blogMapper::toResponse).collect(Collectors.toList());
    }
}
