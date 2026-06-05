package com.xiaoan.bookstore.config;

import com.xiaoan.bookstore.interceptor.AdminJwtInterceptor;
import com.xiaoan.bookstore.interceptor.MpJwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminJwtInterceptor adminJwtInterceptor;
    private final MpJwtInterceptor mpJwtInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminJwtInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/admin/login"
                );

        registry.addInterceptor(mpJwtInterceptor)
                .addPathPatterns("/api/mp/**")
                .excludePathPatterns(
                        "/api/mp/login"
                );

        registry.addInterceptor(mpJwtInterceptor)
                .addPathPatterns("/api/file/signed-url", "/api/file/download");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
                List<MediaType> mediaTypes = new ArrayList<>();
                mediaTypes.add(new MediaType("application", "json", StandardCharsets.UTF_8));
                jacksonConverter.setSupportedMediaTypes(mediaTypes);
            }
        }
    }
}
