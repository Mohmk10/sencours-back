package com.sencours.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sencours.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics - Auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // Swagger/OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Utilitaires publics (hash BCrypt)
                        .requestMatchers("/api/v1/utility/**").permitAll()

                        // Lecture publique des cours, sections, leçons et catégories
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sections/*/lessons/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sections/*/lessons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // Super Admin - gestion des admins et instructeurs directs
                        .requestMatchers("/api/v1/super-admin/**").hasRole("SUPER_ADMIN")

                        // Admin - permissions granulaires
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/users/*/toggle-status").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/users/*").hasRole("SUPER_ADMIN")

                        // Admin - gestion candidatures et users (ADMIN + SUPER_ADMIN)
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Candidatures instructeur - étudiants authentifiés
                        .requestMatchers(HttpMethod.POST, "/api/v1/instructor-applications").hasRole("ETUDIANT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/instructor-applications/my-application").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/instructor-applications/check").authenticated()

                        // Reviews - authentifié
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/reviews/*").authenticated()

                        // Gestion des cours - INSTRUCTEUR, ADMIN, SUPER_ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/*").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/courses/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")

                        // Sections et Leçons
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/sections/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/*/sections/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/sections/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/sections/*/lessons/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/sections/*/lessons/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/sections/*/lessons/**").hasAnyRole("INSTRUCTEUR", "ADMIN", "SUPER_ADMIN")

                        // Catégories - ADMIN + SUPER_ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Gestion des utilisateurs - ADMIN + SUPER_ADMIN
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .message("Authentification requise pour accéder à cette ressource")
                    .path(request.getRequestURI())
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
