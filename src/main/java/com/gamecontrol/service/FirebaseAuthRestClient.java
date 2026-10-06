package com.gamecontrol.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class FirebaseAuthRestClient {

    private static final String URL_LOGIN =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={key}";
    private static final String URL_REFRESH =
            "https://securetoken.googleapis.com/v1/token?key={key}";

    private final RestClient restClient;
    private final String apiKey;

    public FirebaseAuthRestClient(
            RestClient.Builder builder,
            @Value("${firebase.api-key:}") String apiKey
    ) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
    }

    public record SessaoFirebase(String uid, String idToken, String refreshToken, long expiresIn) {
    }

    private record RespostaLogin(String localId, String idToken, String refreshToken, String expiresIn) {
    }

    private record RespostaRefresh(
            @JsonProperty("user_id") String userId,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") String expiresIn
    ) {
    }

    public SessaoFirebase entrarComEmailESenha(String email, String senha) {
        validarApiKey();
        try {
            RespostaLogin resposta = restClient.post()
                    .uri(URL_LOGIN, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", senha, "returnSecureToken", true))
                    .retrieve()
                    .body(RespostaLogin.class);
            return new SessaoFirebase(
                    resposta.localId(),
                    resposta.idToken(),
                    resposta.refreshToken(),
                    Long.parseLong(resposta.expiresIn())
            );
        } catch (RestClientResponseException e) {
            throw traduzirErro(e);
        }
    }

    public SessaoFirebase renovarToken(String refreshToken) {
        validarApiKey();
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "refresh_token");
        formulario.add("refresh_token", refreshToken);
        try {
            RespostaRefresh resposta = restClient.post()
                    .uri(URL_REFRESH, apiKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(RespostaRefresh.class);
            return new SessaoFirebase(
                    resposta.userId(),
                    resposta.idToken(),
                    resposta.refreshToken(),
                    Long.parseLong(resposta.expiresIn())
            );
        } catch (RestClientResponseException e) {
            throw traduzirErro(e);
        }
    }

    private void validarApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Propriedade firebase.api-key não configurada. Crie config/firebase.properties a partir de config/firebase.properties.example.");
        }
    }

    private static ResponseStatusException traduzirErro(RestClientResponseException e) {
        String codigo = extrairCodigoErro(e);
        if (codigo.startsWith("INVALID_LOGIN_CREDENTIALS")
                || codigo.startsWith("EMAIL_NOT_FOUND")
                || codigo.startsWith("INVALID_PASSWORD")
                || codigo.startsWith("INVALID_EMAIL")) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        if (codigo.startsWith("TOKEN_EXPIRED")
                || codigo.startsWith("INVALID_REFRESH_TOKEN")
                || codigo.startsWith("USER_NOT_FOUND")
                || codigo.startsWith("INVALID_GRANT_TYPE")
                || codigo.startsWith("MISSING_REFRESH_TOKEN")) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
        }
        if (codigo.startsWith("USER_DISABLED")) {
            return new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário desativado.");
        }
        if (codigo.startsWith("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            return new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente novamente mais tarde."
            );
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Falha ao comunicar com o Firebase Authentication: " + codigo
        );
    }

    private static String extrairCodigoErro(RestClientResponseException e) {
        try {
            Map<?, ?> corpo = e.getResponseBodyAs(Map.class);
            if (corpo != null && corpo.get("error") instanceof Map<?, ?> erro) {
                Object mensagem = erro.get("message");
                if (mensagem != null) {
                    return String.valueOf(mensagem);
                }
            }
        } catch (RuntimeException ignorada) {
        }
        return e.getStatusText();
    }
}
