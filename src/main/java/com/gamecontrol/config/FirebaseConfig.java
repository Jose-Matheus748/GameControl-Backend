package com.gamecontrol.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    @Bean
    public Firestore firestore(
            @Value("${firebase.project-id:}") String projectId
    ) throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {

            InputStream in = null;

            // procura na pasta config da raiz do projeto
            Path externalPath = Paths.get("config", "firebaseKey.json");

            if (Files.exists(externalPath)) {
                in = Files.newInputStream(externalPath);
            } else {
                // fallback para resources
                in = getClass()
                        .getClassLoader()
                        .getResourceAsStream("config/firebaseKey.json");
            }

            if (in == null) {
                throw new IOException(
                        "Arquivo da chave do Firebase não encontrado nem em /config nem em resources."
                );
            }

            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in));

            if (StringUtils.hasText(projectId)) {
                builder.setProjectId(projectId.trim());
            }

            FirebaseApp.initializeApp(builder.build());
        }

        return FirestoreClient.getFirestore();
    }
}