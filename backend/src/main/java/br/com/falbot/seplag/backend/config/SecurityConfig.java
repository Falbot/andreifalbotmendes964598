package br.com.falbot.seplag.backend.config;

import br.com.falbot.seplag.backend.seguranca.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Value("${app.seguranca.origins-permitidas}")
    private String originsPermitidas;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtServico jwtServico) throws Exception {

        Set<String> permitidas = Arrays.stream(originsPermitidas.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        var originBlock = new OriginBlockFilter(permitidas);
        var jwtFilter = new JwtAuthFilter(jwtServico);
        var rateLimit = new RateLimitFilter();

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/api/ping",
                                "/api/v1/ping",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/ws/**",
                                "/ws-teste.html",
                                "/test-capa.html"
                        ).permitAll()
                        .requestMatchers(
                                "/api/autenticacao/**",
                                "/api/v1/autenticacao/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(originBlock, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimit, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
