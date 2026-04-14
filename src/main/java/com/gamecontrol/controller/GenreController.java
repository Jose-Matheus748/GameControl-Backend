package com.gamecontrol.controller;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final Firestore firestore;
    private final String collection;

    public GenreController(Firestore firestore,
                           @Value("${firebase.collection.genres}") String collection) {
        this.firestore = firestore;
        this.collection = collection;
    }

    // ✅ GET - listar todos os gêneros
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarGeneros() {
        try {
            List<QueryDocumentSnapshot> docs =
                    firestore.collection(collection).get().get().getDocuments();

            List<Map<String, Object>> lista = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {
                Map<String, Object> genero = new HashMap<>();
                genero.put("id", doc.getId());
                genero.putAll(doc.getData());
                lista.add(genero);
            }

            return ResponseEntity.ok(lista);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ POST - criar automaticamente
    @PostMapping
    public ResponseEntity<List<String>> criarGeneros(@RequestBody List<String> generos) {
        try {
            List<String> ids = new ArrayList<>();

            for (String nome : generos) {
                String slug = nome.toLowerCase().trim().replace(" ", "-");

                var query = firestore.collection(collection)
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

                    var doc = firestore.collection(collection).document();
                    doc.set(novo).get();

                    ids.add(doc.getId());
                }
            }

            return ResponseEntity.ok(ids);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}