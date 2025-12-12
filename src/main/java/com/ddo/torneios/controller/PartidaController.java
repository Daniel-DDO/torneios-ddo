package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.service.ClassificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/partida")
public class PartidaController {

    @Autowired
    private ClassificacaoService classificacaoService;

    @Autowired
    private PartidaRepository partidaRepository;

    @PostMapping("/registrar-resultado")
    public ResponseEntity<String> registrarResultado(@RequestBody PartidaDTO dto) {
        try {
            if (dto.id() == null) {
                return ResponseEntity.badRequest().body("ID da partida é obrigatório.");
            }

            classificacaoService.registrarResultado(dto);
            return ResponseEntity.ok("Resultado registrado e coeficientes calculados com sucesso!");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erro ao registrar resultado: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno no servidor: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidaDTO> buscarPorId(@PathVariable String id) {
        return partidaRepository.findById(id)
                .map(PartidaDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}