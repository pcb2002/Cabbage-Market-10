package com.example.cabbagemarket10.global.security.config;

import com.example.cabbagemarket10.domain.auth.service.CookieProperties;
import com.example.cabbagemarket10.global.common.config.properties.AppProperties;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.CsrfCookieResponseFilter;
import com.example.cabbagemarket10.global.security.SecurityErrorResponseWriter;
import com.example.cabbagemarket10.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
        com.example.cabbagemarket10.global.security.jwt.JwtProperties.class,
        CookieProperties.class
})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfCookieResponseFilter csrfCookieResponseFilter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    private final CookieProperties cookieProperties;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .csrfTokenRepository(cookieCsrfTokenRepository())
                        .requireCsrfProtectionMatcher(refreshTokenRequestMatcher()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) ->
                                securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                securityErrorResponseWriter.write(response, ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/login.html",
                                "/login-password.html",
                                "/signup.html",
                                "/item.html",
                                "/sell.html",
                                "/edit-item.html",
                                "/mypage.html",
                                "/chat.html",
                                "/favicon.ico",
                                "/app.js",
                                "/styles.css"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/popular").permitAll()
                        .requestMatchers(new RegexRequestMatcher("^/api/clients/\\d+$", "GET")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clients/{clientId}/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v2/items/search").permitAll()
                        .requestMatchers("/ws/chat","/ws/chat/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterAfter(csrfCookieResponseFilter, CsrfFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(builder -> builder
                .secure(cookieProperties.secure())
                .sameSite("Lax")
                .path("/"));
        return repository;
    }

    private RequestMatcher refreshTokenRequestMatcher() {
        return new OrRequestMatcher(
                new RegexRequestMatcher("^/api/auth/refresh$", HttpMethod.POST.name()),
                new RegexRequestMatcher("^/api/auth/logout$", HttpMethod.POST.name()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins = appProperties.cors() == null || appProperties.cors().allowedOrigins() == null
                ? List.of("http://localhost:5173")
                : appProperties.cors().allowedOrigins();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
