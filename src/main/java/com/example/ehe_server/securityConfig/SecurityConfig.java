package com.example.ehe_server.securityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Keep the JwtTokenValidatorInterface if needed by other parts of your security setup
    // private final JwtTokenValidatorInterface jwtTokenValidator;
    // public SecurityConfig(JwtTokenValidatorInterface jwtTokenValidator) {
    //     this.jwtTokenValidator = jwtTokenValidator;
    // }
    // If not needed directly here anymore, remove the constructor injection for it.

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF (common for stateless APIs)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // *** USE THIS FOR CORS ***
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll() // Public endpoints
                            .requestMatchers("/api/admin/**").hasRole("ADMIN") // Admin-only endpoints
                            .requestMatchers("/api/user/**").hasRole("USER") // User endpoints (includes admins)
                            .anyRequest().authenticated(); // All other requests require authentication
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless session management for JWT
                .httpBasic(basic -> basic.disable()) // Disable HTTP Basic auth
                .formLogin(form -> form.disable()); // Disable Form Login

        // The JwtAuthenticationFilter should be added automatically as it's a @Component
        // If not, you might need: .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // assuming jwtAuthenticationFilter is injected or available as a bean.

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allowed origin(s) - Frontend URL
        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:5173"));
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList("*")); // Allow all standard headers needed
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        // Expose headers required by the frontend (e.g., Set-Cookie for JWT)
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie"));
        // How long the results of a preflight request can be cached
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this configuration to all paths ("/**")
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // For production environments, require HTTPS
    @Bean
    @Profile("prod") // This bean will only be active in the "prod" profile
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        // Configure like the main filter chain but add HTTPS requirement
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Use the same CORS source
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/user/**").hasRole("USER")
                            .anyRequest().authenticated();
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // Add HTTPS requirement for production
                .requiresChannel(channel ->
                        channel.anyRequest().requiresSecure());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}