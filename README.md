# AutoLab

Simulador educacional de diagnóstico automotivo. Altere os sintomas de um cenário e veja quais regras do Drools geram cada hipótese.

**[Testar a demo local](#rodar-localmente)** · [Publicar com Render](#publicar)

![Demonstração do AutoLab: combustível baixo aciona duas hipóteses. Com nível normal, apenas a hipótese de ignição permanece.](docs/demo.gif)

## Experimente em menos de um minuto

1. Abra **Experimentar caso pronto**. O motor gira, mas não pega, e o nível de combustível está baixo.
2. Expanda **Por que apareceu?** para conferir a regra e as evidências.
3. Mude o nível de combustível para **Normal** e clique em **Simular diagnóstico**. A hipótese de combustível sai do resultado e a de ignição permanece.

## Do terminal à web

```text
React + TypeScript → POST /api/diagnosticos → Spring Boot → Drools
                      ← hipóteses, regras e evidências ←
```

O projeto começou como um aplicativo de terminal. Agora, as [regras Drools](src/main/resources/regras/diagnostico.drl) geram objetos estruturados com as evidências que receberam. O [serviço Java](src/main/java/com/example/sistemaespecialista/service/DiagnosticoService.java) executa as regras em uma sessão por chamada. A interface exibe a explicação e destaca hipóteses removidas após uma mudança nos sintomas.

Os [testes de integração](src/test/java/com/example/sistemaespecialista/SistemaEspecialistaApplicationTests.java) cobrem a comparação do caso principal, respostas negativas e desconhecidas, limites de quilometragem e as evidências da regra de injeção.

## Rodar localmente

Com Docker:

```sh
docker build -t autolab .
docker run --rm -p 8080:8080 autolab
```

Abra [localhost:8080](http://localhost:8080/). React e a API são servidos pela mesma aplicação.

Para desenvolver com Java 17 e Node.js 22, execute `./mvnw spring-boot:run` na raiz e, em outro terminal, execute `npm ci` e `npm run dev` dentro de `frontend/`. Abra [localhost:5173](http://localhost:5173/). No PowerShell, use `.\mvnw.cmd spring-boot:run`. Para usar o modo terminal, execute o WAR com `--cli`.

## Publicar

O [Dockerfile](Dockerfile) empacota a interface e a API em uma única imagem. O [Blueprint do Render](render.yaml) configura o serviço web. A publicação requer acesso a uma conta do Render. Os visitantes podem usar a aplicação sem cadastro.

[![Publicar no Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/MatheusSaleh/SistemaEspecialista/tree/portfolio/autolab)
