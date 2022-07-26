package com.cn.springflowable.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenning
 */
@Configuration
public class BeanConfig {
    @Bean
    public RestResponseFactory restResponseFactory(ObjectMapper objectMapper){
        return new RestResponseFactory(objectMapper);
    }
}
