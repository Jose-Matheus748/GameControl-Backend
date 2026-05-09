package com.gamecontrol.service;

import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private static final String COLLECTION_NAME = "reviews";

    public ReviewDTO saveReview(CreateReviewRequest request) {
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
        List<ReviewDTO> reviews = getReviewsByGame(gameId);

        if (reviews.isEmpty()) return 0.0;

        double sum = reviews.stream()
                .mapToDouble(r -> r.getRating())
                .sum();

        return sum / reviews.size();
    }

    public String deleteReview(String id) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        dbFirestore.collection(COLLECTION_NAME).document(id).delete();
        return "Avaliação " + id + " removida com sucesso.";
    }
}