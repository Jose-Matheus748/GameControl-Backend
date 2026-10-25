package com.gamecontrol.service;

import com.gamecontrol.dto.request.FollowRequestDTO;
import com.gamecontrol.dto.FollowStatusDTO;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Pedidos para seguir perfis privados.
 * <p>Cada pedido é um documento em {@code followRequests} com id {@code <solicitante>_<alvo>}
 * (então só existe um por par). Estados: {@code PENDING} (aguardando) e {@code REJECTED}
 * (recusado, guardado só para controlar o tempo até poder pedir de novo). Quando o pedido é
 * aceito ou cancelado, o documento é apagado.
 */
@Service
public class FollowRequestService {

    private static final Logger log = LoggerFactory.getLogger(FollowRequestService.class);

    static final String PENDING = "PENDING";
    static final String REJECTED = "REJECTED";

    private final Firestore firestore;
    private final NotificationService notificationService;
    private final String nomeColecaoPedidos;
    private final String nomeColecaoUsuarios;
    private final long segundosParaNovaTentativa;

    public FollowRequestService(
            Firestore firestore,
            NotificationService notificationService,
            @Value("${firebase.collection.followrequests:followRequests}") String nomeColecaoPedidos,
            @Value("${firebase.collection.users}") String nomeColecaoUsuarios,
            @Value("${app.privacy.follow-request-retry-seconds:30}") long segundosParaNovaTentativa
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
        this.nomeColecaoPedidos = nomeColecaoPedidos;
        this.nomeColecaoUsuarios = nomeColecaoUsuarios;
        this.segundosParaNovaTentativa = segundosParaNovaTentativa;
    }

    /**
     * {@code requesterId} pede para seguir {@code targetId} (perfil privado).
     * Idempotente: se já existe um pedido pendente, não faz nada e não notifica de novo.
     * Se o último pedido foi recusado há pouco tempo, responde 429.
     */
    public void solicitar(String requesterId, String requesterUsername, String targetId) {
        DocumentReference referencia = referencia(requesterId, targetId);
        try {
            DocumentSnapshot atual = referencia.get().get();
            if (atual.exists()) {
                if (PENDING.equals(atual.getString("status"))) {
                    return;
                }
                long restante = segundosRestantes(atual);
                if (restante > 0) {
                    throw new ResponseStatusException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Seu pedido foi recusado. Você poderá tentar de novo em " + restante + " segundo(s)."
                    );
                }
            }

            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("requesterId", requesterId);
            dados.put("targetId", targetId);
            dados.put("status", PENDING);
            dados.put("createdAt", Instant.now().toString());
            // set() substitui o documento inteiro, então um "rejectedAt" antigo some.
            referencia.set(dados).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao solicitar para seguir.", e);
        }

        notificationService.notificarPedidoSeguir(requesterId, requesterUsername, targetId);
    }

    /** Pedidos pendentes recebidos por {@code ownerId}, do mais recente para o mais antigo. */
    public List<FollowRequestDTO> listarRecebidas(String ownerId) {
        try {
            QuerySnapshot resultado = firestore.collection(nomeColecaoPedidos)
                    .whereEqualTo("targetId", ownerId)
                    .whereEqualTo("status", PENDING)
                    .get()
                    .get();

            List<FollowRequestDTO> lista = new ArrayList<>();
            for (QueryDocumentSnapshot pedido : resultado.getDocuments()) {
                String requesterId = pedido.getString("requesterId");
                FollowRequestDTO dto = new FollowRequestDTO();
                dto.setRequesterId(requesterId);
                dto.setCreatedAt(pedido.getString("createdAt"));
                if (requesterId != null) {
                    DocumentSnapshot solicitante = firestore.collection(nomeColecaoUsuarios)
                            .document(requesterId)
                            .get()
                            .get();
                    dto.setRequesterUsername(solicitante.getString("username"));
                    dto.setRequesterProfilePictureUrl(solicitante.getString("profilePictureUrl"));
                }
                lista.add(dto);
            }
            lista.sort(Comparator.comparing(
                    FollowRequestDTO::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));
            return lista;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao listar pedidos para seguir.", e);
        }
    }

    /** {@code ownerId} aceita o pedido de {@code requesterId}: passa a ser seguido e o pedido some. */
    public void aceitar(String ownerId, String requesterId) {
        DocumentReference referenciaPedido = referencia(requesterId, ownerId);
        String nomeDono = null;
        try {
            exigirPendente(referenciaPedido.get().get());

            DocumentReference refDono = firestore.collection(nomeColecaoUsuarios).document(ownerId);
            DocumentReference refSolicitante = firestore.collection(nomeColecaoUsuarios).document(requesterId);
            DocumentSnapshot dono = refDono.get().get();
            DocumentSnapshot solicitante = refSolicitante.get().get();
            if (!dono.exists() || !solicitante.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }
            nomeDono = dono.getString("username");

            WriteBatch batch = firestore.batch();
            batch.update(refDono, "followers", FieldValue.arrayUnion(requesterId));
            batch.update(refSolicitante, "following", FieldValue.arrayUnion(ownerId));
            batch.delete(referenciaPedido);
            batch.commit().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao aceitar pedido para seguir.", e);
        }

        notificationService.removerNotificacaoPedido(ownerId, requesterId);
        notificationService.notificarPedidoAceito(ownerId, nomeDono, requesterId);
    }

    /**
     * {@code ownerId} recusa o pedido de {@code requesterId}. O documento fica como REJECTED só
     * para marcar a hora: o solicitante pode pedir de novo depois do intervalo configurado.
     */
    public void recusar(String ownerId, String requesterId) {
        DocumentReference referenciaPedido = referencia(requesterId, ownerId);
        try {
            exigirPendente(referenciaPedido.get().get());
            referenciaPedido.update(
                    "status", REJECTED,
                    "rejectedAt", Instant.now().toString()
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao recusar pedido para seguir.", e);
        }

        notificationService.removerNotificacaoPedido(ownerId, requesterId);
    }

    /**
     * Cancela um pedido ainda pendente (usado quando o solicitante toca em "Solicitado" ou chama
     * deixar de seguir). Pedido recusado não é apagado, para não burlar o intervalo de espera.
     */
    public void cancelarPendente(String requesterId, String targetId) {
        DocumentReference referenciaPedido = referencia(requesterId, targetId);
        try {
            DocumentSnapshot pedido = referenciaPedido.get().get();
            if (pedido.exists() && PENDING.equals(pedido.getString("status"))) {
                referenciaPedido.delete().get();
                notificationService.removerNotificacaoPedido(targetId, requesterId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao cancelar pedido para seguir.", e);
        }
    }

    /** Relação de {@code userId} com {@code targetId}, para o app escolher o botão. */
    public FollowStatusDTO status(String userId, String targetId) {
        try {
            DocumentSnapshot usuario = firestore.collection(nomeColecaoUsuarios).document(userId).get().get();
            if (!usuario.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }
            if (UserFirestoreMapper.lerListaIds(usuario, "following").contains(targetId)) {
                return new FollowStatusDTO("FOLLOWING", 0);
            }

            DocumentSnapshot pedido = referencia(userId, targetId).get().get();
            if (pedido.exists()) {
                if (PENDING.equals(pedido.getString("status"))) {
                    return new FollowStatusDTO("REQUESTED", 0);
                }
                return new FollowStatusDTO("NONE", segundosRestantes(pedido));
            }
            return new FollowStatusDTO("NONE", 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao consultar a relação entre os usuários.", e);
        }
    }

    /**
     * Quando o perfil deixa de ser privado, os pedidos pendentes são aceitos (como no Instagram).
     * Nunca lança exceção: a troca de privacidade já foi gravada.
     */
    public void aceitarTodosPendentes(String ownerId) {
        try {
            QuerySnapshot pendentes = firestore.collection(nomeColecaoPedidos)
                    .whereEqualTo("targetId", ownerId)
                    .whereEqualTo("status", PENDING)
                    .get()
                    .get();
            for (QueryDocumentSnapshot pedido : pendentes.getDocuments()) {
                String requesterId = pedido.getString("requesterId");
                if (requesterId == null) {
                    continue;
                }
                try {
                    aceitar(ownerId, requesterId);
                } catch (RuntimeException e) {
                    log.warn("Não foi possível aceitar o pedido de {} para {}: {}", requesterId, ownerId, e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Não foi possível aceitar os pedidos pendentes de {}: {}", ownerId, e.getMessage());
        }
    }

    /** Apaga todos os pedidos em que o usuário aparece (como solicitante ou como alvo). */
    public void removerTodosDoUsuario(String userId) {
        try {
            WriteBatch batch = firestore.batch();
            boolean temAlgo = false;
            for (String campo : List.of("requesterId", "targetId")) {
                QuerySnapshot resultado = firestore.collection(nomeColecaoPedidos)
                        .whereEqualTo(campo, userId)
                        .get()
                        .get();
                for (QueryDocumentSnapshot pedido : resultado.getDocuments()) {
                    batch.delete(pedido.getReference());
                    temAlgo = true;
                }
            }
            if (temAlgo) {
                batch.commit().get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Erro ao remover pedidos para seguir.", e);
        }
    }

    private DocumentReference referencia(String requesterId, String targetId) {
        return firestore.collection(nomeColecaoPedidos).document(requesterId + "_" + targetId);
    }

    private static void exigirPendente(DocumentSnapshot pedido) {
        if (!pedido.exists() || !PENDING.equals(pedido.getString("status"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido para seguir não encontrado.");
        }
    }

    /** Segundos que faltam para poder pedir de novo após uma recusa (0 = já pode). */
    private long segundosRestantes(DocumentSnapshot pedido) {
        String texto = pedido.getString("rejectedAt");
        if (texto == null) {
            return 0;
        }
        try {
            long decorridoMs = Duration.between(Instant.parse(texto), Instant.now()).toMillis();
            long restanteMs = segundosParaNovaTentativa * 1000 - decorridoMs;
            return restanteMs <= 0 ? 0 : (restanteMs + 999) / 1000;
        } catch (DateTimeParseException e) {
            return 0;
        }
    }
}
