package com.gamecontrol.service;

import com.gamecontrol.dto.GenreDTO;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GenreService {

    private final Firestore firestore;
    private final String collection;

    public GenreService(
            Firestore firestore,
            @Value("${firebase.collection.genres}") String collection
    ) {
        this.firestore = firestore;
        this.collection = collection;
    }

    //Criei esse meotodo apenas para garantir que nao será criado generos duplicados
    private String gerarSlug(String nome) {
        return nome.toLowerCase()
                .trim()
                .replaceAll("\\s+", "-");
    }

    public List<String> garantirGeneros(List<String> generos, String gameId) throws Exception {
        List<String> ids = new ArrayList<>();

        if (generos == null) return ids;

        for (String nome : generos) {

            String slug = gerarSlug(nome);

            QuerySnapshot query = firestore.collection(collection)
                    .whereEqualTo("slug", slug)
                    .limit(1)
                    .get()
                    .get();

            DocumentReference ref;

            if (!query.isEmpty()) {
                ref = query.getDocuments().get(0).getReference();

                ref.update(
                        "gameIds",
                        FieldValue.arrayUnion(gameId)
                ).get();

            } else {
                ref = firestore.collection(collection).document();

                Map<String, Object> novo = new HashMap<>();
                novo.put("name", nome);
                novo.put("slug", slug);
                novo.put("gameIds", List.of(gameId));

                ref.set(novo).get();
            }

            ids.add(ref.getId());
        }

        return ids;
    }

    public List<GenreDTO> listarGeneros() {
        try {
            QuerySnapshot snapshot =
                    firestore.collection(collection).get().get();

            List<GenreDTO> lista = new ArrayList<>();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                lista.add(GenreFirestoreMapper.fromSnapshot(doc));
            }

            return lista;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}