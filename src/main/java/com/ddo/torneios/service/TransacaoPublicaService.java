package com.ddo.torneios.service;

import com.ddo.torneios.dto.TransacaoPublicaDTO;
import com.ddo.torneios.repository.TransacaoPublicaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransacaoPublicaService {

    private static final int TAMANHO_MAXIMO = 50;

    private final TransacaoPublicaRepository transacaoRepository;

    public TransacaoPublicaService(TransacaoPublicaRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransacaoPublicaDTO> listarUltimas(int pagina, int tamanho) {
        int paginaSegura = Math.max(0, pagina);
        int tamanhoSeguro = Math.max(1, Math.min(tamanho, TAMANHO_MAXIMO));
        return transacaoRepository.buscarUltimasTransacoes(PageRequest.of(paginaSegura, tamanhoSeguro));
    }
}
