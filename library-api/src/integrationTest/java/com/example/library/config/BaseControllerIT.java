package com.example.library.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.library.security.jwt.JwtService;
import com.example.library.user.User;
import com.example.library.user.UserRepository;

/**
 * Classe base para testes de integração que precisam de JWT real.
 *
 * Use quando o Service acessa SecurityContextHolder (ex: LoanService).
 * Para controllers sem acesso ao SecurityContext (Category, Author, Book),
 * prefira @AutoConfigureMockMvc(addFilters = false) + @WithMockUser.
 */
public abstract class BaseControllerIT {

    // protected → subclasses podem usar diretamente (ex: gerar token extra)
    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User testUser;
    protected User adminUser;
    protected String userToken;
    protected String adminToken;

    protected void createUsers() {
        testUser = new User();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles(Set.of("ROLE_USER"));
        testUser = userRepository.save(testUser);
        userToken = jwtService.generateToken(testUser);

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRoles(Set.of("ROLE_ADMIN"));
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser);
    }

    /** Request autenticada como usuário comum. */
    protected MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + userToken);
    }

    /** Request autenticada como admin. */
    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + adminToken);
    }

    /**
     * Cria um usuário extra e retorna o token.
     * Útil para simular acesso não autorizado a recursos de outro usuário.
     */
    protected String tokenForNewUser(String email, String... roles) {
        User user = new User();
        user.setName("Extra User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("pass"));
        user.setRoles(roles.length > 0 ? Set.of(roles) : Set.of("ROLE_USER"));
        user = userRepository.save(user);
        return jwtService.generateToken(user);
    }
}