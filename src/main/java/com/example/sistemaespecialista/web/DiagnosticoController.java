package com.example.sistemaespecialista.web;

import com.example.sistemaespecialista.model.Hipotese;
import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.service.DiagnosticoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DiagnosticoController {
    private final DiagnosticoService service;

    public DiagnosticoController(DiagnosticoService service) {
        this.service = service;
    }

    @PostMapping("/api/diagnosticos")
    public Resultado diagnosticar(@RequestBody Solicitacao solicitacao) {
        return new Resultado(service.diagnosticar(solicitacao.sintomas()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro entradaInvalida(IllegalArgumentException exception) {
        return new Erro(exception.getMessage());
    }

    public record Solicitacao(List<Sintoma> sintomas) {}
    public record Resultado(List<Hipotese> hipoteses) {}
    public record Erro(String mensagem) {}
}
