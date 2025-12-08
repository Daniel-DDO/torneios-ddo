package com.ddo.torneios.controller;

import com.ddo.torneios.model.Avatar;
import com.ddo.torneios.request.AvatarRequest;
import com.ddo.torneios.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avatares")
public class AvatarController {

    @Autowired
    private AvatarService avatarService;

    @GetMapping
    public ResponseEntity<List<Avatar>> listar() {
        return ResponseEntity.ok(avatarService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Avatar> criar(@RequestBody AvatarRequest request) {
        Avatar novo = avatarService.cadastrarAvatar(
                request.getAdminId(),
                request.getNome(),
                request.getUrl()
        );
        return ResponseEntity.status(201).body(novo);
    }
}