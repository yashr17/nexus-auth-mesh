package com.nexus.idp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.nexus.idp.config.RsaKeyProperties;

@SpringBootApplication(scanBasePackages = "com.nexus")
@EnableConfigurationProperties(RsaKeyProperties.class)
public class IdpApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdpApplication.class, args);
    }
}
