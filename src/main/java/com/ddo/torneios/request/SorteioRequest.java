package com.ddo.torneios.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SorteioRequest {
    private String temporadaId;
    private List<String> jogadoresIds;
    private List<String> clubesIds;
}