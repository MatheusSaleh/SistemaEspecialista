package com.example.sistemaespecialista.web;

import com.example.sistemaespecialista.model.CatalogoRegras;
import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.service.DiagnosticoService;
import com.example.sistemaespecialista.service.DiagnosticoService.Simulacao;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/api/regras")
    public CatalogoRegras listarRegras() {
        return service.catalogo();
    }

    @PostMapping("/api/diagnosticos")
    public Simulacao diagnosticar(@RequestBody Solicitacao solicitacao) {
        return service.simular(solicitacao.sintomas());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro entradaInvalida(IllegalArgumentException exception) {
        return new Erro(exception.getMessage());
    }

    public record Solicitacao(List<Sintoma> sintomas) {}
    public record Erro(String mensagem) {}
}
