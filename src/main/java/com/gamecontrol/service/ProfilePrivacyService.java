package com.gamecontrol.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;

/**
 * Regra de visibilidade do conteúdo (posts e playlists) de um perfil.
 * <p>Perfil público: qualquer um vê. Perfil privado: só o próprio dono e quem o segue.
 * As reviews não passam por aqui: continuam visíveis na página do jogo.
 */
@Service
public class ProfilePrivacyService {

    private final Firestore firestore;
    private final String nomeColecaoUsuarios;

    public ProfilePrivacyService(
            Firestore firestore,
            @Value("${firebase.collection.users}") String nomeColecaoUsuarios
    ) {
        this.firestore = firestore;
        this.nomeColecaoUsuarios = nomeColecaoUsuarios;
    }

    /**
     * Lança 403 se {@code viewerId} (nulo = anônimo) não pode ver o conteúdo do dono.
     * Se o dono não existir, não há o que proteger e a chamada passa.
     */
    public void exigirAcesso(String donoId, String viewerId) {
        if (donoId == null || donoId.isBlank()) {
            return;
        }
        DocumentSnapshot dono;
        try {
            dono = firestore.collection(nomeColecaoUsuarios).document(donoId).get().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao verificar a privacidade do perfil.", e);
        }
        if (!UserFirestoreMapper.podeVerConteudo(dono, viewerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Este perfil é privado. Siga o usuário para ver o conteúdo."
            );
        }
    }
}
