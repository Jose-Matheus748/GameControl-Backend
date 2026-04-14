package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreateGenreRequest;
import com.gamecontrol.dto.GenreDTO;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class GenreService {

    private final Firestore firestore;
    private final String collection;

    public GenreService(Firestore firestore,
                        @Value("${firebase.collection.genres}") String collection) {
        this.firestore = firestore;
        this.collection = collection;
    }

    public GenreDTO createGenre(CreateGenreRequest req) {
        try {
            Map<String, Object> data = GenreFirestoreMapper.toMap(req);

            DocumentReference ref = firestore.collection(collection).document();
            ref.set(data).get();

            DocumentSnapshot doc = ref.get().get();
            return GenreFirestoreMapper.fromSnapshot(doc);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Firestore error", e);
        }
    }

    public List<GenreDTO> listGenres() {
        try {
            QuerySnapshot snapshot = firestore.collection(collection).get().get();

            List<GenreDTO> list = new ArrayList<>();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                list.add(GenreFirestoreMapper.fromSnapshot(doc));
            }

            return list;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Firestore error", e);
        }
    }
}