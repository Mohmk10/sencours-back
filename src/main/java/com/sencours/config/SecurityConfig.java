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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics - Auth (register et login uniquement)
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // Swagger/OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Lecture publique des cours, sections, leçons et catégories
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sections/*/lessons/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sections/*/lessons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // Endpoints Admin
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Reviews - authentifié (tous les rôles peuvent gérer leurs avis)
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/reviews/*").authenticated()

                        // Gestion des cours - INSTRUCTEUR et ADMIN (sauf reviews déjà gérés)
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/*").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/courses/**").hasAnyRole("ADMIN", "INSTRUCTEUR")

                        // Sections et Leçons - INSTRUCTEUR et ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/sections/**").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/*/sections/**").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*/sections/**").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/sections/*/lessons/**").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/sections/*/lessons/**").hasAnyRole("ADMIN", "INSTRUCTEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/sections/*/lessons/**").hasAnyRole("ADMIN", "INSTRUCTEUR")

                        // Gestion des catégories - ADMIN uniquement
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")

                        // Gestion des utilisateurs - ADMIN uniquement
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

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
