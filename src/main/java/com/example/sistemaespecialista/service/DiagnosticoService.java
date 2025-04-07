package com.example.sistemaespecialista.service;

import com.example.sistemaespecialista.model.Sintoma;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticoService {

    private final KieContainer kieContainer;

    public DiagnosticoService() {
        KieServices kieServices = KieServices.Factory.get();
        this.kieContainer = kieServices.getKieClasspathContainer();
    }

    public void diagnosticar(Sintoma... sintomas){
        KieSession kieSession = kieContainer.newKieSession("ksession-rules");
        for (Sintoma s : sintomas) {
            kieSession.insert(s);
        }
        kieSession.fireAllRules();
        kieSession.dispose();
    }
}
