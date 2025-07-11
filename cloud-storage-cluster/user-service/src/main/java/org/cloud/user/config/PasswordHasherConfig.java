package org.cloud.user.config;

import org.cloud.user.util.secure.impl.BcryptPasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordHasherConfig {

    @Bean
    public BcryptPasswordHasher getBcryptPasswordHasher() {
        return new BcryptPasswordHasher();
    }
}
