package com.ddo.torneios.service;

import com.ddo.torneios.model.Jogador;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    @Value("${senha.token.login}")
    private String secretString;

    public String gerarToken(Jogador jogador) {
        Instant expirationTime = Instant.now().plus(7, ChronoUnit.DAYS);

        SecretKey key = getKey();

        return Jwts.builder()
                .setSubject(jogador.getId())
                .claim("discord", jogador.getDiscord())
                .claim("cargo", jogador.getCargo().name())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validarTokenEObterId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }
}