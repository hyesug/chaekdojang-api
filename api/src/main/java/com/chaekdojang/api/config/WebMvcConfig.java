package com.chaekdojang.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.chaekdojang.api.domain.upload.StorageProperties;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(storageProperties.getLocal().getUploadDir()).toAbsolutePath().normalize();
        Path ebookDir = Paths.get(storageProperties.getLocal().getEbookDir()).toAbsolutePath().normalize();
        if (ebookDir.startsWith(uploadDir)) {
            throw new IllegalStateException("전자책 폴더는 공개 이미지 폴더 밖에 있어야 합니다.");
        }
        // 공개 파일은 프로필 이미지 폴더로 한정한다. 기존 전자책 폴더도 노출하지 않는다.
        registry.addResourceHandler(storageProperties.getLocal().getPublicPath().replaceAll("/+$", "") + "/**")
                .addResourceLocations(uploadDir.toUri().toString().replaceAll("/+$", "") + "/");
    }
}
