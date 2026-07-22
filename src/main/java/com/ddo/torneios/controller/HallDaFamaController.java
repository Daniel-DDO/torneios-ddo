package com.ddo.torneios.controller;

import com.ddo.torneios.dto.HallDaFamaDTO;
import com.ddo.torneios.service.HallDaFamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hall-da-fama")
@RequiredArgsConstructor
public class HallDaFamaController {

    private final HallDaFamaService hallDaFamaService;

    @GetMapping
    public ResponseEntity<HallDaFamaDTO> obterHallDaFama() {
        return ResponseEntity.ok(hallDaFamaService.obterHallDaFama());
    }
}