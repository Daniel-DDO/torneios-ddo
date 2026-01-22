package com.ddo.torneios.service;

import com.ddo.torneios.dto.AnuncioDTO;
import com.ddo.torneios.model.Anuncio;
import com.ddo.torneios.repository.AnuncioRepository;
import com.ddo.torneios.request.AnuncioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;

    @Transactional
    public AnuncioDTO criarAnuncio(AnuncioRequest request) {
        Anuncio anuncio = requestToEntity(request);

        if (anuncio.getDataPostagem() == null) {
            anuncio.setDataPostagem(LocalDateTime.now());
        }

        anuncioRepository.save(anuncio);
        return entityToResponse(anuncio);
    }

    public AnuncioDTO buscarPorId(String id) {
        Anuncio anuncio = anuncioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado"));
        return entityToResponse(anuncio);
    }

    public List<AnuncioDTO> listarUltimos10() {
        return anuncioRepository.findTop10ByOrderByDataPostagemDesc()
                .stream()
                .map(this::entityToResponse)
                .toList();
    }

    public AnuncioDTO buscarMaisRecente() {
        Anuncio anuncio = anuncioRepository.findTopByOrderByDataPostagemDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum anúncio postado ainda"));
        return entityToResponse(anuncio);
    }

    public List<AnuncioDTO> buscarPorTitulo(String termo) {
        return anuncioRepository.findByTituloContainingIgnoreCase(termo)
                .stream()
                .map(this::entityToResponse)
                .toList();
    }

    @Transactional
    public AnuncioDTO atualizarAnuncio(String id, AnuncioRequest request) {
        Anuncio anuncio = anuncioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado"));

        anuncio.setTitulo(request.getTitulo());
        anuncio.setMensagem(request.getMensagem());
        anuncio.setTipoMensagem(request.getTipoMensagem());
        anuncio.setImagem(request.getImagem());
        anuncio.setCorMensagem(request.getCorMensagem());

        anuncioRepository.save(anuncio);
        return entityToResponse(anuncio);
    }

    @Transactional
    public void excluirAnuncio(String id) {
        if (!anuncioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado");
        }
        anuncioRepository.deleteById(id);
    }

    private Anuncio requestToEntity(AnuncioRequest request) {
        Anuncio anuncio = new Anuncio();
        anuncio.setTitulo(request.getTitulo());
        anuncio.setMensagem(request.getMensagem());
        anuncio.setTipoMensagem(request.getTipoMensagem());
        anuncio.setImagem(request.getImagem());
        anuncio.setDataPostagem(request.getDataPostagem());
        anuncio.setCorMensagem(request.getCorMensagem());
        return anuncio;
    }

    private AnuncioDTO entityToResponse(Anuncio anuncio) {
        return new AnuncioDTO(
                anuncio.getId(),
                anuncio.getTitulo(),
                anuncio.getMensagem(),
                anuncio.getDataPostagem()
        );
    }
}