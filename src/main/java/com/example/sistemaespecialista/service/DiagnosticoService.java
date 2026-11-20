package com.example.sistemaespecialista.service;

import com.example.sistemaespecialista.model.Hipotese;
import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.model.SintomasAceitos;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class DiagnosticoService {
    private final KieBase regras;

    public DiagnosticoService() {
        var recurso = KieServices.Factory.get().getResources()
                .newClassPathResource("regras/diagnostico.drl", UTF_8.name(), DiagnosticoService.class.getClassLoader());
        var compilador = KnowledgeBuilderFactory.newKnowledgeBuilder();
        compilador.add(recurso, ResourceType.DRL);
        if (compilador.hasErrors()) {
            throw new IllegalStateException("Falha ao carregar as regras Drools: " + compilador.getErrors());
        }
        regras = compilador.newKieBase();
    }

    public List<Hipotese> diagnosticar(List<Sintoma> sintomas) {
        List<Sintoma> fatos = SintomasAceitos.validar(sintomas);
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
}
