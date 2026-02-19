package com.example.library.swagger;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {
	
    @Bean
    OpenAPI libraryOpenAPI() {
        return new OpenAPI()
        		.info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .description("""
                                                Insira o token JWT gerado após o login:
                                                
                                                access_token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                                                """)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub Repository")
                        .url("https://github.com/erichiroshi/library-api"))
                .tags(apiTags());		
    }
    
	private Info apiInfo() {
		return new Info()
				.title("Library API")
				.description("""
                        REST API para gerenciamento de biblioteca.
                        
                        Principais recursos:
                        - Autenticação JWT
                        - Controle de acesso por roles
                        - Gestão de livros, autores e categorias
                        - Monitoramento via Actuator
                        """)
				.version("v1.0.0")
				.license(new License()
						.name("MIT License")
						.url("https://opensource.org/licenses/MIT"))
				.contact(new Contact()
						.name("Eric Hiroshi")
						.email("erichiroshi@hotmail.com")
						.url("https://www.linkedin.com/in/eric-hiroshi/"));
	}
	
	private List<Tag> apiTags() {
		return List.of(new Tag().name("Auth").description("Endpoints para autenticação e renovação de tokens JWT"),
				new Tag().name("Authors").description("Endpoints para gerenciamento de autores"),
				new Tag().name("Books").description("Endpoints para gerenciamento de livros"),
				new Tag().name("Loans").description("Endpoints para gerenciamento de categorias de livros"),
				new Tag().name("Categories").description("Endpoints para gerenciamento de empréstimos de livros"));
	}
}
