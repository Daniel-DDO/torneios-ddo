package com.ddo.torneios.service;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.exception.ClubeExisteException;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.repository.ClubeRepository;
import com.ddo.torneios.request.ClubeRequest;
import jakarta.persistence.EntityNotFoundException;
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

    public Clube atualizarClube(String id, ClubeRequest request) {
        Clube clube = clubeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clube não encontrado com ID: " + id));

        if (request.getNome() != null && !request.getNome().isBlank()) {
            if (clubeRepository.existsByNome(request.getNome())) {
                throw new ClubeExisteException(request.getNome());
            }
            clube.setNome(request.getNome());
        }

        if (request.getSigla() != null && !request.getSigla().isBlank()) {
            clube.setSigla(request.getSigla());
        }

        if (request.getEstadio() != null && !request.getEstadio().isBlank()) {
            clube.setEstadio(request.getEstadio());
        }

        if (request.getImagem() != null) {
            clube.setImagem(request.getImagem());
        }

        if (request.getLigaClube() != null) {
            clube.setLigaClube(request.getLigaClube());
        }

        if (request.getEstrelas() != null) {
            clube.setEstrelas(request.getEstrelas());
        }

        if (request.getCorPrimaria() != null && !request.getCorPrimaria().isBlank()) {
            clube.setCorPrimaria(request.getCorPrimaria());
        }

        if (request.getCorSecundaria() != null && !request.getCorSecundaria().isBlank()) {
            clube.setCorSecundaria(request.getCorSecundaria());
        }

        return clubeRepository.save(clube);
    }
}
