package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Entity
public class Jogador implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    @NotBlank
    private String discord;

    private String email;
    private String senha;
    private Integer finais;
    private Integer titulos;
    private Integer golsMarcados;
    private Integer golsSofridos;
    private Integer partidasJogadas;
    private Integer vitorias;
    private Integer empates;
    private Integer derrotas;

    @NotNull
    private LocalDateTime criacaoConta;

    @NotNull
    private LocalDateTime modificacaoConta;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusJogador statusJogador;

    @NotNull
    private boolean contaReivindicada;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Cargo cargo;

    @Column(columnDefinition = "TEXT")
    private String imagem;

    @Column(columnDefinition = "TEXT")
    private String descricao;
    private Integer pin;

    private LocalDateTime suspensoAte;

    private Long cartoesAmarelos;
    private Long cartoesVermelhos;

    @ColumnDefault("0.00")
    private BigDecimal saldoVirtual;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "jogador_insignias",
            joinColumns = @JoinColumn(name = "jogador_id"),
            inverseJoinColumns = @JoinColumn(name = "insignia_id")
    )
    private Set<Insignia> insignias;

    @ColumnDefault("0.00")
    private BigDecimal pontosCoeficiente;

    private String codigoReivindicacao;

    private LocalDateTime validadeCodigoReivindicacao;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "jogador_id")
    private List<Conquista> conquistas = new ArrayList<>();

    public Jogador(String nome, String discord) {
        this.nome = nome;
        this.discord = discord;
        this.finais = 0;
        this.titulos = 0;
        this.golsMarcados = 0;
        this.golsSofridos = 0;
        this.partidasJogadas = 0;
        this.criacaoConta = LocalDateTime.now();
        this.modificacaoConta = LocalDateTime.now();
        this.statusJogador = StatusJogador.ATIVO;
        this.contaReivindicada = false;
        this.cargo = Cargo.JOGADOR;
        this.cartoesAmarelos = 0L;
        this.cartoesVermelhos = 0L;
        this.pin = ThreadLocalRandom.current().nextInt(10000, 1000000);
    }

    public Jogador() {

    }

    @PrePersist
    public void prePersist() {
        if (this.pin == null) {
            this.pin = ThreadLocalRandom.current().nextInt(10000, 1000000);
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.cargo == Cargo.PROPRIETARIO) {
            return List.of(new SimpleGrantedAuthority("PROPRIETARIO"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        else if (this.cargo == Cargo.DIRETOR) {
            return List.of(new SimpleGrantedAuthority("DIRETOR"));
        }
        else if (this.cargo == Cargo.ADMINISTRADOR) {
            return List.of(new SimpleGrantedAuthority("ADMINISTRADOR"));
        }
        else {
            return List.of(new SimpleGrantedAuthority("JOGADOR"));
        }
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
}
