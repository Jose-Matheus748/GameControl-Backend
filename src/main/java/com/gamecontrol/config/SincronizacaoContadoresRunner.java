package com.gamecontrol.config;

import com.gamecontrol.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SincronizacaoContadoresRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoContadoresRunner.class);

    private final UserService userService;

    public SincronizacaoContadoresRunner(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int atualizados = userService.sincronizarContadoresDeSeguidores();
            log.info("Contador de seguidores sincronizado em {} usuario(s).", atualizados);
        } catch (RuntimeException e) {
            log.warn("Nao foi possivel sincronizar o contador de seguidores.", e);
        }
    }
}
