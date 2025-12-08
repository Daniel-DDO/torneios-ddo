package com.ddo.torneios.service;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.exception.ClubeExisteException;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.repository.ClubeRepository;
import com.ddo.torneios.request.ClubeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ClubeService {

    @Autowired
    private ClubeRepository clubeRepository;

    public void cadastrarClube(ClubeRequest request) {
        if (clubeRepository.existsBySigla(request.getSigla()) &&
        clubeRepository.existsByNome(request.getNome())) {
            throw new ClubeExisteException(request.getSigla()+" - "+request.getNome());
        }

        Clube clube = new Clube(request.getNome(), request.getEstadio(), request.getImagem(),
                request.getLigaClube(), request.getSigla(), request.getCorPrimaria(), request.getCorSecundaria(), request.getEstrelas());

        clubeRepository.save(clube);
    }

    public PaginacaoDTO<Clube> listarClubes(
            String nomeFiltro,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Clube> paginaEntidades;

        if (nomeFiltro != null && !nomeFiltro.isBlank()) {
            paginaEntidades = clubeRepository.findByNomeContainingIgnoreCase(nomeFiltro, pageable);
        } else {
            paginaEntidades = clubeRepository.findAll(pageable);
        }

        return new PaginacaoDTO<>(
                paginaEntidades.getContent(),
                paginaEntidades.getNumber(),
                paginaEntidades.getTotalPages(),
                paginaEntidades.getTotalElements(),
                paginaEntidades.getSize(),
                paginaEntidades.isLast()
        );
    }

    public ResponseEntity<Clube> retornarClube(String id) {
        return clubeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
