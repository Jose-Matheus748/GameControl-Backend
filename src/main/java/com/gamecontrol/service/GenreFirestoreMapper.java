package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreateGenreRequest;
import com.gamecontrol.dto.GenreDTO;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

final class GenreFirestoreMapper {

    private GenreFirestoreMapper() {}

    static Map<String, Object> toMap(CreateGenreRequest req) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", req.getName());
        return m;
    }

    static GenreDTO fromSnapshot(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        GenreDTO dto = new GenreDTO();
        dto.setId(doc.getId());
        dto.setName(doc.getString("name"));
        return dto;
    }
}