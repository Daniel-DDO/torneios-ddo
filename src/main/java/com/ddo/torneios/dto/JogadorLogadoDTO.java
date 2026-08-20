package com.ddo.torneios.dto;

import com.ddo.torneios.model.Cargo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * UserDetails enxuto usado SOMENTE pelo SecurityFilter.
 * Nunca carrega insignias/conquistas (que são EAGER em Jogador) — é isso
 * que elimina a cascata de queries em toda requisição autenticada.
 */
public class JogadorLogadoDTO implements UserDetails {

    private final String id;
    private final Cargo cargo;

    public JogadorLogadoDTO(String id, Cargo cargo) {
        this.id = id;
        this.cargo = cargo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (cargo == Cargo.PROPRIETARIO) {
            return List.of(new SimpleGrantedAuthority("PROPRIETARIO"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if (cargo == Cargo.DIRETOR) {
            return List.of(new SimpleGrantedAuthority("DIRETOR"));
        } else if (cargo == Cargo.ADMINISTRADOR) {
            return List.of(new SimpleGrantedAuthority("ADMINISTRADOR"));
        } else {
            return List.of(new SimpleGrantedAuthority("JOGADOR"));
        }
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return id; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}