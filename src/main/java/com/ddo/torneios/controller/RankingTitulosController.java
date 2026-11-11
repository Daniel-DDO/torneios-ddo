package com.ddo.torneios.controller;

import com.ddo.torneios.dto.RankingTitulosDTO;
import com.ddo.torneios.service.RankingTitulosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RankingTitulosController {

    private final RankingTitulosService rankingService;

    public RankingTitulosController(RankingTitulosService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/players/top-titles")
    public ResponseEntity<List<RankingTitulosDTO>> buscarTop10() {
        return ResponseEntity.ok(rankingService.buscarTop10());
    }
}
