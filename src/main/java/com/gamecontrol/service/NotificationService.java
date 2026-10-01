package com.gamecontrol.service;

import com.gamecontrol.dto.NotificationDTO;
import com.gamecontrol.enums.NotificationType;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Regras das notificações: cria o registro no Firestore (histórico exibido
 * no app) e pede ao {@link PushNotificationService} o envio do push.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int LIMITE_LISTAGEM = 50;

    private final Firestore firestore;
    private final PushNotificationService pushNotificationService;
    private final String nomeColecaoNotificacoes;

    public NotificationService(
            Firestore firestore,
            PushNotificationService pushNotificationService,
            @Value("${firebase.collection.notifications}") String nomeColecaoNotificacoes
    ) {
        this.firestore = firestore;
        this.pushNotificationService = pushNotificationService;
        this.nomeColecaoNotificacoes = nomeColecaoNotificacoes;
    }

    /**
     * Chamado quando {@code followerId} começa a seguir {@code followedId}.
     * Nunca lança exceção: uma falha aqui não pode desfazer o "seguir".
     */
    public void notificarNovoSeguidor(String followerId, String followerUsername, String followedId) {
        String nome = followerUsername != null && !followerUsername.isBlank()
                ? followerUsername
                : "Alguém";
        String mensagem = nome + " começou a seguir você.";
        try {
            criar(followedId, followerId, nome, NotificationType.NEW_FOLLOWER, mensagem);
        } catch (Exception e) {
            log.warn("Não foi possível salvar notificação de novo seguidor: {}", e.getMessage());
        }
        pushNotificationService.enviarParaUsuario(
                followedId,
                "Novo seguidor",
                mensagem,
                Map.of(
                        "type", NotificationType.NEW_FOLLOWER.name(),
                        "actorId", followerId
                )
        );
    }

    public List<NotificationDTO> listarDoUsuario(String userId) {
        List<NotificationDTO> notificacoes = new ArrayList<>();
        for (QueryDocumentSnapshot documento : buscarDoUsuario(userId).getDocuments()) {
            notificacoes.add(NotificationFirestoreMapper.paraDto(documento));
        }
        // Ordenação em memória para não exigir índice composto no Firestore.
        notificacoes.sort(Comparator.comparing(
                NotificationDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return notificacoes.size() > LIMITE_LISTAGEM
                ? notificacoes.subList(0, LIMITE_LISTAGEM)
                : notificacoes;
    }

    public long contarNaoLidas(String userId) {
        try {
            return firestore.collection(nomeColecaoNotificacoes)
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("read", false)
                    .get()
                    .get()
                    .size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao contar notificações.", e);
        }
    }

    public void marcarComoLida(String id) {
        try {
            DocumentReference referencia = firestore.collection(nomeColecaoNotificacoes).document(id);
            DocumentSnapshot documento = referencia.get().get();
            if (!documento.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada.");
            }
            referencia.update("read", true).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao marcar notificação como lida.", e);
        }
    }

    public void marcarTodasComoLidas(String userId) {
        try {
            QuerySnapshot naoLidas = firestore.collection(nomeColecaoNotificacoes)
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("read", false)
                    .get()
                    .get();
            if (naoLidas.isEmpty()) {
                return;
            }
            WriteBatch batch = firestore.batch();
            for (QueryDocumentSnapshot documento : naoLidas.getDocuments()) {
                batch.update(documento.getReference(), "read", true);
            }
            batch.commit().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao marcar notificações como lidas.", e);
        }
    }

    private void criar(
            String recipientId,
            String actorId,
            String actorUsername,
            NotificationType type,
            String message
    ) throws ExecutionException, InterruptedException {
        firestore.collection(nomeColecaoNotificacoes)
                .add(NotificationFirestoreMapper.paraDocumento(recipientId, actorId, actorUsername, type, message))
                .get();
    }

    private QuerySnapshot buscarDoUsuario(String userId) {
        try {
            return firestore.collection(nomeColecaoNotificacoes)
                    .whereEqualTo("recipientId", userId)
                    .get()
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao listar notificações.", e);
        }
    }
}
