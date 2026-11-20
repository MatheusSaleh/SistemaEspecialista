package com.example.sistemaespecialista.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SintomasAceitos {
    private static final List<Opcao> SIM_NAO = List.of(new Opcao("sim", "Sim"), new Opcao("nao", "Não"));
    private static final List<Opcao> ESTADO_LUZ = List.of(new Opcao("acesa", "Acesa"), new Opcao("apagada", "Apagada"));
    private static final List<Campo> CAMPOS = List.of(
            escolha("motor_nao_liga", "O motor fica sem girar na partida?", "partida", SIM_NAO),
            escolha("motor_gira_mas_nao_pega", "O motor gira, mas não pega?", "partida", SIM_NAO),
            escolha("bateria_nova", "A bateria é nova?", "partida", SIM_NAO),
            escolha("terminais_oxidados", "Há oxidação visível nos terminais da bateria?", "partida", SIM_NAO),
            escolha("clique_partida", "Há um único clique ao tentar dar partida?", "partida", SIM_NAO),
            escolha("luzes_enfraquecem", "As luzes ficam fracas ao tentar dar partida?", "partida", SIM_NAO),
            escolha("temperatura_motor", "Temperatura do motor", "arrefecimento", List.of(new Opcao("normal", "Normal"), new Opcao("alta", "Alta"))),
            escolha("luz_alerta", "Luz de alerta de temperatura", "arrefecimento", ESTADO_LUZ),
            escolha("nivel_arrefecimento", "Nível do líquido de arrefecimento", "arrefecimento", List.of(new Opcao("normal", "Normal"), new Opcao("baixo", "Baixo"))),
            escolha("ventoinha_liga", "A ventoinha entra em funcionamento?", "arrefecimento", SIM_NAO),
            escolha("vazamento_arrefecimento", "Há sinais externos de vazamento de líquido?", "arrefecimento", SIM_NAO),
            escolha("temperatura_oscilando", "O indicador de temperatura oscila?", "arrefecimento", SIM_NAO),
            escolha("nivel_combustivel", "Nível de combustível", "injecao", List.of(new Opcao("baixo", "Baixo"), new Opcao("medio", "Normal"), new Opcao("cheio", "Cheio"))),
            escolha("consumo_excessivo_combustivel", "O consumo de combustível aumentou?", "injecao", SIM_NAO),
            escolha("luz_injecao", "Luz da injeção", "injecao", ESTADO_LUZ),
            escolha("resposta_acelerador", "Resposta do acelerador", "injecao", List.of(new Opcao("normal", "Normal"), new Opcao("lenta", "Lenta"))),
            escolha("clima", "Clima", "injecao", List.of(new Opcao("frio", "Frio"), new Opcao("quente", "Quente"), new Opcao("umido", "Úmido"), new Opcao("seco", "Seco"))),
            escolha("tipo_combustivel", "Tipo de combustível", "injecao", List.of(new Opcao("etanol", "Etanol"), new Opcao("gasolina", "Gasolina"))),
            escolha("perde_potencia_carga", "Há perda de potência sob carga?", "injecao", SIM_NAO),
            escolha("marcha_lenta_irregular", "A marcha lenta oscila?", "injecao", SIM_NAO),
            escolha("luz_injecao_piscando", "A luz da injeção pisca?", "injecao", SIM_NAO),
            escolha("freio_mole", "O pedal de freio está mole?", "freios", SIM_NAO),
            escolha("vazamento_fluido", "Há vazamento de fluido de freio?", "freios", SIM_NAO),
            escolha("ruido_frenagem", "Há ruído metálico ao frear?", "freios", SIM_NAO),
            escolha("vibracao_frenagem", "Há vibração no pedal ao frear?", "freios", SIM_NAO),
            escolha("pedal_baixa", "O pedal desce progressivamente ao manter pressão?", "freios", SIM_NAO),
            escolha("ruido_suspensao", "Há ruído na suspensão?", "suspensao", SIM_NAO),
            escolha("veiculo_puxa", "O veículo tende a puxar para um lado?", "suspensao", SIM_NAO),
            escolha("pneu_desgaste_irregular", "Há desgaste irregular nos pneus?", "suspensao", SIM_NAO),
            escolha("veiculo_balanca", "A carroceria balança repetidamente após uma ondulação?", "suspensao", SIM_NAO),
            escolha("direcao_pesada", "A direção ficou mais pesada?", "suspensao", SIM_NAO),
            escolha("ruido_direcao", "Há ruído ao girar o volante?", "suspensao", SIM_NAO),
            new Campo("quilometragem", "Quilometragem", "manutencao", "km", List.of()),
            escolha("luz_oleo", "Luz do óleo", "manutencao", ESTADO_LUZ),
            escolha("ruido_motor", "Ruído do motor", "manutencao", List.of(new Opcao("estranho", "Estranho"), new Opcao("normal", "Normal"))),
            new Campo("ultimo_checkup", "Tempo desde a última revisão", "manutencao", "dias", List.of()),
            escolha("indicador_pneu", "Os pneus chegaram ao indicador de desgaste?", "manutencao", SIM_NAO)
    );
    private static final Map<String, Campo> POR_NOME = CAMPOS.stream()
            .collect(Collectors.toUnmodifiableMap(Campo::nome, Function.identity()));

    private SintomasAceitos() {}

    public static List<Campo> campos() {
        return CAMPOS;
    }

    public static Campo campo(String nome) {
        return POR_NOME.get(nome);
    }

    public static List<Sintoma> validar(List<Sintoma> sintomas) {
        if (sintomas == null || sintomas.size() > CAMPOS.size()) {
            throw new IllegalArgumentException("Envie uma lista com até " + CAMPOS.size() + " sintomas.");
        }

        Set<String> nomes = new HashSet<>();
        List<Sintoma> fatos = new ArrayList<>();
        for (Sintoma sintoma : sintomas) {
            if (sintoma == null || sintoma.getNome() == null || sintoma.getValor() == null) {
                throw new IllegalArgumentException("Cada sintoma deve ter um nome e um valor.");
            }

            String nome = sintoma.getNome().strip();
            Campo campo = campo(nome);
            if (campo == null) {
                throw new IllegalArgumentException("Sintoma desconhecido.");
            }
            if (!nomes.add(nome)) {
                throw new IllegalArgumentException("Sintoma repetido: " + nome);
            }
            String valor = Normalizer.normalize(sintoma.getValor().strip().toLowerCase(Locale.ROOT),
                    Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            if ("nao sei".equals(valor) || "nao_sei".equals(valor)) {
                continue;
            }

            fatos.add(new Sintoma(nome, validarValor(campo, valor)));
        }
        return fatos;
    }

    private static String validarValor(Campo campo, String valor) {
        if (campo.numerico()) {
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= 0) {
                    return Integer.toString(numero);
                }
            } catch (NumberFormatException ignorada) {
                // O valor fora da faixa recebe a mesma mensagem que uma entrada não numérica.
            }
            throw new IllegalArgumentException("Informe um número inteiro não negativo para " + campo.rotulo() + ".");
        }
        if (!campo.aceitaOpcao(valor)) {
            throw new IllegalArgumentException("Valor inválido para " + campo.rotulo() + ".");
        }
        return valor;
    }

    private static Campo escolha(String nome, String rotulo, String categoria, List<Opcao> opcoes) {
        return new Campo(nome, rotulo, categoria, null, opcoes);
    }

    public record Campo(String nome, String rotulo, String categoria, String unidade, List<Opcao> opcoes) {
        public boolean numerico() {
            return unidade != null;
        }

        public boolean aceitaOpcao(String valor) {
            return opcoes.stream().anyMatch(opcao -> opcao.valor().equals(valor));
        }
    }

    public record Opcao(String valor, String rotulo) {}
}
