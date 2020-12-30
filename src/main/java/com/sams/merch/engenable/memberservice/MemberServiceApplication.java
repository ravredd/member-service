package com.sams.merch.engenable.memberservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import lombok.Generated;

@SpringBootApplication(exclude = { ErrorMvcAutoConfiguration.class })
public class MemberServiceApplication {

    @Generated
    public static void main(String[] args) {
        SpringApplication.run(MemberServiceApplication.class);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
