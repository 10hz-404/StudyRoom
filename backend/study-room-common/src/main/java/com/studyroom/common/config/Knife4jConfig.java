package com.studyroom.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("????????? API")
                .version("1.0.0")
                .description("?? Spring Boot 3 + MyBatis-Plus + JWT ?????\n\n" +
                    "**????**: Bearer Token (JWT)\n" +
                    "**????**: ?? S2024001 / ??? A001 (?? 123456)")
                .contact(new Contact().name("????").email("dev@studyroom.com")));
    }
}
