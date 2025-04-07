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
        System.out.println("Este sistema simula um mecânico virtual que avalia sintomas do veículo e fornece diagnósticos automáticos.");
        System.out.println("\n⚙️ Como usar:");
        System.out.println("Você será solicitado a informar sintomas. Para sintomas booleanos, responda com 'sim' ou 'nao'.");
        System.out.println("Para sintomas com valor numérico, digite o número inteiro. Para sintomas de texto, siga as opções listadas.");
        System.out.println("Digite 'fim' para encerrar a entrada de sintomas e receber o diagnóstico.\n");

        System.out.println("📋 Sintomas reconhecidos:");
        System.out.println("- motor_nao_liga (sim)");
        System.out.println("- motor_gira_mas_nao_pega (sim)");
        System.out.println("- nivel_combustivel (baixo/medio/cheio)");
        System.out.println("- ruido_suspensao (sim)");
        System.out.println("- quilometragem (ex: 70000, 120000)");
        System.out.println("- bateria_nova (sim/nao)");
        System.out.println("- luz_oleo (acesa/apagada)");
        System.out.println("- freio_mole (sim)");
        System.out.println("- vazamento_fluido (sim/nao)");
        System.out.println("- temperatura_motor (alta/normal)");
        System.out.println("- luz_alerta (acesa/apagada)");
        System.out.println("- ruido_motor (estranho/normal)");
        System.out.println("- consumo_excessivo_combustivel (sim/nao)");
        System.out.println("- luz_injecao (acesa/apagada)");
        System.out.println("- resposta_acelerador (lenta/normal)");
        System.out.println("- ultimo_checkup (dias desde o último, ex: 200)");
        System.out.println("- clima (frio/quente/úmido/seco)");
        System.out.println("- tipo_combustivel (etanol/gasolina)");

        System.out.println("\n🧾 Diagnósticos possíveis:");
        System.out.println("- Verificar bateria.");
        System.out.println("- Possível falha no alternador – bateria nova não sustenta carga.");
        System.out.println("- Problema na ignição.");
        System.out.println("- Sem combustível.");
        System.out.println("- Revisar suspensão – desgaste por quilometragem.");
        System.out.println("- Troca de óleo urgente.");
        System.out.println("- Falha no sistema de freios – verificar fluido.");
        System.out.println("- Superaquecimento do motor – verifique sistema de arrefecimento.");
        System.out.println("- Possível desgaste da correia dentada.");
        System.out.println("- Dificuldade de combustão a frio – verificar sistema de partida e velas.");
        System.out.println("- Ambiente frio/úmido pode afetar sensores – verificar MAP/MAF e conectores.");
        System.out.println("- Falha avançada na injeção – verificar bicos, sonda lambda e ECU.");
        System.out.println("- Falha sistêmica na injeção – revisão completa recomendada.");

        System.out.println("\n✅ Comece informando os sintomas abaixo:");



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
