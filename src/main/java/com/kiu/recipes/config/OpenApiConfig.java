package com.kiu.recipes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI recipeApi() {
        return new OpenAPI().info(new Info()
                .title("Recipe Sharing API")
                .version("1.0")
                .description("Chefs publish recipes, organised by category and tags. "
                        + "Supports validation, server-side pagination/sorting and recipe photo upload."));
    }
}
