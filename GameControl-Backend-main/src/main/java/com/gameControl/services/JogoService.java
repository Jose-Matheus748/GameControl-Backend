package com.gameControl.services;

import com.gameControl.dto.JogoDTO;
import com.gameControl.model.Jogo;
import com.gameControl.repository.JogoRepository;

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
public class JogoService {
    private final JogoRepository jogoRepository;

    @Value("${game.upload.dir}")
    private String uploadDir;

    public JogoService(JogoRepository jogoRepository) {
        this.jogoRepository = jogoRepository;
    }

    public List<JogoDTO> listarJogos() {
        return jogoRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<JogoDTO> buscarJogoPorID(Long id) {
        return jogoRepository.findById(id).map(this::toDTO);
    }

    public JogoDTO cadastrarJogo(JogoDTO jogoDTO) {
        Jogo jogo = new Jogo();
        jogo.setTitulo(jogoDTO.getTitle());
        jogo.setDescricao(jogoDTO.getDescription());
        jogo.setDesenvolvedor(jogoDTO.getDeveloper());
        jogo.setEditora(jogoDTO.getPublisher());
        jogo.setGeneros(jogoDTO.getGenres());
        jogo.setDataLancamento(jogoDTO.getReleaseDate());

        if (jogoDTO.getCoverImage() != null && !jogoDTO.getCoverImage().isEmpty()) {
            String url = salvarArquivo(jogoDTO.getCoverImage());
            jogo.setUrlCapa(url);
        }

        Jogo jogoSalvo = jogoRepository.save(jogo);
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

            file.transferTo(filePath.toFile());

            return "/games/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar a imagem do jogo", e);
        }
    }

    public JogoDTO atualizarJogo(Long id, Jogo updatedJogo) {
        return jogoRepository.findById(id)
                .map(game -> {
                    game.setTitulo(updatedJogo.getTitulo());
                    game.setDesenvolvedor(updatedJogo.getDesenvolvedor());
                    game.setEditora(updatedJogo.getEditora());
                    game.setGeneros(updatedJogo.getGeneros());
                    game.setDataLancamento(updatedJogo.getDataLancamento());
                    game.setUrlCapa(updatedJogo.getUrlCapa());
                    game.setDescricao(updatedJogo.getDescricao());

                    Jogo jogoSalvo = jogoRepository.save(game);
                    return toDTO(jogoSalvo);
                }).orElseThrow(() -> new RuntimeException("Jogo não encontrado."));
    }

    public boolean deletarJogo(Long id) {
        if (jogoRepository.existsById(id)) {
            jogoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private JogoDTO toDTO(Jogo jogo) {
        JogoDTO dto = new JogoDTO();
        dto.setId(jogo.getId());
        dto.setTitle(jogo.getTitulo());
        dto.setDeveloper(jogo.getDesenvolvedor());
        dto.setPublisher(jogo.getEditora());
        dto.setGenres(jogo.getGeneros());
        dto.setReleaseDate(jogo.getDataLancamento());
        dto.setCoverImageUrl(jogo.getUrlCapa());
        dto.setDescription(jogo.getDescricao());

        return dto;
    }
}
