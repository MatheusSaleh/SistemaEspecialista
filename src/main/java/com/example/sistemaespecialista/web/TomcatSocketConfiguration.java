package com.example.sistemaespecialista.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class TomcatSocketConfiguration {
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> autolabSocket(
            @Value("${autolab.socket.path:}") String socket) {
        return factory -> {
            if (socket.isBlank()) {
                return;
            }
            if (!Path.of(socket).isAbsolute()) {
                throw new IllegalStateException("AUTOLAB_SOCKET_PATH deve ser um caminho absoluto.");
            }
            factory.addConnectorCustomizers(connector -> {
                if (!connector.setProperty("unixDomainSocketPath", socket)
                        || !connector.setProperty("unixDomainSocketPathPermissions", "rw-rw----")
                        || !connector.setProperty("unixDomainSocketParentPermissions", "rwxr-x---")) {
                    throw new IllegalStateException("O conector HTTP não oferece suporte ao socket Unix.");
                }
            });
        };
    }
}
