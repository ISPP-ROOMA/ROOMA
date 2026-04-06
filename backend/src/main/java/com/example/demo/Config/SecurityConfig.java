package com.example.demo.Config;

import com.example.demo.Jwt.JwtAccessDeniedHandler;
import com.example.demo.Jwt.JwtAuthenticationEntryPoint;
import com.example.demo.Jwt.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsConfig corsConfig;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, JwtAccessDeniedHandler jwtAccessDeniedHandler, CorsConfig corsConfig) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.corsConfig = corsConfig;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfig))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/users/profile").authenticated()
                        .requestMatchers("/api/bills/me/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bills/apartment/**").hasAnyRole("LANDLORD","ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/bills/apartment/**").hasAnyRole("LANDLORD","ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bills/debts/{debtId}/pay").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/apartments/*/incidents/**").hasAnyRole("TENANT", "LANDLORD", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/apartments/*/incidents").hasRole("TENANT")
                        .requestMatchers(HttpMethod.PATCH, "/api/apartments/*/incidents/**").hasAnyRole("TENANT", "LANDLORD", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/apartments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/apartments/**").hasRole("LANDLORD")
                        .requestMatchers(HttpMethod.PUT, "/api/apartments/**").hasRole("LANDLORD")
                        .requestMatchers(HttpMethod.DELETE, "/api/apartments/**").hasRole("LANDLORD")
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}



