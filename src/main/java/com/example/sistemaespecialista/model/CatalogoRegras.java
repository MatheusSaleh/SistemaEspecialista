package com.example.sistemaespecialista.model;

import com.example.sistemaespecialista.model.SintomasAceitos.Campo;
import org.kie.api.definition.rule.Rule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record CatalogoRegras(List<Categoria> categorias, List<Campo> campos, List<Regra> regras) {
    private static final List<Categoria> CATEGORIAS = List.of(
            new Categoria("partida", "Partida e sistema elétrico"),
            new Categoria("arrefecimento", "Arrefecimento"),
            new Categoria("injecao", "Injeção e combustível"),
            new Categoria("freios", "Freios"),
            new Categoria("suspensao", "Suspensão e direção"),
            new Categoria("manutencao", "Manutenção preventiva")
    );
    private static final Map<String, Integer> ORDEM = IntStream.range(0, CATEGORIAS.size()).boxed()
            .collect(Collectors.toUnmodifiableMap(i -> CATEGORIAS.get(i).id(), i -> i));

    public static CatalogoRegras de(List<Rule> regrasDrools) {
        List<Regra> regras = regrasDrools.stream()
                .map(Regra::de)
                .sorted(Comparator.comparingInt((Regra regra) -> ORDEM.get(regra.categoria()))
                        .thenComparing(Regra::nome))
                .toList();
        if (regras.stream().map(Regra::id).distinct().count() != regras.size()) {
            throw new IllegalStateException("Há identificadores duplicados no catálogo de regras.");
        }
        return new CatalogoRegras(CATEGORIAS, SintomasAceitos.campos(), regras);
    }

    public record Categoria(String id, String nome) {}

    public record Regra(String id, String nome, String titulo, String categoria,
                        List<Condicao> condicoes, Map<String, String> exemplo) {
        private static Regra de(Rule regra) {
            String id = metadado(regra, "id");
            String categoria = metadado(regra, "categoria");
            if (!ORDEM.containsKey(categoria)) {
                throw new IllegalStateException("Categoria desconhecida na regra " + regra.getName());
            }
            List<Condicao> condicoes = Arrays.stream(metadado(regra, "quando").split(";"))
                    .map(Condicao::de)
                    .toList();
            Map<String, String> exemplo = new LinkedHashMap<>();
            for (Condicao condicao : condicoes) {
                if (exemplo.put(condicao.campo(), condicao.valorExemplo()) != null) {
                    throw new IllegalStateException("Campo repetido nas condições de " + regra.getName());
                }
            }
            return new Regra(id, regra.getName(), metadado(regra, "titulo"), categoria,
                    condicoes, Map.copyOf(exemplo));
        }

        private static String metadado(Rule regra, String chave) {
            if (regra.getMetaData().get(chave) instanceof String texto && !texto.isBlank()) {
                return texto;
            }
            throw new IllegalStateException("Metadado " + chave + " ausente na regra " + regra.getName());
        }
    }

    public record Condicao(String campo, List<String> valores, Integer maiorQue) {
        private static Condicao de(String expressao) {
            String[] partes = expressao.split("[=>]", 2);
            Campo campo = SintomasAceitos.campo(partes[0]);
            if (partes.length != 2 || campo == null) {
                throw new IllegalStateException("Condição inválida no catálogo: " + expressao);
            }
            if (campo.numerico() && expressao.contains(">")) {
                int limite = Integer.parseInt(partes[1]);
                if (limite >= 0 && limite < Integer.MAX_VALUE) {
                    return new Condicao(campo.nome(), List.of(), limite);
                }
            } else if (!campo.numerico() && expressao.contains("=")) {
                List<String> valores = List.of(partes[1].split("/"));
                if (valores.stream().allMatch(campo::aceitaOpcao)) {
                    return new Condicao(campo.nome(), valores, null);
                }
            }
            throw new IllegalStateException("Condição inválida no catálogo: " + expressao);
        }

        public boolean aceita(String valor) {
            return maiorQue == null ? valores.contains(valor) : Integer.parseInt(valor) > maiorQue;
        }

        private String valorExemplo() {
            return maiorQue == null ? valores.get(0) : Integer.toString(maiorQue + 1);
        }
    }
}
