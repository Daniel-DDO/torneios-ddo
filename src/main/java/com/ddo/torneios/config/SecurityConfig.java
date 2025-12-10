package com.ddo.torneios.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.PUT, "/jogador/avatarId").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/jogador/avatar").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/jogador/atualizarConta").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/jogador/alterarSenha").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/jogador/uploadfoto").authenticated()
                        .requestMatchers(HttpMethod.POST, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/inscricao/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/inscricao/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/participacao-fase/**").hasAuthority("PROPRIETARIO")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}