package com.ddo.torneios.service;

import com.ddo.torneios.dto.MediasGlobaisEstiloDTO;
import com.ddo.torneios.repository.JogadorClubeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class EstiloGlobalCache {

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;

    @Cacheable("mediasGlobaisEstilo")
    public double[] obterMediasGlobais() {
        MediasGlobaisEstiloDTO r = jogadorClubeRepository.buscarMediasGlobaisEstilo();
        if (r == null) return new double[]{0.0, 0.0, 0.0};
        return new double[]{ nz(r.mediaGolsMarcadosGlobal()), nz(r.mediaGolsSofridosGlobal()), nz(r.mediaEstrelasGlobal()) };
    }

    private double nz(Double v) { return v == null ? 0.0 : v; }
}
