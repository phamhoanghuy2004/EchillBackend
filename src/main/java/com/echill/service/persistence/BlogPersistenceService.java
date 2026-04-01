package com.echill.service.persistence;

import com.echill.dto.request.BlogRequest;
import com.echill.entity.Blog;
import com.echill.entity.User;
import com.echill.repository.BlogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogPersistenceService {

    BlogRepository blogRepository;

    @Transactional
    public Blog saveBlog(BlogRequest request, User user, String imageUrl, String imagePublicId) {
        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(imageUrl)
                .imagePublicId(imagePublicId)
                .user(user)
                .build();

        return blogRepository.save(blog);
    }

    @Transactional
    public Blog updateBlog(BlogRequest request, Blog blog, String newImageUrl, String newImagePublicId) {
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        if (newImageUrl != null) {
            blog.setImageUrl(newImageUrl);
            blog.setImagePublicId(newImagePublicId);
        }
        return blogRepository.save(blog);
    }

    @Transactional
    public void deleteBlog(Blog blog) {
        blogRepository.delete(blog);
    }
}