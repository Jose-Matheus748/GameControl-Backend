package com.gamecontrol.service;

import com.gamecontrol.dto.FeedPaginaDTO;
import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.gamecontrol.dto.request.UpdateUserPostRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class UserPostService {

    private static final Logger log = LoggerFactory.getLogger(UserPostService.class);

    private final Firestore firestore;
    private final NotificationService notificationService;
    private final ProfilePrivacyService privacyService;
    private final String postsCollection;
    private final String usersCollection;

    public UserPostService(
            Firestore firestore,
            NotificationService notificationService,
            ProfilePrivacyService privacyService,
            @Value("${firebase.collection.posts}") String postsCollection,
            @Value("${firebase.collection.users}") String usersCollection
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
        this.privacyService = privacyService;
        this.postsCollection = postsCollection;
        this.usersCollection = usersCollection;
    }

    public UserPostDTO createPost(CreateUserPostRequest req) {
        try {
            Map<String, Object> dados = UserPostFirestoreMapper.paraDocumento(req);

            DocumentReference ref = firestore.collection(postsCollection).document();
            ref.set(dados).get();

            DocumentSnapshot doc = ref.get().get();

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(req.getUserId())
                    .get()
                    .get();

            return UserPostFirestoreMapper.paraDto(
                    doc,
                    userDoc.getString("username"),
                    userDoc.getString("profilePictureUrl")
            );

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Feed geral: posts de perfis privados só aparecem para quem segue (e para o próprio dono). */
    public List<UserPostDTO> getAllPosts(String viewerId) {
        try {
            QuerySnapshot resultado = firestore.collection(postsCollection)
                    .get()
                    .get();

            return montarLista(resultado.getDocuments(), viewerId);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Feed paginado: posts de quem {@code viewerId} segue (e, se {@code incluirProprios}, os
     * dele mesmo), do mais recente para o mais antigo.
     *
     * <p>Lê só uma página do Firestore em vez da coleção inteira. Como o {@code whereIn} aceita
     * poucos ids, os autores são divididos em lotes consultados em paralelo e os resultados
     * são mesclados. Exige o índice composto {@code posts(userId, createdAt desc)}, declarado
     * em {@code firestore.indexes.json}.
     *
     * @param cursor valor devolvido em {@code proximoCursor} pela página anterior (ou nulo)
     * @throws ResponseStatusException 400 se o cursor for inválido
     */
    public FeedPaginaDTO<UserPostDTO> getFeed(String viewerId, int limite, String cursor, boolean incluirProprios) {
        if (viewerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }

        FeedPaginacao.Cursor inicio;
        try {
            inicio = FeedPaginacao.decodificarCursor(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor inválido.");
        }
        int tamanho = FeedPaginacao.normalizarLimite(limite);

        try {
            DocumentSnapshot viewer = firestore.collection(usersCollection)
                    .document(viewerId)
                    .get()
                    .get();

            Set<String> autores = new LinkedHashSet<>(UserFirestoreMapper.lerListaIds(viewer, "following"));
            if (incluirProprios) {
                autores.add(viewerId);
            } else {
                autores.remove(viewerId);
            }
            if (autores.isEmpty()) {
                return new FeedPaginaDTO<>(List.of(), null, false);
            }

            // Dispara todas as consultas antes de esperar qualquer uma: rodam em paralelo.
            List<ApiFuture<QuerySnapshot>> consultas = new ArrayList<>();
            for (List<String> lote : FeedPaginacao.dividirEmLotes(autores, FeedPaginacao.MAX_IDS_POR_CONSULTA)) {
                Query consulta = firestore.collection(postsCollection)
                        .whereIn("userId", lote)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);
                if (inicio != null) {
                    consulta = consulta.startAfter(inicio.createdAt(), inicio.id());
                }
                // Um a mais que a página: se vier, sabemos que há próxima sem outra consulta.
                consultas.add(consulta.limit(tamanho + 1).get());
            }

            List<List<QueryDocumentSnapshot>> resultados = new ArrayList<>();
            for (ApiFuture<QuerySnapshot> consulta : consultas) {
                resultados.add(consulta.get().getDocuments());
            }

            List<QueryDocumentSnapshot> maisRecentes = FeedPaginacao.mesclarMaisRecentes(
                    resultados,
                    doc -> doc.getString("createdAt"),
                    DocumentSnapshot::getId,
                    tamanho + 1
            );
            boolean temMais = maisRecentes.size() > tamanho;
            List<QueryDocumentSnapshot> pagina = temMais ? maisRecentes.subList(0, tamanho) : maisRecentes;

            String proximoCursor = null;
            if (temMais) {
                QueryDocumentSnapshot ultimo = pagina.get(pagina.size() - 1);
                proximoCursor = FeedPaginacao.codificarCursor(ultimo.getString("createdAt"), ultimo.getId());
            }

            return new FeedPaginaDTO<>(montarLista(pagina, viewerId), proximoCursor, temMais);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (Exception e) {
            log.error("Falha ao montar o feed de {} (o índice composto posts[userId, createdAt] "
                    + "existe? ver firestore.indexes.json): {}", viewerId, e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /** Posts de um perfil. Perfil privado visto por quem não segue: 403. */
    public List<UserPostDTO> getPostsByUser(String userId, String viewerId) {
        privacyService.exigirAcesso(userId, viewerId);
        try {
            QuerySnapshot resultado = firestore.collection(postsCollection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            return montarLista(resultado.getDocuments(), viewerId);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Edita o texto de um post e marca {@code edited = true} (sem guardar data da edição).
     * 401 se anônimo, 404 se o post não existe, 403 se quem pede não é o autor.
     */
    public UserPostDTO updatePost(String postId, UpdateUserPostRequest req, String uid) {
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        if (req.getText() == null || req.getText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O texto do post não pode ser vazio.");
        }
        try {
            DocumentReference ref = firestore.collection(postsCollection).document(postId);
            DocumentSnapshot atual = ref.get().get();

            if (!atual.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado.");
            }
            if (!uid.equals(atual.getString("userId"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o autor pode editar este post.");
            }

            Map<String, Object> alteracoes = new HashMap<>();
            alteracoes.put("text", req.getText());
            alteracoes.put("edited", true);
            ref.update(alteracoes).get();

            DocumentSnapshot doc = ref.get().get();
            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(uid)
                    .get()
                    .get();

            return UserPostFirestoreMapper.paraDto(
                    doc,
                    userDoc.getString("username"),
                    userDoc.getString("profilePictureUrl")
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Apaga o post somente se {@code requesterId} for o dono.
     * 404 se o post não existe; 403 se pertence a outro usuário.
     */
    public void deletePost(String postId, String requesterId) {
        try {
            DocumentReference ref = firestore.collection(postsCollection).document(postId);
            DocumentSnapshot doc = ref.get().get();

            if (!doc.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado.");
            }
            if (requesterId == null || !requesterId.equals(doc.getString("userId"))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Você só pode excluir os seus próprios posts."
                );
            }

            ref.delete().get();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<UserPostDTO> montarLista(List<? extends DocumentSnapshot> posts, String viewerId) throws Exception {
        Map<String, DocumentSnapshot> autores = buscarAutores(posts);
        List<UserPostDTO> lista = new ArrayList<>(posts.size());

        for (DocumentSnapshot doc : posts) {
            String userId = doc.getString("userId");
            if (userId == null || userId.isBlank()) {
                continue;
            }

            DocumentSnapshot userDoc = autores.get(userId);

            if (!UserFirestoreMapper.podeVerConteudo(userDoc, viewerId)) {
                continue;
            }

            lista.add(UserPostFirestoreMapper.paraDto(
                    doc,
                    userDoc != null ? userDoc.getString("username") : null,
                    userDoc != null ? userDoc.getString("profilePictureUrl") : null
            ));
        }

        return lista;
    }

    /** Lê, em uma única chamada, o documento de cada autor distinto dos posts. */
    private Map<String, DocumentSnapshot> buscarAutores(List<? extends DocumentSnapshot> posts) throws Exception {
        Set<String> ids = new LinkedHashSet<>();
        for (DocumentSnapshot post : posts) {
            String userId = post.getString("userId");
            if (userId != null && !userId.isBlank()) {
                ids.add(userId);
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }

        DocumentReference[] referencias = ids.stream()
                .map(id -> firestore.collection(usersCollection).document(id))
                .toArray(DocumentReference[]::new);

        // getAll devolve um snapshot por referência (exists() == false se o usuário não existe).
        Map<String, DocumentSnapshot> porId = new HashMap<>();
        for (DocumentSnapshot autor : firestore.getAll(referencias).get()) {
            porId.put(autor.getId(), autor);
        }
        return porId;
    }

    /** {@code userId} deve ser o uid do token (nunca um valor enviado pelo cliente). */
    public void toggleLike(String postId, String userId) {
        try {

            DocumentReference ref =
                    firestore.collection(postsCollection)
                            .document(postId);

            DocumentSnapshot doc = ref.get().get();

            if (!doc.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado.");
            }

            List<String> likes =
                    (List<String>) doc.get("likedUserIds");

            if (likes == null) {
                likes = new ArrayList<>();
            }

            boolean acabouDeCurtir = !likes.contains(userId);
            if (acabouDeCurtir) {
                likes.add(userId);
            } else {
                likes.remove(userId);
            }

            ref.update("likedUserIds", likes).get();

            // Só notifica ao curtir (não ao descurtir).
            if (acabouDeCurtir) {
                notificarCurtida(doc, postId, userId);
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Falha aqui não pode desfazer a curtida, que já foi gravada. */
    private void notificarCurtida(DocumentSnapshot post, String postId, String likerId) {
        try {
            String donoId = post.getString("userId");
            if (donoId == null || donoId.equals(likerId)) {
                return;
            }
            DocumentSnapshot liker = firestore.collection(usersCollection)
                    .document(likerId)
                    .get()
                    .get();
            notificationService.notificarCurtida(likerId, liker.getString("username"), donoId, postId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Não foi possível notificar curtida do post {}: {}", postId, e.getMessage());
        }
    }
}