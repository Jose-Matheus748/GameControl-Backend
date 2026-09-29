package com.gamecontrol.service;
import com.gamecontrol.dto.request.CreateGameRequest;
import com.gamecontrol.dto.GameDTO;
import com.gamecontrol.dto.PaginaDTO;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    public static final int QUANTIDADE_DESTAQUES_PADRAO = 12;
    private static final int QUANTIDADE_DESTAQUES_MAXIMA = 50;

    public static final int TAMANHO_PAGINA_PADRAO = 12;
    private static final int TAMANHO_PAGINA_MAXIMO = 50;

    private static final int CANDIDATOS_JOGO_DA_SEMANA = 20;
    private static final ZoneId FUSO_JOGO_DA_SEMANA = ZoneId.of("America/Sao_Paulo");

    private static final Duration VALIDADE_CACHE_JOGOS = Duration.ofMinutes(10);

    private record CacheJogos(List<GameDTO> jogos, Map<String, GameDTO> porId, Instant carregadoEm) {
        boolean valido() {
            return carregadoEm.plus(VALIDADE_CACHE_JOGOS).isAfter(Instant.now());
        }
    }

    private volatile CacheJogos cacheJogos;

    public List<GameDTO> listarJogos() {
        return obterCacheJogos().jogos();
    }

    public PaginaDTO<GameDTO> listarCatalogo(String titulo, int pagina, int tamanho) {
        int tamanhoValido = Math.max(1, Math.min(tamanho, TAMANHO_PAGINA_MAXIMO));
        int paginaValida = Math.max(0, pagina);

        List<GameDTO> encontrados = buscarJogosPorTitulo(titulo);
        int inicio = (int) Math.min((long) paginaValida * tamanhoValido, encontrados.size());
        int fim = Math.min(inicio + tamanhoValido, encontrados.size());
        int totalPaginas = (int) Math.ceil((double) encontrados.size() / tamanhoValido);

        return new PaginaDTO<>(
                encontrados.subList(inicio, fim),
                paginaValida,
                tamanhoValido,
                encontrados.size(),
                totalPaginas
        );
    }

    public Optional<GameDTO> buscarJogoDaSemana() {
        List<GameDTO> jogos = obterCacheJogos().jogos();
        List<GameDTO> candidatos = jogos.stream()
                .filter(jogo -> jogo.getIgdbPopularityValue() != null)
                .sorted(Comparator.comparing(GameDTO::getIgdbPopularityValue).reversed())
                .limit(CANDIDATOS_JOGO_DA_SEMANA)
                .toList();
        if (candidatos.isEmpty()) {
            candidatos = jogos;
        }
        if (candidatos.isEmpty()) {
            return Optional.empty();
        }

        long semana = Math.floorDiv(LocalDate.now(FUSO_JOGO_DA_SEMANA).toEpochDay() + 3, 7);
        return Optional.of(candidatos.get(Math.floorMod(semana, candidatos.size())));
    }

    public List<GameDTO> buscarJogosPorIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, GameDTO> encontrados = new HashMap<>();
        Map<String, GameDTO> porId = obterCacheJogos().porId();
        List<DocumentReference> faltantes = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            GameDTO jogo = porId.get(id);
            if (jogo != null) {
                encontrados.put(id, jogo);
            } else {
                faltantes.add(firestore.collection(nomeColecaoJogos).document(id));
            }
        }

        if (!faltantes.isEmpty()) {
            executar(() -> {
                for (DocumentSnapshot documento : firestore.getAll(faltantes.toArray(new DocumentReference[0])).get()) {
                    if (documento.exists()) {
                        GameDTO jogo = GameFirestoreMapper.fromSnapshot(documento);
                        if (jogo != null) {
                            encontrados.put(documento.getId(), jogo);
                        }
                    }
                }
                return null;
            });
        }

        List<GameDTO> jogos = new ArrayList<>();
        for (String id : ids) {
            GameDTO jogo = encontrados.get(id);
            if (jogo != null) {
                jogos.add(jogo);
            }
        }
        return jogos;
    }

    private CacheJogos obterCacheJogos() {
        CacheJogos atual = cacheJogos;
        if (atual != null && atual.valido()) {
            return atual;
        }
        synchronized (this) {
            atual = cacheJogos;
            if (atual == null || !atual.valido()) {
                atual = carregarCacheJogos();
                cacheJogos = atual;
            }
            return atual;
        }
    }

    private CacheJogos carregarCacheJogos() {
        return executar(() -> {
            QuerySnapshot resultado = firestore.collection(nomeColecaoJogos).get().get();
            List<GameDTO> jogos = new ArrayList<>();
            Map<String, GameDTO> porId = new HashMap<>();

            for (QueryDocumentSnapshot documento : resultado.getDocuments()) {
                GameDTO jogo = GameFirestoreMapper.fromSnapshot(documento);
                if (jogo != null) {
                    jogos.add(jogo);
                    porId.put(documento.getId(), jogo);
                }
            }
            return new CacheJogos(List.copyOf(jogos), Map.copyOf(porId), Instant.now());
        });
    }

    private void invalidarCacheJogos() {
        cacheJogos = null;
    }

    public List<GameDTO> listarJogosEmDestaque(int quantidade) {
        int quantidadeValida = Math.max(1, Math.min(quantidade, QUANTIDADE_DESTAQUES_MAXIMA));

        return executar(() -> {
            QuerySnapshot resultado = firestore.collection(nomeColecaoJogos)
                    .orderBy("igdbPopularityValue", Query.Direction.DESCENDING)
                    .limit(quantidadeValida)
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

    /**
     * Busca jogos cujo título contenha o termo informado (case-insensitive).
     * Se o título for nulo ou vazio, retorna todos os jogos.
     */
    public List<GameDTO> buscarJogosPorTitulo(String titulo) {
        List<GameDTO> todos = listarJogos();

        String termoBusca = titulo != null ? titulo.trim().toLowerCase() : "";
        if (termoBusca.isEmpty()) {
            return todos;
        }

        List<GameDTO> resultado = new ArrayList<>();
        for (GameDTO jogo : todos) {
            String tituloJogo = jogo.getTitle() != null ? jogo.getTitle().toLowerCase() : "";
            if (tituloJogo.contains(termoBusca)) {
                resultado.add(jogo);
            }
        }
        return resultado;
    }

    /**
     * Retorna a quantidade de jogos correspondentes ao termo de busca.
     * Útil para o app mobile exibir "X jogos encontrados" sem precisar
     * carregar a lista completa no cliente.
     */
    public long contarJogosPorTitulo(String titulo) {
        return buscarJogosPorTitulo(titulo).size();
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
            invalidarCacheJogos();

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
            invalidarCacheJogos();
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
            invalidarCacheJogos();
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

            invalidarCacheJogos();
            return null;
        });
    }
}