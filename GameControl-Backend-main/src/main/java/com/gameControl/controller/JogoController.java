package com.gameControl.controller;

import com.gameControl.dto.JogoDTO;
import com.gameControl.model.Jogo;
import com.gameControl.services.JogoService;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/jogos")
public class JogoController {
    private final JogoService jogoService;

    public JogoController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    @GetMapping
    public ResponseEntity<List<JogoDTO>> listarJogos() {
        List<JogoDTO> jogos = jogoService.listarJogos();
        return ResponseEntity.ok(jogos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JogoDTO> buscarJogoPorID(@PathVariable Long id) {
        return jogoService.buscarJogoPorID(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<JogoDTO> cadastrarJogo(
            @RequestParam String titulo,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String desenvolvedor,
            @RequestParam(required = false) String editora,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataLancamento,
            @RequestParam(required = false) String generos,
            @RequestParam(required = false) String linkExterno,
            @RequestParam(required = false) MultipartFile imagemCapa
    ) {
        JogoDTO jogoDTO = new JogoDTO();
        jogoDTO.setTitulo(titulo);
        jogoDTO.setDescricao(descricao);
        jogoDTO.setDesenvolvedor(desenvolvedor);
        jogoDTO.setEditora(editora);
        jogoDTO.setDataLancamento(dataLancamento);
        jogoDTO.setGeneros(generos);
        jogoDTO.setLinkExterno(linkExterno);
        jogoDTO.setImagemCapa(imagemCapa);

        JogoDTO novoJogo = jogoService.cadastrarJogo(jogoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoJogo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JogoDTO> atualizarJogo(@PathVariable Long id, @Valid @RequestBody Jogo jogo) {
        try {
            JogoDTO jogoAtualizado = jogoService.atualizarJogo(id, jogo);
            return ResponseEntity.ok(jogoAtualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarJogo(@PathVariable Long id) {
        if (jogoService.deletarJogo(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}