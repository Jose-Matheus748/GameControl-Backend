package com.gamecontrol.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Único ponto do backend que conversa com o Firebase Cloud Messaging (FCM).
 * Guarda os tokens dos aparelhos e envia pushes para eles; não sabe nada
 * sobre regras de negócio (isso fica no {@link NotificationService}).
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    /** Mesmo id do canal criado no app (expo-notifications). */
    private static final String ANDROID_CHANNEL_ID = "default";

    private final Firestore firestore;
    private final FirebaseMessaging firebaseMessaging;
    private final String nomeColecaoTokens;

    public PushNotificationService(
            Firestore firestore,
            FirebaseMessaging firebaseMessaging,
            @Value("${firebase.collection.devicetokens}") String nomeColecaoTokens
    ) {
        this.firestore = firestore;
        this.firebaseMessaging = firebaseMessaging;
        this.nomeColecaoTokens = nomeColecaoTokens;
    }

    /**
     * Associa o token do aparelho ao usuário. O token é o id do documento,
     * então se outra conta logar no mesmo aparelho o dono é substituído.
     */
    public void registrarToken(String userId, String token, String platform) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("userId", userId.trim());
        dados.put("platform", platform);
        dados.put("updatedAt", Instant.now().toString());
        try {
            firestore.collection(nomeColecaoTokens).document(token.trim()).set(dados).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao registrar token do aparelho.", e);
        }
    }

    public void removerToken(String token) {
        try {
            firestore.collection(nomeColecaoTokens).document(token.trim()).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao remover token do aparelho.", e);
        }
    }

    /**
     * Envia um push para todos os aparelhos do usuário. Falhas são apenas
     * registradas no log: o push é um "extra" e não deve quebrar a ação
     * que o originou (seguir, curtir...).
     */
    public void enviarParaUsuario(String userId, String titulo, String corpo, Map<String, String> dados) {
        try {
            List<String> tokens = buscarTokens(userId);
            if (tokens.isEmpty()) {
                return;
            }

            MulticastMessage mensagem = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(corpo)
                            .build())
                    .putAllData(dados)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId(ANDROID_CHANNEL_ID)
                                    .build())
                            .build())
                    .build();

            BatchResponse resposta = firebaseMessaging.sendEachForMulticast(mensagem);
            removerTokensInvalidos(tokens, resposta);
        } catch (Exception e) {
            log.warn("Não foi possível enviar push para o usuário {}: {}", userId, e.getMessage());
        }
    }

    private List<String> buscarTokens(String userId) throws ExecutionException, InterruptedException {
        List<String> tokens = new ArrayList<>();
        for (QueryDocumentSnapshot documento : firestore.collection(nomeColecaoTokens)
                .whereEqualTo("userId", userId)
                .get()
                .get()
                .getDocuments()) {
            tokens.add(documento.getId());
        }
        return tokens;
    }

    /** Aparelhos que desinstalaram o app geram tokens inválidos; limpamos para não acumular. */
    private void removerTokensInvalidos(List<String> tokens, BatchResponse resposta) {
        List<SendResponse> respostas = resposta.getResponses();
        for (int i = 0; i < respostas.size(); i++) {
            FirebaseMessagingException erro = respostas.get(i).getException();
            if (erro == null) {
                continue;
            }
            MessagingErrorCode codigo = erro.getMessagingErrorCode();
            if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                try {
                    removerToken(tokens.get(i));
                } catch (Exception e) {
                    log.warn("Não foi possível remover token inválido: {}", e.getMessage());
                }
            }
        }
    }
}
