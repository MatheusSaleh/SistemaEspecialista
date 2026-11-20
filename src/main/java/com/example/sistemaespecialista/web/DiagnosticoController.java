package com.example.sistemaespecialista.web;

import com.example.sistemaespecialista.model.CatalogoRegras;
import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.service.DiagnosticoService;
import com.example.sistemaespecialista.service.DiagnosticoService.Simulacao;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class DiagnosticoController {
    private static final int LIMITE_CORPO = 16 * 1024;
    private final DiagnosticoService service;
    private final ObjectReader leitor;

    public DiagnosticoController(DiagnosticoService service, JsonMapper json) {
        this.service = service;
        leitor = json.readerFor(Solicitacao.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    @GetMapping(path = "/api/regras", produces = APPLICATION_JSON_VALUE)
    public CatalogoRegras listarRegras() {
        return service.catalogo();
    }

    @PostMapping(path = "/api/diagnosticos", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public Simulacao diagnosticar(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > LIMITE_CORPO) {
            throw corpoMuitoGrande();
        }

        byte[] corpo = request.getInputStream().readNBytes(LIMITE_CORPO + 1);
        if (corpo.length > LIMITE_CORPO) {
            throw corpoMuitoGrande();
        }

        Solicitacao solicitacao;
        try {
            solicitacao = leitor.readValue(corpo);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Envie um JSON válido com a lista de sintomas.");
        }
        if (solicitacao == null || solicitacao.sintomas() == null) {
            throw new IllegalArgumentException("Envie uma lista de sintomas.");
        }
        List<Sintoma> sintomas = solicitacao.sintomas().stream()
                .map(sintoma -> sintoma == null ? null : new Sintoma(sintoma.nome(), sintoma.valor()))
                .toList();
        return service.simular(sintomas);
    }

    private ResponseStatusException corpoMuitoGrande() {
        return new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro entradaInvalida(IllegalArgumentException exception) {
        return new Erro(exception.getMessage());
    }

    public record Solicitacao(List<SintomaEntrada> sintomas) {}
    public record SintomaEntrada(String nome, String valor) {}
    public record Erro(String mensagem) {}
}
