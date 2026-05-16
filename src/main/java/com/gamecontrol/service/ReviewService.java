package com.gamecontrol.service;

import com.gamecontrol.dto.GameReviewsPageDTO;
import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.cloud.firestore.AggregateField;
import com.google.cloud.firestore.AggregateQuery;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class ReviewService {

    @Autowired
    private UserService userService;
    @Autowired
    private GameService gameService;

    private static final String COLLECTION_NAME = "reviews";

    public ReviewDTO saveReview(CreateReviewRequest request, String authenticatedUserId) {

        if (authenticatedUserId == null || !authenticatedUserId.equals(request.getUserId())) {
            throw new SecurityException("Operação não autorizada: Você não pode alterar dados de outro usuário.");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            Query query = db.collection(COLLECTION_NAME)
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
                    db.collection(COLLECTION_NAME).document();

            Map<String, Object> data =
                    ReviewFirestoreMapper.toMap(request);

            var user = userService.buscarUsuarioPorId(request.getUserId());

            if (user != null) {
                data.put("userName", user.getUsername());
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
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return ReviewFirestoreMapper.fromSnapshot(document);
        }

        ReviewDTO review = ReviewFirestoreMapper.fromSnapshot(document);

        if (review != null) {
            var user = userService.buscarUsuarioPorId(review.getUserId());

            if (user != null) {
                review.setUserName(user.getUsername());
            } else {
                review.setUserName("Usuário desconhecido");
            }
        }

        return review;
    }

    public String updateReview(String id, CreateReviewRequest request) throws InterruptedException, ExecutionException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document(id);

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
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
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
                } else {
                    review.setUserName("Usuário desconhecido");
                }

                reviews.add(review);
            }
        }

        return reviews;
    }

    public Double getAverageRating(String gameId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        Query query = db.collection(COLLECTION_NAME).whereEqualTo("gameId", gameId);

        AggregateQuery aggregateQuery = query.aggregate(AggregateField.average("rating"));

        ApiFuture<AggregateQuerySnapshot> future = aggregateQuery.get();
        AggregateQuerySnapshot snapshot = future.get();

        Double average = snapshot.get(AggregateField.average("rating"));

        return (average != null) ? Math.round(average * 10.0) / 10.0 : 0.0;
    }

    public String deleteReview(String id, String authenticatedUserId) {
        try {
            Firestore dbFirestore = FirestoreClient.getFirestore();

            DocumentSnapshot snapshot = dbFirestore.collection(COLLECTION_NAME).document(id).get().get();

            if (!snapshot.exists()) {
                throw new IllegalArgumentException("Avaliação não encontrada.");
            }

            String reviewOwnerId = snapshot.getString("userId");

            if (authenticatedUserId == null || !authenticatedUserId.equals(reviewOwnerId)) {
                throw new SecurityException("Operação não autorizada: Você não pode excluir a avaliação de outro usuário.");
            }

            dbFirestore.collection(COLLECTION_NAME).document(id).delete().get();
            return "Avaliação " + id + " removida com sucesso.";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operação interrompida ao deletar review", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro ao executar operação no Firestore", e);
        }
    }

    public GameReviewsPageDTO getReviewPage(String gameId, String userId) throws Exception {
        var game = gameService.buscarJogoPorId(gameId);
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