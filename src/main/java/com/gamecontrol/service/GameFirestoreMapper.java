package com.gamecontrol.service;

import com.gamecontrol.dto.request.CreateGameRequest;
import com.gamecontrol.dto.GameDTO;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GameFirestoreMapper {

    private GameFirestoreMapper() {
    }

    static GameDTO fromSnapshot(DocumentSnapshot snap) {
        if (!snap.exists()) {
            return null;
        }
        GameDTO dto = new GameDTO();
        dto.setId(snap.getId());
        dto.setCoverImageUrl(snap.getString("coverImageUrl"));
        dto.setDescription(snap.getString("description"));
        dto.setDeveloper(snap.getString("developer"));
        Object genres = snap.get("genreIds");
        if (genres instanceof List<?> list) {
            dto.setGenreIds((List<String>) list);
        }
        dto.setIgdbId(toLong(snap.get("igdbId")));
        dto.setIgdbPopularityValue(toDouble(snap.get("igdbPopularityValue")));
        dto.setIgdbUrl(snap.getString("igdbUrl"));
        dto.setPublisher(snap.getString("publisher"));
        dto.setRank(toLong(snap.get("rank")));
        dto.setRating(toDouble(snap.get("rating")));
        dto.setRatingCount(toLong(snap.get("ratingCount")));
        dto.setReleaseDate(snap.getString("releaseDate"));
        dto.setSlug(snap.getString("slug"));
        dto.setTitle(snap.getString("title"));
        dto.setThemeId(toInteger(snap.get("themeId")));
        dto.setTotalRating(toDouble(snap.get("totalRating")));
        dto.setTotalRatingCount(toLong(snap.get("totalRatingCount")));
        Timestamp ts = snap.getTimestamp("syncedAt");
        if (ts != null) {
            dto.setSyncedAt(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()));
        }
        return dto;
    }

    static Map<String, Object> toMap(CreateGameRequest req) {
        Map<String, Object> m = new HashMap<>();
        putIfNotNull(m, "coverImageUrl", req.getCoverImageUrl());
        putIfNotNull(m, "description", req.getDescription());
        putIfNotNull(m, "developer", req.getDeveloper());
        putIfNotNull(m, "genreIds", req.getGenreIds());
        putIfNotNull(m, "igdbId", req.getIgdbId());
        putIfNotNull(m, "igdbPopularityValue", req.getIgdbPopularityValue());
        putIfNotNull(m, "igdbUrl", req.getIgdbUrl());
        putIfNotNull(m, "publisher", req.getPublisher());
        putIfNotNull(m, "rank", req.getRank());
        putIfNotNull(m, "rating", req.getRating());
        putIfNotNull(m, "ratingCount", req.getRatingCount());
        putIfNotNull(m, "releaseDate", req.getReleaseDate());
        putIfNotNull(m, "slug", req.getSlug());
        putIfNotNull(m, "themeId", req.getThemeId());
        m.put("title", req.getTitle());
        putIfNotNull(m, "totalRating", req.getTotalRating());
        putIfNotNull(m, "totalRatingCount", req.getTotalRatingCount());
        if (req.getSyncedAt() != null) {
            Instant i = req.getSyncedAt();
            m.put("syncedAt", Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano()));
        }
        return m;
    }

    /**
     * Campos não nulos são gravados (merge). {@code syncedAt} em null não altera o campo no merge.
     */
    static Map<String, Object> patchMap(GameDTO patch) {
        Map<String, Object> m = new HashMap<>();
        putIfNotNull(m, "coverImageUrl", patch.getCoverImageUrl());
        putIfNotNull(m, "description", patch.getDescription());
        putIfNotNull(m, "developer", patch.getDeveloper());
        putIfNotNull(m, "genreIds", patch.getGenres());
        putIfNotNull(m, "igdbId", patch.getIgdbId());
        putIfNotNull(m, "igdbPopularityValue", patch.getIgdbPopularityValue());
        putIfNotNull(m, "igdbUrl", patch.getIgdbUrl());
        putIfNotNull(m, "publisher", patch.getPublisher());
        putIfNotNull(m, "rank", patch.getRank());
        putIfNotNull(m, "rating", patch.getRating());
        putIfNotNull(m, "ratingCount", patch.getRatingCount());
        putIfNotNull(m, "releaseDate", patch.getReleaseDate());
        putIfNotNull(m, "slug", patch.getSlug());
        putIfNotNull(m, "themeId", patch.getThemeId());
        putIfNotNull(m, "title", patch.getTitle());
        putIfNotNull(m, "totalRating", patch.getTotalRating());
        putIfNotNull(m, "totalRatingCount", patch.getTotalRatingCount());
        if (patch.getSyncedAt() != null) {
            Instant i = patch.getSyncedAt();
            m.put("syncedAt", Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano()));
        }
        return m;
    }

    private static void putIfNotNull(Map<String, Object> m, String key, Object value) {
        if (value != null) {
            m.put(key, value);
        }
    }

    private static Integer toInteger(Object v) {

        if (v == null) return null;

        if (v instanceof Integer i) return i;

        if (v instanceof Long l) return l.intValue();

        return ((Number) v).intValue();
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Integer i) {
            return i.longValue();
        }
        if (v instanceof Double d) {
            return d.longValue();
        }
        return ((Number) v).longValue();
    }

    private static Double toDouble(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Double d) {
            return d;
        }
        if (v instanceof Float f) {
            return f.doubleValue();
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }
}