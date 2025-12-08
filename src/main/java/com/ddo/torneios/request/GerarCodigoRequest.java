package com.ddo.torneios.request;

import lombok.Data;

@Data
public class GerarCodigoRequest {
    private String adminId;
    private String jogadorId;
}