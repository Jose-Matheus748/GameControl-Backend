package com.gamecontrol.service;
import com.gamecontrol.dto.request.CreateGameRequest;
import com.gamecontrol.dto.GameDTO;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Service
public class GameService {

    private final Firestore firestore;
    private final String nomeColecaoJogos;
    private final GenreService genreService;

    public GameService(Firestore firestore, @Value("${firebase.collection.games}") String nomeColecaoJogos, GenreService genreService) {
        this.firestore = firestore;
        this.nomeColecaoJogos = nomeColecaoJogos;
        this.genreService = genreService;
    }

    public List<GameDTO> listarJogos() {
        return executar(() -> {
            QuerySnapshot resultado = firestore.collection(nomeColecaoJogos).get().get();
            List<GameDTO> jogos = new ArrayList<>();

            // Lista de termos recorrentes em slugs/títulos de jogos adultos
            List<String> termosProibidos = List.of(
                    "succubus", "prison", "delude", "hentai", "adult", "sexy", "erotic",
                    "nsfw", "naked", "sex", "porn", "femboy", "sleepover", "ecchi",
                    "harem", "uncensored", "lewd", "fetish", "milf", "corruption",
                    "mistress", "lust", "desire", "breeding", "affair", "sin", "oppai", "cumming", "Opala"
            );

            for (QueryDocumentSnapshot documento : resultado.getDocuments()) {
                GameDTO jogo = GameFirestoreMapper.fromSnapshot(documento);

                if (jogo != null) {
                    // Normalização para minúsculas para garantir a comparação
                    String slugLower = jogo.getSlug() != null ? jogo.getSlug().toLowerCase() : "";
                    String titleLower = jogo.getTitle() != null ? jogo.getTitle().toLowerCase() : "";

                    // Verifica se o slug ou título contém alguma das palavras proibidas
                    boolean contemConteudoRestrito = termosProibidos.stream()
                            .anyMatch(termo -> slugLower.contains(termo) || titleLower.contains(termo));

                    if (!contemConteudoRestrito) {
                        jogos.add(jogo);
                    }
                }
            }
            return jogos;
        });
    }

    public List<GameDTO> listarDozeJogos() {
        return executar(() -> {
            QuerySnapshot resultado = firestore.collection(nomeColecaoJogos)
                    .orderBy("title")
                    .limit(12)
                    .get()
                    .get();
            List<GameDTO> jogos = new ArrayList<>();
            for (QueryDocumentSnapshot documento : resultado.getDocuments()) {
                GameDTO jogo = GameFirestoreMapper.fromSnapshot(documento);
                if (jogo != null) {
                    jogos.add(jogo);
                }
            }
            return jogos;
        });
    }

    public Optional<GameDTO> buscarJogoPorId(String id) {
        return executar(() -> {
            DocumentSnapshot documento = firestore.collection(nomeColecaoJogos).document(id).get().get();
            if (!documento.exists()) {
                return Optional.<GameDTO>empty();
            }
            return Optional.ofNullable(GameFirestoreMapper.fromSnapshot(documento));
        });
    }

    public Optional<GameDTO> buscarJogoPorSlug(String slug) {
        return executar(() -> {
            QuerySnapshot resultado = firestore.collection(nomeColecaoJogos)
                    .whereEqualTo("slug", slug)
                    .limit(1)
                    .get()
                    .get();
            List<QueryDocumentSnapshot> encontrados = resultado.getDocuments();
            if (encontrados.isEmpty()) {
                return Optional.<GameDTO>empty();
            }
            return Optional.ofNullable(GameFirestoreMapper.fromSnapshot(encontrados.getFirst()));
        });
    }

    public GameDTO cadastrarJogo(CreateGameRequest requisicao) {
        return executar(() -> {
            Map<String, Object> dados = GameFirestoreMapper.toMap(requisicao);

            // cria referência e gera ID automaticamente
            DocumentReference referencia =
                    firestore.collection(nomeColecaoJogos).document();

            String gameId = referencia.getId();

            // cria / atualiza gêneros com o ID do jogo
            List<String> genreIds =
                    genreService.garantirGeneros(
                            requisicao.getGenres(),
                            gameId
                    );

            dados.put("genreIds", genreIds);
            dados.remove("genres");

            if (!dados.containsKey("syncedAt")) {
                dados.put("syncedAt", FieldValue.serverTimestamp());
            }

            referencia.set(dados).get();

            DocumentSnapshot salvo = referencia.get().get();

            return GameFirestoreMapper.fromSnapshot(salvo);
        });
    }

    public GameDTO atualizarJogo(String id, GameDTO alteracoes) {
        return executar(() -> {
            DocumentReference referencia = firestore.collection(nomeColecaoJogos).document(id);
            DocumentSnapshot atual = referencia.get().get();
            if (!atual.exists()) {
                throw new IllegalArgumentException("Jogo não encontrado.");
            }
            Map<String, Object> campos = GameFirestoreMapper.patchMap(alteracoes);
            if (campos.isEmpty()) {
                return GameFirestoreMapper.fromSnapshot(atual);
            }
            referencia.set(campos, SetOptions.merge()).get();
            return GameFirestoreMapper.fromSnapshot(referencia.get().get());
        });
    }

    public boolean deletarJogo(String id) {
        return executar(() -> {
            DocumentReference referencia = firestore.collection(nomeColecaoJogos).document(id);
            DocumentSnapshot documento = referencia.get().get();
            if (!documento.exists()) {
                return false;
            }
            referencia.delete().get();
            return true;
        });
    }

    /**
     * O Firestore usa chamadas assíncronas; {@code .get()} pode lançar exceções checadas.
     * Este método centraliza o tratamento para o controller ficar simples.
     */
    private static <T> T executar(Callable<T> operacao) {
        try {
            return operacao.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação no Firestore interrompida.", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(
                    causa != null ? causa.getMessage() : "Falha ao acessar o Firestore.",
                    e
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Erro inesperado no Firestore.", e);
        }
    }

    public void sincronizarGenerosDosJogosExistentes() {
        executar(() -> {
            QuerySnapshot snapshot = firestore.collection(nomeColecaoJogos)
                    .get()
                    .get();

            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {

                String genresString = doc.getString("genres");

                if (genresString == null || genresString.isBlank()) {
                    continue;
                }

                List<String> nomesGeneros = Arrays.stream(genresString.split(","))
                        .map(String::trim)
                        .toList();

                List<String> genreIds =
                        genreService.garantirGeneros(
                                nomesGeneros,
                                doc.getId()
                        );

                doc.getReference().update("genreIds", genreIds).get();
            }

            return null;
        });
    }
}