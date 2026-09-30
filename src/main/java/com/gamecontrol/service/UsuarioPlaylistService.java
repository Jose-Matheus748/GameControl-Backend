package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreatePlaylistRequest;
import com.gamecontrol.dto.GameDTO;
import com.gamecontrol.dto.UsuarioPlayListDTO;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Service
public class UsuarioPlaylistService {

    private final Firestore firestore;
    private final String colecao;
    private final GameService gameService;

    public UsuarioPlaylistService(
            Firestore firestore,
            @Value("${firebase.collection.playlists}") String colecao,
            GameService gameService
    ) {
        this.firestore = firestore;
        this.colecao = colecao;
        this.gameService = gameService;
    }

    public UsuarioPlayListDTO criarPlaylist(String usuarioIdAutenticado, CreatePlaylistRequest request) {
        return executar(() -> {
            request.setUsuarioId(usuarioIdAutenticado);

            Map<String, Object> dados = PlaylistFirestoreMapper.toMapFromRequest(request);

            DocumentReference ref;
            if (request.getDocumentId() != null && !request.getDocumentId().isBlank()) {
                ref = firestore.collection(colecao).document(request.getDocumentId().trim());
            } else {
                ref = firestore.collection(colecao).document();
            }

            ref.set(dados).get();
            return comJogos(PlaylistFirestoreMapper.fromSnapshot(ref.get().get()));
        });
    }

    public List<UsuarioPlayListDTO> listarPlaylistsPorUsuario(String usuarioId, boolean incluirJogos) {
        List<UsuarioPlayListDTO> playlists = executar(() -> {
            QuerySnapshot resultado = firestore.collection(colecao)
                    .whereEqualTo("usuarioId", usuarioId)
                    .get().get();
            List<UsuarioPlayListDTO> lista = new ArrayList<>();
            for (QueryDocumentSnapshot doc : resultado.getDocuments()) {
                lista.add(PlaylistFirestoreMapper.fromSnapshot(doc));
            }
            return lista;
        });
        if (incluirJogos) {
            preencherJogos(playlists);
        }
        return playlists;
    }

    public Optional<UsuarioPlayListDTO> buscarPlaylistPorID(String id) {
        return executar(() -> {
            DocumentSnapshot doc = firestore.collection(colecao).document(id).get().get();
            return doc.exists() ? Optional.of(comJogos(PlaylistFirestoreMapper.fromSnapshot(doc))) : Optional.empty();
        });
    }

    public UsuarioPlayListDTO atualizarPlaylist(String id, UsuarioPlayListDTO dto) {
        return executar(() -> {
            DocumentReference ref = firestore.collection(colecao).document(id);
            if (!ref.get().get().exists()) throw new RuntimeException("Playlist não encontrada.");

            Map<String, Object> campos = PlaylistFirestoreMapper.patchMap(dto);
            ref.set(campos, SetOptions.merge()).get();
            return comJogos(PlaylistFirestoreMapper.fromSnapshot(ref.get().get()));
        });
    }

    public boolean deletarPlaylist(String id) {
        return executar(() -> {
            DocumentReference ref = firestore.collection(colecao).document(id);
            if (!ref.get().get().exists()) return false;
            ref.delete().get();
            return true;
        });
    }

    public UsuarioPlayListDTO adicionarJogo(String playlistId, String gameId) {
        return executar(() -> {
            DocumentReference ref = firestore.collection(colecao).document(playlistId);
            ref.update("jogosIds", FieldValue.arrayUnion(gameId)).get();
            return comJogos(PlaylistFirestoreMapper.fromSnapshot(ref.get().get()));
        });
    }

    public UsuarioPlayListDTO removerJogo(String playlistId, String gameId) {
        return executar(() -> {
            DocumentReference ref = firestore.collection(colecao).document(playlistId);
            ref.update("jogosIds", FieldValue.arrayRemove(gameId)).get();
            return comJogos(PlaylistFirestoreMapper.fromSnapshot(ref.get().get()));
        });
    }

    private UsuarioPlayListDTO comJogos(UsuarioPlayListDTO playlist) {
        if (playlist != null) {
            preencherJogos(List.of(playlist));
        }
        return playlist;
    }

    private void preencherJogos(List<UsuarioPlayListDTO> playlists) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (UsuarioPlayListDTO playlist : playlists) {
            ids.addAll(playlist.getJogosIds());
        }

        Map<String, GameDTO> jogosPorId = new HashMap<>();
        for (GameDTO jogo : gameService.buscarJogosPorIds(ids)) {
            jogosPorId.put(jogo.getId(), jogo);
        }

        for (UsuarioPlayListDTO playlist : playlists) {
            List<GameDTO> jogos = new ArrayList<>();
            for (String id : playlist.getJogosIds()) {
                GameDTO jogo = jogosPorId.get(id);
                if (jogo != null) {
                    jogos.add(jogo);
                }
            }
            playlist.setJogos(jogos);
        }
    }

    private static <T> T executar(Callable<T> operacao) {
        try {
            return operacao.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause().getMessage() : "Erro Firestore", e);
        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado", e);
        }
    }
}