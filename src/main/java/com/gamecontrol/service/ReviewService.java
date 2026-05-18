package com.gamecontrol.service;

import com.gamecontrol.dto.GameDTO;
import com.gamecontrol.dto.GameReviewsPageDTO;
import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class ReviewService {

    private final Firestore firestore;
    private final UserService userService;
    private final GameService gameService;
    private final String COLLECTION_NAME;

    public ReviewService(
            Firestore firestore,
            UserService userService,
            GameService gameService,
            @Value("${firebase.collection.reviews}") String reviewsCollection
    ) {
        this.firestore = firestore;
        this.userService = userService;
        this.gameService = gameService;
        this.COLLECTION_NAME = reviewsCollection;
    }

    public ReviewDTO saveReview(CreateReviewRequest request, String authenticatedUserId) {

        if (authenticatedUserId == null || !authenticatedUserId.equals(request.getUserId())) {
            throw new SecurityException("Operação não autorizada: Você não pode alterar dados de outro usuário.");
        }

        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", request.getUserId())
                    .whereEqualTo("gameId", request.getGameId());

            QuerySnapshot snapshot = query.get().get();

            if (!snapshot.isEmpty()) {
                DocumentSnapshot existingDoc = snapshot.getDocuments().get(0);

                Map<String, Object> updates = ReviewFirestoreMapper.patchMap(request);

                existingDoc.getReference().update(updates).get();

                DocumentSnapshot updatedDoc =
                        existingDoc.getReference().get().get();

                return ReviewFirestoreMapper.fromSnapshot(updatedDoc);
            }

            DocumentReference docRef =
                    firestore.collection(COLLECTION_NAME).document();

            Map<String, Object> data =
                    ReviewFirestoreMapper.toMap(request);

            var user = userService.buscarUsuarioPorId(request.getUserId());

            if (user != null) {
                data.put("userName", user.getUsername());
                data.put("profilePictureUrl", user.getProfilePictureUrl());
            }

            data.put(
                    "createdAt",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            docRef.set(data).get();

            DocumentSnapshot createdDoc = docRef.get().get();

            return ReviewFirestoreMapper.fromSnapshot(createdDoc);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operação interrompida ao salvar review", e);

        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao executar operação no Firestore", e);

        } catch (Exception e) {
            throw new RuntimeException("Erro inesperado ao salvar review", e);
        }
    }

    public ReviewDTO getReviewById(String id) throws InterruptedException, ExecutionException {
        DocumentReference documentReference = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (!document.exists()) {
            return null;
        }

        ReviewDTO review = ReviewFirestoreMapper.fromSnapshot(document);

        if (review != null) {
            var user = userService.buscarUsuarioPorId(review.getUserId());
            if (user != null) {
                review.setUserName(user.getUsername());
                review.setProfilePictureUrl(user.getProfilePictureUrl());
            } else {
                review.setUserName("Usuário desconhecido");
            }
        }

        return review;
    }

    public String updateReview(String id, CreateReviewRequest request) throws InterruptedException, ExecutionException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);

        ApiFuture<DocumentSnapshot> futureSnapshot = docRef.get();
        DocumentSnapshot document = futureSnapshot.get();

        if (document.exists()) {
            ApiFuture<WriteResult> futureUpdate = docRef.update(
                    "rating", request.getRating(),
                    "description", request.getDescription()
            );
            return "Avaliação atualizada em: " + futureUpdate.get().getUpdateTime().toString();
        }
        return "Erro: Avaliação não encontrada.";
    }

    public List<ReviewDTO> getReviewsByGame(String gameId) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("gameId", gameId)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<ReviewDTO> reviews = new ArrayList<>();

        for (DocumentSnapshot doc : documents) {
            ReviewDTO review = ReviewFirestoreMapper.fromSnapshot(doc);

            if (review != null) {
                var user = userService.buscarUsuarioPorId(review.getUserId());

                if (user != null) {
                    review.setUserName(user.getUsername());
                    review.setProfilePictureUrl(user.getProfilePictureUrl());
                } else {
                    review.setUserName("Usuário desconhecido");
                }

                reviews.add(review);
            }
        }

        return reviews;
    }

    public Double getAverageRating(String gameId) throws Exception {
        QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("gameId", gameId)
                .get()
                .get();

        List<QueryDocumentSnapshot> documents = snapshot.getDocuments();

        if (documents.isEmpty()) return 0.0;

        double sum = 0.0;
        int count = 0;

        for (QueryDocumentSnapshot doc : documents) {
            Double rating = doc.getDouble("rating");
            if (rating != null) {
                sum += rating;
                count++;
            }
        }

        return count == 0 ? 0.0 : Math.round((sum / count) * 10.0) / 10.0;
    }

    public String deleteReview(String id, String authenticatedUserId) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION_NAME).document(id).get().get();

            if (!snapshot.exists()) {
                throw new IllegalArgumentException("Avaliação não encontrada.");
            }

            String reviewOwnerId = snapshot.getString("userId");

            if (authenticatedUserId == null || !authenticatedUserId.equals(reviewOwnerId)) {
                throw new SecurityException("Operação não autorizada: Você não pode excluir a avaliação de outro usuário.");
            }

            firestore.collection(COLLECTION_NAME).document(id).delete().get();
            return "Avaliação " + id + " removida com sucesso.";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operação interrompida ao deletar review", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao executar operação no Firestore", e);
        }
    }

    public GameReviewsPageDTO getReviewPage(String gameId, String userId) throws Exception {
        GameDTO game = gameService.buscarJogoPorId(gameId).orElse(null);
        List<ReviewDTO> reviews = getReviewsByGame(gameId);

        double sum = reviews.stream().mapToDouble(ReviewDTO::getRating).sum();
        double avg = reviews.isEmpty() ? 0.0 : sum / reviews.size();
        double roundedAvg = Math.round(avg * 10.0) / 10.0;
        String display = (roundedAvg % 1 == 0) ? String.format("%.0f", roundedAvg) : String.valueOf(roundedAvg);

        ReviewDTO userReview = null;
        if (userId != null && !userId.isBlank()) {
            userReview = reviews.stream()
                    .filter(r -> userId.equals(r.getUserId()))
                    .findFirst()
                    .orElse(null);

            if (userReview != null) {
                final String targetId = userReview.getId();
                reviews = reviews.stream()
                        .filter(r -> !targetId.equals(r.getId()))
                        .toList();
            }
        }
        return new GameReviewsPageDTO(game, reviews, userReview, roundedAvg, display);
    }


}