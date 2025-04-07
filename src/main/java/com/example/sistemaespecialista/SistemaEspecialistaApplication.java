package com.example.sistemaespecialista;

import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.service.DiagnosticoService;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class SistemaEspecialistaApplication {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Sintoma> sintomas = new ArrayList<>();

        System.out.println("=== SISTEMA ESPECIALISTA DE DIAGNÓSTICO AUTOMOTIVO ===");

        while (true) {
            System.out.println("\nDigite o sintoma (ou 'fim' para diagnosticar):");
            System.out.println("Exemplos: motor_nao_liga, motor_gira_mas_nao_pega, nivel_combustivel, ruido_suspensao, quilometragem");
            System.out.print("Sintoma: ");
            String nome = scanner.nextLine().trim();

            if (nome.equalsIgnoreCase("fim")) break;

            System.out.print("Valor: ");
            String valor = scanner.nextLine().trim();

            sintomas.add(new Sintoma(nome, valor));
        }

        KieServices kieServices = KieServices.Factory.get();
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        KieSession kieSession = kieContainer.newKieSession("ksession-rules");

        for (Sintoma s : sintomas) {
            kieSession.insert(s);
        }

        System.out.println("\n=== DIAGNÓSTICO ===");
        kieSession.fireAllRules();
        kieSession.dispose();

        System.out.println("=== FIM DO DIAGNÓSTICO ===");
    }
}
