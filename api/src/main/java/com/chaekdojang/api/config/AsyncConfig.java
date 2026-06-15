package com.chaekdojang.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // @Async 사용 활성화 (접속 로그 비동기 DB 저장용)
}
