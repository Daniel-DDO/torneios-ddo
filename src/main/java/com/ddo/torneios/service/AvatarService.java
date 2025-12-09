package com.ddo.torneios.service;

import com.ddo.torneios.model.Avatar;
import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.AvatarRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.exception.RegraNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AvatarService {

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    public List<Avatar> listarTodos() {
        return avatarRepository.findAll();
    }

    @Transactional
    public Avatar cadastrarAvatar(String adminId, String nome, String url) {
        Jogador admin = jogadorRepository.findById(adminId)
                .orElseThrow(() -> new RegraNegocioException("Usuário admin não encontrado."));

        if (!isCargoAlto(admin.getCargo())) {
            throw new RegraNegocioException("Sem permissão. Apenas Administradores podem adicionar avatares.");
        }

        return avatarRepository.save(new Avatar(nome, url));
    }

    @Transactional
    public void deletarAvatar(String adminId, String avatarId) {
        Jogador admin = jogadorRepository.findById(adminId)
                .orElseThrow(() -> new RegraNegocioException("Admin não encontrado."));

        if (!isCargoAlto(admin.getCargo())) {
            throw new RegraNegocioException("Sem permissão para deletar.");
        }
        avatarRepository.deleteById(avatarId);
    }

    private boolean isCargoAlto(Cargo cargo) {
        return cargo == Cargo.ADMINISTRADOR ||
                cargo == Cargo.DIRETOR ||
                cargo == Cargo.PROPRIETARIO;
    }
}