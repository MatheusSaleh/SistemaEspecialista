package com.example.sistemaespecialista.service;

import com.example.sistemaespecialista.model.CatalogoRegras;
import com.example.sistemaespecialista.model.CatalogoRegras.Condicao;
import com.example.sistemaespecialista.model.Hipotese;
import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.model.SintomasAceitos;
import org.drools.model.codegen.ExecutableModelProject;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class DiagnosticoService {
    private final KieBase regras;
    private final CatalogoRegras catalogo;

    public DiagnosticoService() {
        var recurso = KieServices.Factory.get().getResources()
                .newClassPathResource("regras/diagnostico.drl", UTF_8.name(), DiagnosticoService.class.getClassLoader());
        regras = new KieHelper()
                .setClassLoader(DiagnosticoService.class.getClassLoader())
                .addResource(recurso, ResourceType.DRL)
                .build(ExecutableModelProject.class);
        catalogo = CatalogoRegras.de(regras.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(java.util.Collection::stream)
                .toList());
    }

    public CatalogoRegras catalogo() {
        return catalogo;
    }

    public List<Hipotese> diagnosticar(List<Sintoma> sintomas) {
        return executarRegras(SintomasAceitos.validar(sintomas));
    }

    public Simulacao simular(List<Sintoma> sintomas) {
        List<Sintoma> fatos = SintomasAceitos.validar(sintomas);
        return new Simulacao(executarRegras(fatos), sugerirPerguntas(fatos));
    }

    private List<Hipotese> executarRegras(List<Sintoma> fatos) {
        KieSession sessao = regras.newKieSession();
        try {
            fatos.forEach(sessao::insert);
            sessao.fireAllRules();
            return sessao.getObjects(Hipotese.class::isInstance).stream()
                    .map(Hipotese.class::cast)
                    .sorted(Comparator.comparing(Hipotese::id))
                    .toList();
        } finally {
            sessao.dispose();
        }
    }

    private List<Pergunta> sugerirPerguntas(List<Sintoma> fatos) {
        Map<String, String> respostas = fatos.stream()
                .collect(Collectors.toMap(Sintoma::getNome, Sintoma::getValor));
        Map<String, Candidata> candidatas = new HashMap<>();
        for (var regra : catalogo.regras()) {
            if (regra.condicoes().size() < 2 || contradizRespostas(regra.condicoes(), respostas)) {
                continue;
            }

            var faltantes = regra.condicoes().stream()
                    .map(Condicao::campo)
                    .filter(campo -> !respostas.containsKey(campo))
                    .toList();
            if (faltantes.size() == 1) {
                var candidata = candidatas.computeIfAbsent(faltantes.get(0), campo -> new Candidata());
                candidata.regras.add(regra.nome());
                candidata.evidencias = Math.max(candidata.evidencias, regra.condicoes().size() - 1);
            }
        }

        return candidatas.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Candidata>>comparingInt(item -> item.getValue().evidencias)
                        .reversed().thenComparing(Map.Entry::getKey))
                .limit(3)
                .map(item -> new Pergunta(item.getKey(), List.copyOf(item.getValue().regras)))
                .toList();
    }

    private boolean contradizRespostas(List<Condicao> condicoes, Map<String, String> respostas) {
        return condicoes.stream().anyMatch(condicao ->
                respostas.containsKey(condicao.campo()) && !condicao.aceita(respostas.get(condicao.campo())));
    }

    private static class Candidata {
        private final List<String> regras = new ArrayList<>();
        private int evidencias;
    }

    public record Pergunta(String campo, List<String> regrasPossiveis) {}
    public record Simulacao(List<Hipotese> hipoteses, List<Pergunta> perguntas) {}
}
