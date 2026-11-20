package com.example.sistemaespecialista;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SistemaEspecialistaApplicationTests {
    @Autowired
    private MockMvc mvc;

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

    private ResultActions simular(String sintomas) throws Exception {
        return mvc.perform(post("/api/diagnosticos")
                .contentType(APPLICATION_JSON)
                .content("{\"sintomas\":" + sintomas + "}"));
    }
}
