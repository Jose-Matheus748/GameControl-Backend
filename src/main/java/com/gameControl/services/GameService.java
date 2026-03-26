package com.gameControl.services;

import com.gameControl.dto.GameDTO;
import com.gameControl.model.Game;
import com.gameControl.repository.GameRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class GameService {
    private final GameRepository gameRepository;

    @Value("${game.upload.dir}")
    private String uploadDir;
    
    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<GameDTO> listarJogos() {
        List<Game> jogosListados = gameRepository.findAll();

        return jogosListados.stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<GameDTO> buscarJogoPorID(Long id) {
        return gameRepository.findById(id).map(this::toDTO);
    }


    public GameDTO cadastrarJogo(GameDTO gameDTO) {
        Game jogo = new Game();
        jogo.setTitle(gameDTO.getTitle());
        jogo.setDescription(gameDTO.getDescription());
        jogo.setDeveloper(gameDTO.getDeveloper());
        jogo.setPublisher(gameDTO.getPublisher());
        jogo.setGenres(gameDTO.getGenres());
        jogo.setReleaseDate(gameDTO.getReleaseDate());

        if (gameDTO.getCoverImage() != null && !gameDTO.getCoverImage().isEmpty()) {
            String url = salvarArquivo(gameDTO.getCoverImage());
            jogo.setCoverImageUrl(url);
        }

        Game jogoSalvo = gameRepository.save(jogo);
        return toDTO(jogoSalvo);
    }
    
    private String salvarArquivo(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            System.out.println("Salvando arquivo em: " + filePath.toAbsolutePath()); // <--- log para verificar
            file.transferTo(filePath.toFile());

            return "/games/" + fileName; // URL pública
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar a imagem do jogo", e);
        }
    }

    public GameDTO atualizarJogo(Long id, Game updatedGame) {
        return gameRepository.findById(id)
                .map( game -> {
                    game.setTitle(updatedGame.getTitle());
                    game.setDeveloper(updatedGame.getDeveloper());
                    game.setPublisher(updatedGame.getPublisher());
                    game.setGenres(updatedGame.getGenres());
                    game.setReleaseDate(updatedGame.getReleaseDate());
                    game.setCoverImageUrl(updatedGame.getCoverImageUrl());
                    game.setDescription(updatedGame.getDescription());

                    // A média de avaliação e a contagem de avaliações não devem ser atualizadas diretamente por esta rota
                    // Elas devem ser atualizadas por um service de avaliação separado.

                    Game jogoSalvo = gameRepository.save(game);
                    return toDTO(jogoSalvo);
                }).orElseThrow(() -> new RuntimeException("Jogo não encontrado."));
    }

    public boolean deletarJogo(Long id) {
        if (gameRepository.existsById(id)) {
            gameRepository.deleteById(id);
            return true;
        }

        return false;
    }

    private GameDTO toDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setTitle(game.getTitle());
        dto.setDeveloper(game.getDeveloper());
        dto.setPublisher(game.getPublisher());
        dto.setGenres(game.getGenres());
        dto.setReleaseDate(game.getReleaseDate());
        dto.setCoverImageUrl(game.getCoverImageUrl());
        dto.setDescription(game.getDescription());

        return dto;
    }
}
