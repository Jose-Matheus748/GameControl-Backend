package com.gamecontrol.service;

import com.gamecontrol.dto.PostCommentDTO;
import com.gamecontrol.dto.request.CreatePostCommentRequest;
import com.gamecontrol.dto.request.UpdatePostCommentRequest;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class PostCommentService {

    private static final Logger log = LoggerFactory.getLogger(PostCommentService.class);

    private final Firestore firestore;
    private final NotificationService notificationService;
    private final String commentsCollection;
    private final String usersCollection;
    private final String postsCollection;

    public PostCommentService(
            Firestore firestore,
            NotificationService notificationService,
            @Value("${firebase.collection.postcomments}") String commentsCollection,
            @Value("${firebase.collection.users}") String usersCollection,
            @Value("${firebase.collection.posts}") String postsCollection
    ) {
        this.firestore = firestore;
        this.notificationService = notificationService;
        this.commentsCollection = commentsCollection;
        this.usersCollection = usersCollection;
        this.postsCollection = postsCollection;
    }

    public PostCommentDTO createComment(CreatePostCommentRequest req) {
        try {
            Map<String, Object> dados = PostCommentFirestoreMapper.paraDocumento(req);

            // Salva o comentário
            DocumentReference commentRef = firestore.collection(commentsCollection).document();
            commentRef.set(dados).get();

            // Atualiza o array commentIds no post
            DocumentReference postRef = firestore.collection(postsCollection).document(req.getPostId());
            postRef.update("commentIds", FieldValue.arrayUnion(commentRef.getId())).get();

            DocumentSnapshot doc = commentRef.get().get();

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(req.getUserId())
                    .get()
                    .get();

            String username = userDoc.getString("username");
            notificarComentario(req, username);

            return PostCommentFirestoreMapper.paraDto(doc, username);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Falha aqui não pode desfazer o comentário, que já foi gravado. */
    private void notificarComentario(CreatePostCommentRequest req, String commenterUsername) {
        try {
            DocumentSnapshot post = firestore.collection(postsCollection)
                    .document(req.getPostId())
                    .get()
                    .get();
            String donoId = post.getString("userId");
            if (donoId == null || donoId.equals(req.getUserId())) {
                return;
            }
            notificationService.notificarComentario(
                    req.getUserId(), commenterUsername, donoId, req.getPostId(), req.getContent());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Não foi possível notificar comentário no post {}: {}", req.getPostId(), e.getMessage());
        }
    }

    public List<PostCommentDTO> getCommentsByPost(String postId) {
        try {
            QuerySnapshot resultado = firestore.collection(commentsCollection)
                    .whereEqualTo("postId", postId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<PostCommentDTO> getCommentsByUser(String userId) {
        try {
            QuerySnapshot resultado = firestore.collection(commentsCollection)
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            return montarLista(resultado);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Edita o conteúdo de um comentário e marca {@code edited = true} (sem guardar data da edição).
     * 401 se anônimo, 404 se o comentário não existe, 403 se quem pede não é o autor.
     */
    public PostCommentDTO updateComment(String commentId, UpdatePostCommentRequest req, String uid) {
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comentário não pode ser vazio.");
        }
        try {
            DocumentReference ref = firestore.collection(commentsCollection).document(commentId);
            DocumentSnapshot atual = ref.get().get();

            if (!atual.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado.");
            }
            if (!uid.equals(atual.getString("userId"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o autor pode editar este comentário.");
            }

            Map<String, Object> alteracoes = new HashMap<>();
            alteracoes.put("content", req.getContent());
            alteracoes.put("edited", true);
            ref.update(alteracoes).get();

            DocumentSnapshot doc = ref.get().get();
            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(uid)
                    .get()
                    .get();

            return PostCommentFirestoreMapper.paraDto(doc, userDoc.getString("username"));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida.", e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void deleteComment(String commentId) {
        try {
            // Busca o comentário para saber o postId antes de deletar
            DocumentSnapshot doc = firestore.collection(commentsCollection)
                    .document(commentId)
                    .get()
                    .get();

            String postId = doc.getString("postId");

            // Remove o ID do array no post
            if (postId != null) {
                firestore.collection(postsCollection)
                        .document(postId)
                        .update("commentIds", FieldValue.arrayRemove(commentId));
            }

            firestore.collection(commentsCollection).document(commentId).delete();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<PostCommentDTO> montarLista(QuerySnapshot resultado) throws Exception {
        List<PostCommentDTO> lista = new ArrayList<>();

        for (DocumentSnapshot doc : resultado.getDocuments()) {
            String userId = doc.getString("userId");

            DocumentSnapshot userDoc = firestore.collection(usersCollection)
                    .document(userId)
                    .get()
                    .get();

            lista.add(PostCommentFirestoreMapper.paraDto(doc, userDoc.getString("username")));
        }

        return lista;
    }
}