package com.example.sistemaespecialista;

import com.example.sistemaespecialista.model.Sintoma;
import com.example.sistemaespecialista.service.DiagnosticoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class SistemaEspecialistaApplication {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--cli")) {
            executarTerminal();
            return;
        }

        SpringApplication.run(SistemaEspecialistaApplication.class, args);
    }

    private static void executarTerminal() {
        List<Sintoma> sintomas = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== SIMULADOR DE DIAGNÓSTICO AUTOMOTIVO ===");
        System.out.println("Exemplos: motor_gira_mas_nao_pega (sim/nao), nivel_combustivel (baixo/medio/cheio).");
        System.out.println("Digite 'fim' para rodar a simulação.");

        while (true) {
            System.out.print("Sintoma: ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String nome = scanner.nextLine().strip();
            if ("fim".equalsIgnoreCase(nome) || !scanner.hasNextLine()) {
                break;
            }

            System.out.print("Valor: ");
            sintomas.add(new Sintoma(nome, scanner.nextLine().strip()));
        }

        try {
            var hipoteses = new DiagnosticoService().diagnosticar(sintomas);
            if (hipoteses.isEmpty()) {
                System.out.println("Nenhuma regra corresponde às respostas.");
            }
            hipoteses.forEach(hipotese -> System.out.println(hipotese.titulo() + " [" + hipotese.regra() + "]"));
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
        }
    }
}
