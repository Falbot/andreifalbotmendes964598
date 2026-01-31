package br.com.falbot.seplag.backend.config;

import br.com.falbot.seplag.backend.seguranca.JwtAuthFilter;
import br.com.falbot.seplag.backend.seguranca.JwtServico;
import br.com.falbot.seplag.backend.seguranca.OriginBlockFilter;
import br.com.falbot.seplag.backend.seguranca.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.origins-permitidas:*}")
    private String originsPermitidas;
    @Value("${app.cors.allow-credentials:false}")
    private boolean corsAllowCredentials;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        List<String> permitidas = Arrays.stream(originsPermitidas.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(permitidas);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin","X-Requested-With"));
        boolean allowAll = permitidas.contains("*");
        cfg.setAllowCredentials(allowAll ? false : corsAllowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtServico jwtServico,
            ObjectProvider<RateLimitFilter> rateLimitFilterProvider
    ) throws Exception {

        Set<String> permitidas = Arrays.stream(originsPermitidas.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        var originBlock = new OriginBlockFilter(permitidas);
        var jwtFilter = new JwtAuthFilter(jwtServico);

        var chain = http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) ->
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))
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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(originBlock, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // ✅ Só adiciona RateLimit se ele existir como Bean (e isso depende da property)
        RateLimitFilter rateLimitFilter = rateLimitFilterProvider.getIfAvailable();
        boolean allowAll = permitidas.contains("*");
        if (!allowAll) {
            chain = chain.addFilterBefore(originBlock, UsernamePasswordAuthenticationFilter.class);
        }
        if (rateLimitFilter != null) {
            chain = chain.addFilterAfter(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return chain.build();
    }
}
