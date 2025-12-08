package com.ddo.torneios.request;

import lombok.Data;

@Data
public class AvatarRequest {
    private String adminId;
    private String nome;
    private String url;
}