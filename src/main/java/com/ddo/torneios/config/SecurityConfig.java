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
                        .requestMatchers(HttpMethod.POST, "/clube/cadastrar").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/clube/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/fases/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/fases/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/leiloes/admin/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/saldo").authenticated()
                        .requestMatchers(HttpMethod.POST, "/titulos/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/anuncios/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/anuncios/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/anuncios/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/notificacoes/enviar-jogador").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/notificacoes/enviar-todos").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.GET, "/api/notificacoes/admin/todas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/lidas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/antigas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/geral").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/perfil").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/jogador/me/credenciais").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/status").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/cargo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.GET, "/jogador/pin").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/reivindicar-direto").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/gerar-pins-legado").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.GET, "/jogador/{id}/transacoes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/torneio/criar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/temporada/criar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/temporada/{id}/encerrar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/noticias/gerar/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/inscricao/inscrever").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/inscricao/substituir-jogador").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/inscricao/trocar-clube").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/inscricao/substituir-jogador-torneio").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/inscricao/desfazer-substituicao/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/inscricao/sorteio").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/inscricao/sorteio/confirmar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/titulos/lote").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/titulos/conceder").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/titulos/conceder-legado").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/titulos/conceder-coletivo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/partida/registrar-resultado").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/partida/{id}/desfazer-resultado").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/partida/{partidaId}/analisar-problema").authenticated()
                        .requestMatchers(HttpMethod.POST, "/partida/{partidaId}/reanalisar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/status").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/cargo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/cadastrar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/competicao/cadastrar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/competicao/{id}/vincular-titulo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/clube/cadastrar").hasAuthority("PROPRIETARIO")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT", "PATCH"));
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