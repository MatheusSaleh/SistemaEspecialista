package com.example.sistemaespecialista.model;

import java.util.Arrays;
import java.util.List;

public record Hipotese(String id, String titulo, String regra, List<Evidencia> evidencias) {
    public record Evidencia(String sintoma, String valor) {}

    public static Hipotese de(String id, String titulo, String regra, Sintoma... sintomas) {
        return new Hipotese(id, titulo, regra, Arrays.stream(sintomas)
                .map(sintoma -> new Evidencia(sintoma.getNome(), sintoma.getValor()))
                .toList());
    }
}
