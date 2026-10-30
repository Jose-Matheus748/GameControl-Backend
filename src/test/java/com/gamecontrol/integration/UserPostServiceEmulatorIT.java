package com.gamecontrol.integration;

import com.gamecontrol.dto.UserPostDTO;
import com.gamecontrol.dto.request.CreateUserPostRequest;
import com.gamecontrol.service.NotificationService;
import com.gamecontrol.service.ProfilePrivacyService;
import com.gamecontrol.service.UserPostService;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Testes de integração reais contra o Firestore Emulator (sem mocks de Firestore).
 *
 * <p>Pré-requisito: o Firebase Emulator Suite precisa estar rodando antes de executar
 * esta classe:
 *
 * <pre>
 *   firebase emulators:start --only firestore --project demo-gamecontrol-test
 * </pre>
 *
 * <p>Por terminar em "IT" (não "Test"), o Surefire do {@code mvn test} ignora esta
 * classe por padrão. Rode explicitamente com:
 *
 * <pre>
 *   mvn test -Dtest=UserPostServiceEmulatorIT
 * </pre>
 */
class UserPostServiceEmulatorIT {

    private static final String POSTS_COLLECTION = "posts";
    private static final String USERS_COLLECTION = "users";

    private static Firestore firestore;
    private static UserPostService userPostService;

    private final List<DocumentReference> createdDocs = new java.util.ArrayList<>();

    @BeforeAll
    static void setupEmulator() {
        // setEmulatorHost() é o caminho oficial: internamente o próprio
        // FirestoreOptions.Builder.build() já configura o channel em modo
        // plaintext e troca as credenciais por uma EmulatorCredentials interna
        // (ver FirestoreOptions.java). Não sobrescrever credenciais aqui é
        // proposital — fazer isso manualmente (ex: NoCredentials) quebra essa
        // configuração e causa erro de handshake TLS contra o emulador.
        firestore = FirestoreOptions.newBuilder()
                .setProjectId("demo-gamecontrol-test")
                .setEmulatorHost("localhost:8085")
                .build()
                .getService();

        NotificationService notificationServiceMock = mock(NotificationService.class);
        ProfilePrivacyService privacyService = new ProfilePrivacyService(firestore, USERS_COLLECTION);
        userPostService = new UserPostService(
                firestore,
                notificationServiceMock,
                privacyService,
                POSTS_COLLECTION,
                USERS_COLLECTION
        );
    }

    @AfterAll
    static void closeFirestore() throws Exception {
        if (firestore != null) {
            firestore.close();
        }
    }

    @AfterEach
    void cleanupDocs() throws Exception {
        for (DocumentReference ref : createdDocs) {
            ref.delete().get();
        }
        createdDocs.clear();
    }

    private String createUser(String username) throws Exception {
        String userId = "test-user-" + java.util.UUID.randomUUID();
        Map<String, Object> dados = new HashMap<>();
        dados.put("username", username);
        dados.put("profilePictureUrl", null);

        DocumentReference ref = firestore.collection(USERS_COLLECTION).document(userId);
        ref.set(dados).get();
        createdDocs.add(ref);
        return userId;
    }

    @Test
    void criaPostERecuperaPelaListagemDoUsuario() throws Exception {
        String userId = createUser("JogadorEmulator");

        CreateUserPostRequest req = new CreateUserPostRequest();
        req.setUserId(userId);
        req.setText("Post criado via Firestore Emulator");

        UserPostDTO created = userPostService.createPost(req);
        createdDocs.add(firestore.collection(POSTS_COLLECTION).document(created.getId()));

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getUsername()).isEqualTo("JogadorEmulator");
        assertThat(created.getText()).isEqualTo("Post criado via Firestore Emulator");
        assertThat(created.getLikedUserIds()).isEmpty();

        List<UserPostDTO> posts = userPostService.getPostsByUser(userId, userId);
        assertThat(posts).extracting(UserPostDTO::getId).contains(created.getId());
    }

    @Test
    void alternaCurtidaAdicionandoERemovendoUsuario() throws Exception {
        String authorId = createUser("Autor");
        String likerId = createUser("QuemCurte");

        CreateUserPostRequest req = new CreateUserPostRequest();
        req.setUserId(authorId);
        req.setText("Post para testar curtida");
        UserPostDTO created = userPostService.createPost(req);
        createdDocs.add(firestore.collection(POSTS_COLLECTION).document(created.getId()));

        userPostService.toggleLike(created.getId(), likerId);
        List<UserPostDTO> afterLike = userPostService.getPostsByUser(authorId, authorId);
        UserPostDTO likedPost = afterLike.stream()
                .filter(p -> p.getId().equals(created.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(likedPost.getLikedUserIds()).containsExactly(likerId);

        userPostService.toggleLike(created.getId(), likerId);
        List<UserPostDTO> afterUnlike = userPostService.getPostsByUser(authorId, authorId);
        UserPostDTO unlikedPost = afterUnlike.stream()
                .filter(p -> p.getId().equals(created.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(unlikedPost.getLikedUserIds()).isEmpty();
    }

    @Test
    void deletaPostRemovendoDoFirestore() throws Exception {
        String userId = createUser("DonoDoPost");

        CreateUserPostRequest req = new CreateUserPostRequest();
        req.setUserId(userId);
        req.setText("Post que será excluído");
        UserPostDTO created = userPostService.createPost(req);

        userPostService.deletePost(created.getId(), userId);

        List<UserPostDTO> posts = userPostService.getPostsByUser(userId, userId);
        assertThat(posts).extracting(UserPostDTO::getId).doesNotContain(created.getId());
    }

    @Test
    void naoPermiteApagarPostDeOutroUsuario() throws Exception {
        String donoId = createUser("Dono");
        String intrusoId = createUser("Intruso");

        CreateUserPostRequest req = new CreateUserPostRequest();
        req.setUserId(donoId);
        req.setText("Post protegido");
        UserPostDTO created = userPostService.createPost(req);
        createdDocs.add(firestore.collection(POSTS_COLLECTION).document(created.getId()));

        assertThatThrownBy(() -> userPostService.deletePost(created.getId(), intrusoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        List<UserPostDTO> posts = userPostService.getPostsByUser(donoId, donoId);
        assertThat(posts).extracting(UserPostDTO::getId).contains(created.getId());
    }

    @Test
    void apagarPostInexistenteRetorna404() throws Exception {
        String userId = createUser("Qualquer");

        assertThatThrownBy(() -> userPostService.deletePost("post-que-nao-existe", userId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void curtirPostInexistenteRetorna404() throws Exception {
        String userId = createUser("Curtidor");

        assertThatThrownBy(() -> userPostService.toggleLike("post-que-nao-existe", userId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
