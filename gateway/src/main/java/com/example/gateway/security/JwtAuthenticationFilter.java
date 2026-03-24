package com.example.gateway.security;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final JwtService jwtService;

    // Rotas públicas — não passam pelo filtro JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/actuator"
    );

	@Override
	public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.path();

        // 1. Rota pública — deixa passar (next.handle continua o fluxo)
        if (isPublicPath(path)) {
            return next.handle(request);
        }

        // 2. Extração do Header (API Síncrona do ServerRequest)
        String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Propaga identidade do usuário nos headers para os serviços downstream
        String username = jwtService.extractUsername(token);
        List<String> roles = jwtService.extractRoles(token);

        log.debug("JWT validated for user={} path={}", username, path);

        // 4. Mutação da Request (No WebMVC, usamos o builder do ServerRequest)
        ServerRequest mutatedRequest = ServerRequest.from(request)
                .header("X-User-Id", username)
                .header("X-User-Roles", String.join(",", roles))
                .build();

        return next.handle(mutatedRequest);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

}