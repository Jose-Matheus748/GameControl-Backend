package com.gamecontrol.service;

import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GenreService {

    private final Firestore firestore;
    private final String collection;

    public GenreService(Firestore firestore,
                        @Value("${firebase.collection.genres}") String collection) {
        this.firestore = firestore;
        this.collection = collection;
    }

    public List<String> garantirGeneros(List<String> generos) throws Exception {
        List<String> ids = new ArrayList<>();

        if (generos == null) return ids;

        for (String nome : generos) {

            String slug = nome.toLowerCase().trim().replace(" ", "-");

            QuerySnapshot query = firestore.collection(collection)
                    .whereEqualTo("slug", slug)
                    .limit(1)
                    .get()
                    .get();

            if (!query.isEmpty()) {
                ids.add(query.getDocuments().get(0).getId());
            } else {
                Map<String, Object> novo = new HashMap<>();
                novo.put("name", nome);
                novo.put("slug", slug);

                DocumentReference doc = firestore.collection(collection).document();
                doc.set(novo).get();

                ids.add(doc.getId());
            }
        }

        return ids;
    }
}