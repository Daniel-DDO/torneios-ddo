package com.ddo.torneios.request;

import com.ddo.torneios.model.Cargo;
import lombok.Data;

@Data
public class ReivindicarDiretoRequest {
    private String discord;
    private String novoEmail;
    private String novaSenha;
    private Cargo cargo;
}