package com.example.sistemaespecialista;

import com.jayway.jsonpath.JsonPath;
import com.example.sistemaespecialista.model.Hipotese;
import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.model.SintomasAceitos;
import com.example.sistemaespecialista.service.DiagnosticoService;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SistemaEspecialistaApplicationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DiagnosticoService service;

    @Test
    void publicaAs28RegrasNasSeisAreas() throws Exception {
        mvc.perform(get("/api/regras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regras.length()").value(28))
                .andExpect(jsonPath("$.categorias.length()").value(6))
                .andExpect(jsonPath("$.regras[?(@.categoria == 'partida')]", hasSize(6)))
                .andExpect(jsonPath("$.regras[?(@.categoria == 'arrefecimento')]", hasSize(5)))
                .andExpect(jsonPath("$.regras[?(@.categoria == 'injecao')]", hasSize(5)))
                .andExpect(jsonPath("$.regras[?(@.categoria == 'freios')]", hasSize(4)))
                .andExpect(jsonPath("$.regras[?(@.categoria == 'suspensao')]", hasSize(4)))
                .andExpect(jsonPath("$.regras[?(@.categoria == 'manutencao')]", hasSize(4)));
    }

    @TestFactory
    Stream<DynamicTest> exemplosECondicoesDoCatalogoCorrespondemAoDrools() {
        return service.catalogo().regras().stream().map(regra -> DynamicTest.dynamicTest(regra.nome(), () -> {
            assertThat(diagnosticar(regra.exemplo()))
                    .filteredOn(hipotese -> hipotese.id().equals(regra.id()))
                    .singleElement().satisfies(hipotese -> {
                        assertThat(hipotese.regra()).isEqualTo(regra.nome());
                        assertThat(hipotese.evidencias()).containsExactlyElementsOf(regra.condicoes().stream()
                                .map(condicao -> new Hipotese.Evidencia(condicao.campo(), regra.exemplo().get(condicao.campo())))
                                .toList());
                    });

            for (var condicao : regra.condicoes()) {
                var entrada = new HashMap<>(regra.exemplo());
                entrada.remove(condicao.campo());
                assertThat(diagnosticar(entrada)).extracting(Hipotese::id).doesNotContain(regra.id());
                if (condicao.maiorQue() != null) {
                    entrada.put(condicao.campo(), condicao.maiorQue().toString());
                    assertThat(diagnosticar(entrada)).extracting(Hipotese::id).doesNotContain(regra.id());
                } else {
                    for (var opcao : SintomasAceitos.campo(condicao.campo()).opcoes()) {
                        entrada.put(condicao.campo(), opcao.valor());
                        var ids = assertThat(diagnosticar(entrada)).extracting(Hipotese::id);
                        if (condicao.aceita(opcao.valor())) {
                            ids.contains(regra.id());
                        } else {
                            ids.doesNotContain(regra.id());
                        }
                    }
                }
            }
        }));
    }

    @Test
    void respostaDeAcompanhamentoDiferenciaQuedaDeTensaoDeProblemaNaPartida() throws Exception {
        String sintomas = """
                [
                  {"nome":"motor_nao_liga","valor":"sim"},
                  {"nome":"clique_partida","valor":"sim"},
                  {"nome":"bateria_nova","valor":"nao"},
                  {"nome":"terminais_oxidados","valor":"nao"},
                  {"nome":"luzes_enfraquecem","valor":"%s"}
                ]
                """;

        simular(sintomas.formatted("nao_sei"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas[0].campo").value("luzes_enfraquecem"))
                .andExpect(jsonPath("$.perguntas[0].regrasPossiveis.length()").value(2))
                .andExpect(jsonPath("$.hipoteses[*].id").value(not(hasItem("motor_partida"))));

        simular(sintomas.formatted("nao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas").isEmpty())
                .andExpect(jsonPath("$.hipoteses[*].id").value(hasItem("motor_partida")))
                .andExpect(jsonPath("$.hipoteses[*].id").value(not(hasItem("queda_tensao"))));

        simular(sintomas.formatted("sim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas").isEmpty())
                .andExpect(jsonPath("$.hipoteses[*].id").value(hasItem("queda_tensao")))
                .andExpect(jsonPath("$.hipoteses[*].id").value(not(hasItem("motor_partida"))));
    }

    @Test
    void mudarNivelDeCombustivelRemoveApenasAHipoteseDeCombustivel() throws Exception {
        var sintomas = """
                [
                  {"nome":"motor_gira_mas_nao_pega","valor":"sim"},
                  {"nome":"nivel_combustivel","valor":"%s"}
                ]
                """;

        simular(sintomas.formatted("baixo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hipoteses.length()").value(2))
                .andExpect(jsonPath("$.hipoteses[0].id").value("combustivel"))
                .andExpect(textoJson("$.hipoteses[0].regra", "Sem combustível"))
                .andExpect(jsonPath("$.hipoteses[0].evidencias[1].sintoma").value("nivel_combustivel"))
                .andExpect(jsonPath("$.hipoteses[0].evidencias[1].valor").value("baixo"))
                .andExpect(jsonPath("$.hipoteses[1].id").value("ignicao"));

        simular(sintomas.formatted("medio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hipoteses.length()").value(1))
                .andExpect(jsonPath("$.hipoteses[0].id").value("ignicao"));
    }

    @Test
    void respostasNegativasEDesconhecidasNaoDisparamRegras() throws Exception {
        simular("""
                [
                  {"nome":"motor_nao_liga","valor":"não"},
                  {"nome":"motor_gira_mas_nao_pega","valor":"não sei"},
                  {"nome":"nivel_combustivel","valor":"baixo"}
                ]
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hipoteses").isEmpty());
    }

    @Test
    void ruidoNaSuspensaoExigeQuilometragemAcimaDoLimite() throws Exception {
        var sintomas = """
                [
                  {"nome":"ruido_suspensao","valor":"sim"},
                  {"nome":"quilometragem","valor":"%s"}
                ]
                """;

        simular(sintomas.formatted("100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hipoteses").isEmpty());

        simular(sintomas.formatted("100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hipoteses[0].id").value("suspensao"));

        simular(sintomas.formatted("abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isString());
    }

    @Test
    void explicaAsCondicoesDaRegraDeInjecao() throws Exception {
        simular("""
                [
                  {"nome":"consumo_excessivo_combustivel","valor":"sim"},
                  {"nome":"luz_injecao","valor":"acesa"},
                  {"nome":"quilometragem","valor":"85000"},
                  {"nome":"resposta_acelerador","valor":"lenta"},
                  {"nome":"ultimo_checkup","valor":"200"},
                  {"nome":"clima","valor":"Úmido"},
                  {"nome":"tipo_combustivel","valor":"etanol"}
                ]
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hipoteses[0].id").value("injecao"))
                .andExpect(textoJson("$.hipoteses[0].titulo", "Possível dificuldade de combustão a frio"))
                .andExpect(jsonPath("$.hipoteses[0].evidencias.length()").value(7));
    }

    private ResultMatcher textoJson(String caminho, String esperado) {
        return resultado -> {
            String resposta = new String(resultado.getResponse().getContentAsByteArray(), UTF_8);
            String texto = JsonPath.read(resposta, caminho);
            assertEquals(esperado, texto);
        };
    }

    private List<Hipotese> diagnosticar(Map<String, String> respostas) {
        return service.diagnosticar(respostas.entrySet().stream()
                .map(entrada -> new Sintoma(entrada.getKey(), entrada.getValue()))
                .toList());
    }

    private ResultActions simular(String sintomas) throws Exception {
        return mvc.perform(post("/api/diagnosticos")
                .contentType(APPLICATION_JSON)
                .content("{\"sintomas\":" + sintomas + "}"));
    }
}
