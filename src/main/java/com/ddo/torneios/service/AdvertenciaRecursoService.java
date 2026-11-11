package com.ddo.torneios.service;

import com.ddo.torneios.dto.AdvertenciaRecursoDTO;
import com.ddo.torneios.model.StatusAdvertencia;
import com.ddo.torneios.repository.AdvertenciaRecursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdvertenciaRecursoService {

    private final AdvertenciaRecursoRepository recursoRepository;

    public AdvertenciaRecursoService(AdvertenciaRecursoRepository recursoRepository) {
        this.recursoRepository = recursoRepository;
    }

    @Transactional(readOnly = true)
    public List<AdvertenciaRecursoDTO> listarPendentes() {
        return recursoRepository.buscarRecursosPendentes(StatusAdvertencia.CANCELADA);
    }
}
