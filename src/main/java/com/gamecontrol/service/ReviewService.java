package com.gamecontrol.service;

import com.gamecontrol.dto.ReviewDTO;
import com.gamecontrol.dto.request.CreateReviewRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

@Service
public class ReviewService {

    private static final String COLLECTION_NAME = "reviews";

    public String saveReview(CreateReviewRequest request) throws InterruptedException, ExecutionException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document();

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setId(docRef.getId());
        reviewDTO.setUserId(request.getUserId());
        reviewDTO.setGameId(request.getGameId());
        reviewDTO.setRating(request.getRating());
        reviewDTO.setDescription(request.getDescription());
        reviewDTO.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        ApiFuture<WriteResult> collectionsApiFuture = docRef.set(reviewDTO);
        return collectionsApiFuture.get().getUpdateTime().toString();
    }

    public ReviewDTO getReviewById(String id) throws InterruptedException, ExecutionException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(ReviewDTO.class);
        }
        return null;
    }

    public String updateReview(String id, CreateReviewRequest request) throws InterruptedException, ExecutionException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document(id);

        ApiFuture<DocumentSnapshot> futureSnapshot = docRef.get();
        DocumentSnapshot document = futureSnapshot.get();

        if (document.exists()) {
            // Atualiza apenas campos editáveis para manter integridade
            ApiFuture<WriteResult> futureUpdate = docRef.update(
                    "rating", request.getRating(),
                    "description", request.getDescription()
            );
            return "Avaliação atualizada em: " + futureUpdate.get().getUpdateTime().toString();
        }
        return "Erro: Avaliação não encontrada.";
    }

    public String deleteReview(String id) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        dbFirestore.collection(COLLECTION_NAME).document(id).delete();
        return "Avaliação " + id + " removida com sucesso.";
    }
}