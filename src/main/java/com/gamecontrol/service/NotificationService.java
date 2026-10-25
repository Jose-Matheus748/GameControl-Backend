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
import java.util.LinkedHashMap;
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
    private static final int LIMITE_TRECHO = 80;

    private final Firestore firestore;
    private final PushNotificationService pushNotificationService;
    private final String nomeColecaoNotificacoes;
    private final String nomeColecaoUsuarios;

    public NotificationService(
            Firestore firestore,
            PushNotificationService pushNotificationService,
            @Value("${firebase.collection.notifications}") String nomeColecaoNotificacoes,
            @Value("${firebase.collection.users}") String nomeColecaoUsuarios
    ) {
        this.firestore = firestore;
        this.pushNotificationService = pushNotificationService;
        this.nomeColecaoNotificacoes = nomeColecaoNotificacoes;
        this.nomeColecaoUsuarios = nomeColecaoUsuarios;
    }

    /**
     * Chamado quando {@code followerId} começa a seguir {@code followedId}.
     * Nunca lança exceção: uma falha aqui não pode desfazer o "seguir".
     */
    public void notificarNovoSeguidor(String followerId, String followerUsername, String followedId) {
        String nome = nomeOuPadrao(followerUsername);
        notificar(followedId, followerId, nome, NotificationType.NEW_FOLLOWER,
                "Novo seguidor", nome + " começou a seguir você.", null);
    }

    /**
     * Chamado quando {@code likerId} curte um post de {@code postOwnerId}.
     * Não notifica quem curte o próprio post.
     */
    public void notificarCurtida(String likerId, String likerUsername, String postOwnerId, String postId) {
        String nome = nomeOuPadrao(likerUsername);
        notificar(postOwnerId, likerId, nome, NotificationType.POST_LIKED,
                "Nova curtida", nome + " curtiu sua mensagem.", postId);
    }

    /**
     * Chamado quando {@code commenterId} comenta um post de {@code postOwnerId}.
     * Não notifica quem comenta o próprio post.
     */
    public void notificarComentario(
            String commenterId,
            String commenterUsername,
            String postOwnerId,
            String postId,
            String conteudo
    ) {
        String nome = nomeOuPadrao(commenterUsername);
        String mensagem = nome + " comentou na sua mensagem";
        String trecho = resumir(conteudo);
        if (!trecho.isEmpty()) {
            mensagem += ": " + trecho;
        } else {
            mensagem += ".";
        }
        notificar(postOwnerId, commenterId, nome, NotificationType.POST_COMMENTED,
                "Novo comentário", mensagem, postId);
    }

    /**
     * Chamado quando {@code requesterId} pede para seguir o perfil privado {@code targetId}.
     * O pedido aparece na aba de notificações do dono do perfil.
     */
    public void notificarPedidoSeguir(String requesterId, String requesterUsername, String targetId) {
        String nome = nomeOuPadrao(requesterUsername);
        notificar(targetId, requesterId, nome, NotificationType.FOLLOW_REQUEST,
                "Pedido para seguir", nome + " quer seguir você.", null);
    }

    /**
     * Chamado quando {@code ownerId} aceita o pedido de {@code requesterId}.
     * O destinatário é quem pediu; o ator é o dono do perfil.
     */
    public void notificarPedidoAceito(String ownerId, String ownerUsername, String requesterId) {
        String nome = nomeOuPadrao(ownerUsername);
        notificar(requesterId, ownerId, nome, NotificationType.FOLLOW_REQUEST_ACCEPTED,
                "Pedido aceito", nome + " aceitou seu pedido para seguir.", null);
    }

    /**
     * Apaga a notificação de pedido (FOLLOW_REQUEST) já respondida ou cancelada, para o app
     * não continuar mostrando um pedido que não existe mais. Nunca lança exceção.
     */
    public void removerNotificacaoPedido(String ownerId, String requesterId) {
        try {
            QuerySnapshot pedidos = firestore.collection(nomeColecaoNotificacoes)
                    .whereEqualTo("recipientId", ownerId)
                    .whereEqualTo("actorId", requesterId)
                    .whereEqualTo("type", NotificationType.FOLLOW_REQUEST.name())
                    .get()
                    .get();
            if (pedidos.isEmpty()) {
                return;
            }
            WriteBatch batch = firestore.batch();
            for (QueryDocumentSnapshot documento : pedidos.getDocuments()) {
                batch.delete(documento.getReference());
            }
            batch.commit().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrompido ao remover notificação de pedido para seguir.");
        } catch (Exception e) {
            log.warn("Não foi possível remover a notificação do pedido de {} para {}: {}",
                    requesterId, ownerId, e.getMessage());
        }
    }

    /**
     * Fluxo comum: grava no histórico e dispara o push. Nunca lança exceção,
     * porque a ação que originou o evento (seguir, curtir...) já foi concluída.
     */
    private void notificar(
            String recipientId,
            String actorId,
            String actorUsername,
            NotificationType type,
            String tituloPush,
            String mensagem,
            String postId
    ) {
        if (recipientId == null || recipientId.isBlank() || recipientId.equals(actorId)) {
            return;
        }
        // O usuário pode ter desligado este tipo nas configurações: nesse caso
        // não grava no histórico nem envia o push.
        if (!destinatarioQuerReceber(recipientId, type)) {
            log.debug("Notificação {} ignorada: desativada por {}.", type, recipientId);
            return;
        }
        try {
            criar(recipientId, actorId, actorUsername, type, mensagem, postId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrompido ao salvar notificação {}.", type);
        } catch (Exception e) {
            log.warn("Não foi possível salvar notificação {}: {}", type, e.getMessage());
        }

        // O app lê estes campos para navegar ao tocar na notificação.
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("type", type.name());
        dados.put("actorId", actorId);
        if (postId != null) {
            dados.put("postId", postId);
        }
        pushNotificationService.enviarParaUsuario(recipientId, tituloPush, mensagem, dados);
    }

    /** Consulta as preferências do destinatário. Em caso de falha, entrega (não bloqueia por erro). */
    private boolean destinatarioQuerReceber(String recipientId, NotificationType type) {
        try {
            DocumentSnapshot usuario = firestore.collection(nomeColecaoUsuarios)
                    .document(recipientId)
                    .get()
                    .get();
            return UserFirestoreMapper.notificacaoAtiva(usuario, chavePreferencia(type));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        } catch (Exception e) {
            log.warn("Não foi possível ler preferências de {}: {}", recipientId, e.getMessage());
            return true;
        }
    }

    /** Chave, em {@code notificationPreferences}, que controla cada tipo. */
    private static String chavePreferencia(NotificationType type) {
        return switch (type) {
            case NEW_FOLLOWER, FOLLOW_REQUEST, FOLLOW_REQUEST_ACCEPTED -> "followers";
            case POST_LIKED -> "likes";
            case POST_COMMENTED -> "comments";
        };
    }

    private static String nomeOuPadrao(String username) {
        return username != null && !username.isBlank() ? username : "Alguém";
    }

    private static String resumir(String texto) {
        if (texto == null) {
            return "";
        }
        String limpo = texto.trim().replaceAll("\\s+", " ");
        return limpo.length() > LIMITE_TRECHO ? limpo.substring(0, LIMITE_TRECHO) + "…" : limpo;
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
            String message,
            String postId
    ) throws ExecutionException, InterruptedException {
        firestore.collection(nomeColecaoNotificacoes)
                .add(NotificationFirestoreMapper.paraDocumento(recipientId, actorId, actorUsername, type, message, postId))
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