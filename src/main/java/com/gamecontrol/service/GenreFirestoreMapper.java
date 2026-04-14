package com.gamecontrol.service;

import com.gamecontrol.dto.GenreDTO;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.List;

final class GenreFirestoreMapper {

    private GenreFirestoreMapper() {}

    static GenreDTO fromSnapshot(DocumentSnapshot doc) {
        GenreDTO dto = new GenreDTO();

        dto.setId(doc.getId());
        dto.setName(doc.getString("name"));
        dto.setSlug(doc.getString("slug"));

        List<String> gameIds = (List<String>) doc.get("gameIds");
        dto.setGameIds(gameIds);

        return dto;
    }
}