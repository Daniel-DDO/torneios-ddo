package com.ddo.torneios.service;

import com.ddo.torneios.model.Insignia;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.InsigniaRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.request.InsigniaRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class InsigniaService {

    @Autowired
    private InsigniaRepository insigniaRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    private static final String JOGOS_10 = "DEDICADO";
    private static final String JOGOS_25 = "COMPROMETIDO";
    private static final String JOGOS_50 = "VETERANO BRONZE";
    private static final String JOGOS_100 = "VETERANO PRATA";
    private static final String JOGOS_250 = "VETERANO OURO";
    private static final String JOGOS_500 = "IMORTAL";
    private static final String JOGOS_1000 = "LENDA VIVA";

    private static final String GOLS_50 = "GOLEADOR";
    private static final String GOLS_100 = "ARTILHEIRO";
    private static final String GOLS_250 = "CARRASCO";
    private static final String GOLS_500 = "MÁQUINA";
    private static final String GOLS_1000 = "PELÉ";

    private static final String MATCH_7_GOLS = "AVALANCHE";
    private static final String MATCH_10_GOLS = "DESTRUIDOR";

    private static final String TITULO_1 = "CAMPEÃO";
    private static final String TITULO_3 = "TRICAMPEÃO";
    private static final String TITULO_5 = "DINASTIA";
    private static final String TITULO_10 = "ABSOLUTO";

    public static final String RECORDISTA = "QUEBRA RECORDES";
    public static final String MVP = "MELHOR DO MUNDO";
    public static final String LENDA_DO_TORNEIO = "ÍCONE";

    public List<Insignia> listarTodas() {
        return insigniaRepository.findAll();
    }

    @Transactional
    public Insignia criarInsignia(InsigniaRequest request) {
        Optional<Insignia> existente = insigniaRepository.findByNome(request.getNome());
        if (existente.isPresent()) {
            throw new RuntimeException("Já existe uma insígnia com o nome: " + request.getNome());
        }

        Insignia insignia = new Insignia();
        insignia.setNome(request.getNome());
        insignia.setDescricao(request.getDescricao());
        insignia.setImagem(request.getImagem());

        return insigniaRepository.save(insignia);
    }

    @Transactional
    public void processarPosPartida(Jogador jogador, int golsNestaPartida) {
        Jogador j = jogadorRepository.findById(jogador.getId()).orElse(jogador);

        long jogos = safe(j.getPartidasJogadas());
        long gols = safe(j.getGolsMarcados());
        long titulos = safe(j.getTitulos());
        boolean mudouAlgo = false;

        if (golsNestaPartida >= 7)  if (atribuirInsignia(j, MATCH_7_GOLS)) mudouAlgo = true;
        if (golsNestaPartida >= 10) if (atribuirInsignia(j, MATCH_10_GOLS)) mudouAlgo = true;

        if (jogos >= 10)   if (atribuirInsignia(j, JOGOS_10)) mudouAlgo = true;
        if (jogos >= 25)   if (atribuirInsignia(j, JOGOS_25)) mudouAlgo = true;
        if (jogos >= 50)   if (atribuirInsignia(j, JOGOS_50)) mudouAlgo = true;
        if (jogos >= 100)  if (atribuirInsignia(j, JOGOS_100)) mudouAlgo = true;
        if (jogos >= 250)  if (atribuirInsignia(j, JOGOS_250)) mudouAlgo = true;
        if (jogos >= 500)  if (atribuirInsignia(j, JOGOS_500)) mudouAlgo = true;
        if (jogos >= 1000) if (atribuirInsignia(j, JOGOS_1000)) mudouAlgo = true;

        if (gols >= 50)    if (atribuirInsignia(j, GOLS_50)) mudouAlgo = true;
        if (gols >= 100)   if (atribuirInsignia(j, GOLS_100)) mudouAlgo = true;
        if (gols >= 250)   if (atribuirInsignia(j, GOLS_250)) mudouAlgo = true;
        if (gols >= 500)   if (atribuirInsignia(j, GOLS_500)) mudouAlgo = true;
        if (gols >= 1000)  if (atribuirInsignia(j, GOLS_1000)) mudouAlgo = true;

        if (titulos >= 1)  if (atribuirInsignia(j, TITULO_1)) mudouAlgo = true;
        if (titulos >= 3)  if (atribuirInsignia(j, TITULO_3)) mudouAlgo = true;
        if (titulos >= 5)  if (atribuirInsignia(j, TITULO_5)) mudouAlgo = true;
        if (titulos >= 10) if (atribuirInsignia(j, TITULO_10)) mudouAlgo = true;

        if (mudouAlgo) {
            jogadorRepository.save(j);
            log.info("Insígnias atualizadas automaticamente para: {}", j.getNome());
        }
    }

    @Transactional
    public void concederInsigniaManual(String jogadorId, String nomeInsignia) {
        Jogador j = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        if (atribuirInsignia(j, nomeInsignia)) {
            jogadorRepository.save(j);
            log.info("Insígnia especial [{}] concedida manualmente para {}", nomeInsignia, j.getNome());
        }
    }

    private boolean atribuirInsignia(Jogador jogador, String nomeInsignia) {
        boolean jaTem = jogador.getInsignias().stream()
                .anyMatch(i -> i.getNome().equalsIgnoreCase(nomeInsignia));

        if (jaTem) return false;

        Optional<Insignia> opt = insigniaRepository.findByNome(nomeInsignia);

        if (opt.isPresent()) {
            jogador.getInsignias().add(opt.get());
            log.info("CONQUISTA: {} desbloqueou [{}]", jogador.getNome(), nomeInsignia);
            return true;
        } else {
            log.debug("Insígnia '{}' não encontrada no banco. Crie-a via endpoint para ativar.", nomeInsignia);
            return false;
        }
    }

    private long safe(Integer i) { return i == null ? 0L : i.longValue(); }
}