package com.ddo.torneios.request;

import java.util.List;
import lombok.Data;

@Data
public class GerarCopaRealRequest {
    private String faseId;
    private List<String> idsElite;
    private List<String> idsIntermediarios;
    private List<String> idsResto;
}