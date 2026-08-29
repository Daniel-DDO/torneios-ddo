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

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.PUT, "/jogador/avatarId").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/jogador/avatar").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/jogador/uploadfoto").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/jogador/perfil").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/jogador/me/credenciais").authenticated()
                        .requestMatchers(HttpMethod.GET, "/jogador/{id}/transacoes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/jogador/{id}/planilha").authenticated()
                        .requestMatchers(HttpMethod.POST, "/jogador/{partidaId}/analisar-problema").hasAnyAuthority("PROPRIETARIO", "DIRETOR", "ADMINISTRADOR")

                        .requestMatchers(HttpMethod.POST, "/jogador/cadastrar").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.GET, "/jogador/pin").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.POST, "/jogador/reivindicar-direto").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/gerar-pins-legado").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/status").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/cargo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/saldo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/saldo/zerar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/saldo/zerar-todos").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/saldo/distribuir").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/resetar-senha").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/resetar-pin").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/jogador/{id}/deletar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/jogador/mesclar-contas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/discord").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/jogador/{id}/email-admin").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/clube/cadastrar").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.POST, "/clube/cadastrar-lote").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/clube/{id}").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/clube/{id}/status").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/clube/{id}/valores").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/inscricao/inscrever").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.DELETE, "/inscricao/{id}").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/inscricao/substituir-jogador").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.PATCH, "/inscricao/trocar-clube").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.PUT, "/inscricao/substituir-jogador-torneio").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/inscricao/desfazer-substituicao/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/inscricao/sorteio").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.POST, "/inscricao/sorteio/confirmar").hasAnyAuthority("PROPRIETARIO", "DIRETOR")

                        .requestMatchers(HttpMethod.POST, "/participacao-fase/add").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/participacao-fase/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/participacao-fase/**").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PUT, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/temporada/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/temporada/**").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/torneio/**").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.PUT, "/torneio/**").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.PATCH, "/torneio/**").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.DELETE, "/torneio/**").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/fase-torneio/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/fases/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/fases/**").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/api/leiloes/admin/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/titulos/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/anuncios/**").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.PUT, "/anuncios/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/anuncios/**").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/notificacoes/enviar-jogador").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/notificacoes/enviar-todos").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.GET, "/api/notificacoes/admin/todas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/lidas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/antigas").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/notificacoes/geral").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.POST, "/api/noticias/gerar/**").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/partida/registrar-resultado").hasAnyAuthority("PROPRIETARIO", "DIRETOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/partida/{id}/desfazer-resultado").hasAnyAuthority("PROPRIETARIO", "DIRETOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/partida/{partidaId}/reanalisar").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.POST, "/competicao/cadastrar").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/competicao/{id}/vincular-titulo").hasAuthority("PROPRIETARIO")
                        .requestMatchers(HttpMethod.PATCH, "/competicao/{id}/status").hasAuthority("PROPRIETARIO")

                        .requestMatchers(HttpMethod.GET, "/titulos/conquistas").hasAnyAuthority("PROPRIETARIO", "DIRETOR")
                        .requestMatchers(HttpMethod.POST, "/titulos/conquistas/*/forcar-arte").hasAnyAuthority("PROPRIETARIO", "DIRETOR")

                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "https://torneios-ddo-front.onrender.com",
                "https://torneios-ddo.vercel.app"
                //"http://localhost:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
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