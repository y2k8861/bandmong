package com.bandmong.config;

import com.bandmong.moderation.AccountGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccountGuard guard;

    public WebConfig(AccountGuard guard) {
        this.guard = guard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guard).addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/dev/**", "/api/time");
    }
}
