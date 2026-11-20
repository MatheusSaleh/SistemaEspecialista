package com.example.sistemaespecialista.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SintomasAceitos {
    private static final Set<String> BOOLEANOS = Set.of("sim", "nao");
    private static final Set<String> LUZ = Set.of("acesa", "apagada");
    private static final Set<String> NUMERICOS = Set.of("quilometragem", "ultimo_checkup");
    private static final Map<String, Set<String>> OPCOES = Map.ofEntries(
            Map.entry("motor_nao_liga", BOOLEANOS),
            Map.entry("motor_gira_mas_nao_pega", BOOLEANOS),
            Map.entry("nivel_combustivel", Set.of("baixo", "medio", "cheio")),
            Map.entry("ruido_suspensao", BOOLEANOS),
            Map.entry("bateria_nova", BOOLEANOS),
            Map.entry("luz_oleo", LUZ),
            Map.entry("freio_mole", BOOLEANOS),
            Map.entry("vazamento_fluido", BOOLEANOS),
            Map.entry("temperatura_motor", Set.of("alta", "normal")),
            Map.entry("luz_alerta", LUZ),
            Map.entry("ruido_motor", Set.of("estranho", "normal")),
            Map.entry("consumo_excessivo_combustivel", BOOLEANOS),
            Map.entry("luz_injecao", LUZ),
            Map.entry("resposta_acelerador", Set.of("lenta", "normal")),
            Map.entry("clima", Set.of("frio", "quente", "umido", "seco")),
            Map.entry("tipo_combustivel", Set.of("etanol", "gasolina"))
    );

    private SintomasAceitos() {}

    public static List<Sintoma> validar(List<Sintoma> sintomas) {
        if (sintomas == null || sintomas.size() > OPCOES.size() + NUMERICOS.size()) {
            throw new IllegalArgumentException("Envie uma lista com até 18 sintomas.");
        }

        Set<String> nomes = new HashSet<>();
        List<Sintoma> fatos = new ArrayList<>();
        for (Sintoma sintoma : sintomas) {
            if (sintoma == null || sintoma.getNome() == null || sintoma.getValor() == null) {
                throw new IllegalArgumentException("Cada sintoma deve ter um nome e um valor.");
            }

            String nome = sintoma.getNome().strip();
            String valor = Normalizer.normalize(sintoma.getValor().strip().toLowerCase(Locale.ROOT),
                    Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            if (!OPCOES.containsKey(nome) && !NUMERICOS.contains(nome)) {
                throw new IllegalArgumentException("Sintoma desconhecido.");
            }
            if (!nomes.add(nome)) {
                throw new IllegalArgumentException("Sintoma repetido: " + nome);
            }
            if ("nao sei".equals(valor) || "nao_sei".equals(valor)) {
                continue;
            }

            if (NUMERICOS.contains(nome)) {
                try {
                    int numero = Integer.parseInt(valor);
                    if (numero < 0) {
                        throw new NumberFormatException();
                    }
                    valor = Integer.toString(numero);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Informe um número inteiro não negativo para " + nome + ".");
                }
            } else if (!OPCOES.get(nome).contains(valor)) {
                throw new IllegalArgumentException("Valor inválido para " + nome + ".");
            }

            fatos.add(new Sintoma(nome, valor));
        }
        return fatos;
    }
}
