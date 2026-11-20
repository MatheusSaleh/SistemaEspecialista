"""Gera o PDF desta auditoria. Requer Python 3.10+ e ReportLab.

Instale reportlab em um ambiente virtual, depois execute:
    python docs/security-audit/gerar_relatorio.py
"""

from collections import Counter
from html import escape
from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.pdfgen.canvas import Canvas
from reportlab.platypus import Paragraph


ARQUIVO = Path(__file__).with_name("relatorio-auditoria-seguranca.pdf")
TITULO = "Relatório de Auditoria de Segurança — AutoLab"
REVISAO = "b8f45f83d87369060db521161a98204a4c5bbcc5"
DATA = "20 de novembro de 2026"
TOTAL_PAGINAS = 5
ACHADOS = ()
SEVERIDADES = {
    "Crítica": "#B91C1C",
    "Alta": "#EA580C",
    "Média": "#D97706",
    "Baixa": "#2563EB",
    "Informativa": "#64748B",
}
CATEGORIAS = (
    ("1. Isolamento de dono ou inquilino", "Não aplicável"),
    ("2. Autorização de operações privilegiadas", "Não aplicável"),
    ("3. Acesso indevido a objetos por ID (IDOR)", "Não aplicável"),
    ("4. Segredos embutidos", "Verificada"),
    ("5. XSS", "Verificada"),
)

MARINHO = "#14233B"
TEXTO = "#23344B"
SUAVE = "#475C73"
GRADE = "#D7E2EB"
FUNDO = "#F3F7FA"
FORTE = "#059669"
VERDE_ESCURO = "#06604A"
VERDE_FUNDO = "#E8F6F0"
LARGURA, ALTURA = A4
MARGEM = 2 * cm
AREA = LARGURA - 2 * MARGEM


def tinta(cor):
    return HexColor(cor)


def texto(c, x, y, valor, tamanho=10, cor=TEXTO, fonte="Helvetica"):
    c.setFont(fonte, tamanho)
    c.setFillColor(tinta(cor))
    c.drawString(x, ALTURA - y, valor)


def alinhado_direita(c, x, y, valor, tamanho=9, cor=SUAVE, fonte="Helvetica"):
    c.setFont(fonte, tamanho)
    c.setFillColor(tinta(cor))
    c.drawRightString(x, ALTURA - y, valor)


def linha(c, x1, x2, y, cor=GRADE, espessura=0.7):
    c.setStrokeColor(tinta(cor))
    c.setLineWidth(espessura)
    c.line(x1, ALTURA - y, x2, ALTURA - y)


def caixa(c, x, y, largura, altura, cor=FUNDO, raio=8):
    c.setFillColor(tinta(cor))
    c.roundRect(x, ALTURA - y - altura, largura, altura, raio, fill=1, stroke=0)


def paragrafo(c, x, y, largura, valor, tamanho=10.6, entrelinha=14.8,
              cor=TEXTO, fonte="Helvetica"):
    estilo = ParagraphStyle(
        "audit", fontName=fonte, fontSize=tamanho, leading=entrelinha,
        textColor=tinta(cor), splitLongWords=True,
    )
    p = Paragraph(valor, estilo)
    _, altura = p.wrap(largura, 1000)
    p.drawOn(c, x, ALTURA - y - altura)
    return y + altura


def cabecalho(c, pagina, secao):
    texto(c, MARGEM, 35, "AUTOLAB  /  AUDITORIA DE SEGURANÇA", 8.5, MARINHO, "Helvetica-Bold")
    alinhado_direita(c, LARGURA - MARGEM, 35, secao, 8.5)
    linha(c, MARGEM, LARGURA - MARGEM, 47)
    linha(c, MARGEM, LARGURA - MARGEM, ALTURA - 41)
    texto(c, MARGEM, ALTURA - 25, TITULO, 8, SUAVE)
    alinhado_direita(c, LARGURA - MARGEM, ALTURA - 25, f"{pagina} / {TOTAL_PAGINAS}", 8)


def secao(c, y, titulo, subtitulo=None):
    texto(c, MARGEM, y, titulo, 24, MARINHO, "Helvetica-Bold")
    if subtitulo:
        return paragrafo(c, MARGEM, y + 10, AREA, subtitulo, cor=SUAVE)
    return y


def tabela(c, y, colunas, linhas):
    c.setFillColor(tinta(MARINHO))
    c.rect(MARGEM, ALTURA - y - 26, AREA, 26, fill=1, stroke=0)
    x = MARGEM
    for titulo, largura in colunas:
        texto(c, x + 9, y + 17, titulo, 9, "#FFFFFF", "Helvetica-Bold")
        x += largura
    y += 26
    for i, registro in enumerate(linhas):
        alturas = []
        for (_, largura), valor in zip(colunas, registro):
            estilo = ParagraphStyle("medida", fontName="Helvetica", fontSize=9.2, leading=12.5)
            alturas.append(Paragraph(valor, estilo).wrap(largura - 18, 1000)[1])
        altura = max(alturas) + 18
        if i % 2 == 0:
            c.setFillColor(tinta(FUNDO))
            c.rect(MARGEM, ALTURA - y - altura, AREA, altura, fill=1, stroke=0)
        x = MARGEM
        for (_, largura), valor in zip(colunas, registro):
            paragrafo(c, x + 9, y + 8, largura - 18, valor, 9.2, 12.5)
            x += largura
        y += altura
        linha(c, MARGEM, LARGURA - MARGEM, y)
    return y


def prova(c, y, local, codigo, explicacao):
    texto(c, MARGEM, y + 10, local, 9.4, MARINHO, "Helvetica-Bold")
    linhas = codigo.splitlines()
    altura = 14 + len(linhas) * 11
    caixa(c, MARGEM, y + 18, AREA, altura)
    for i, trecho in enumerate(linhas):
        texto(c, MARGEM + 9, y + 31 + 11 * i, trecho, 8.2, MARINHO, "Courier")
    return paragrafo(c, MARGEM, y + 23 + altura, AREA, explicacao, 9.7, 13) + 13


def evidencia(c, y, local, descricao):
    fim = paragrafo(c, MARGEM, y, AREA, f"<b>{escape(local)}</b><br/>{descricao}", 10, 14)
    return fim + 11


def portada(c):
    cabecalho(c, 1, "Escopo e método")
    caixa(c, MARGEM, 86, AREA, 235, MARINHO, 12)
    caixa(c, MARGEM + 22, 111, 48, 5, "#F7B75B", 2)
    texto(c, MARGEM + 23, 157, "Relatório de Auditoria", 31, "#FFFFFF", "Helvetica-Bold")
    texto(c, MARGEM + 23, 196, "de Segurança — AutoLab", 31, "#FFFFFF", "Helvetica-Bold")
    paragrafo(c, MARGEM + 24, 222, AREA - 48,
              "Revisão do código e da configuração versionada do SistemaEspecialista.",
              11, 15, "#D5E3F1")
    texto(c, MARGEM + 24, 280, DATA, 11, "#FFFFFF", "Helvetica-Bold")
    texto(c, MARGEM + 24, 300, f"portfolio/autolab  /  {REVISAO[:12]}", 9.5, "#D5E3F1")

    texto(c, MARGEM, 365, "Stack detectada", 20, MARINHO, "Helvetica-Bold")
    y = tabela(c, 381, (("Camada", 105), ("Tecnologia e observação", AREA - 105)), (
        ("Backend", "Java 17, Maven, Spring Boot WebMVC 4.1.3, Tomcat 11.0.29, Jackson 3 e Drools 10.2.0."),
        ("Dados e auth", "Processamento em memória. A aplicação não tem banco, ORM, query builder, contas, papéis, sessão de autenticação ou inquilinos."),
        ("Frontend", "React 19, TypeScript e Vite. A interface carrega JSON da API na mesma origem."),
        ("Deploy e CI", "Docker multistage, Docker Compose com socket Unix, Nginx e GitHub Actions. Helm e Terraform ausentes no inventário."),
    ))
    texto(c, MARGEM, y + 30, "Nota metodológica", 16, MARINHO, "Helvetica-Bold")
    fim = paragrafo(c, MARGEM, y + 40, AREA,
        "As categorias 1 a 3 foram mapeadas ao acesso a dados, aos papéis e à posse de objetos nos handlers Spring MVC. "
        "A categoria 4 foi aplicada ao código, ao deploy, ao CI, ao histórico Git local e ao bundle. "
        "Na categoria 5, rastreei dados da API até o DOM e revisei a validação e as respostas do servidor.")
    paragrafo(c, MARGEM, fim + 12, AREA,
        f"Os 35 arquivos do projeto no workspace correspondem à revisão publicada <b>{REVISAO[:7]}</b>, desconsiderando diferenças de fim de linha. "
        "A revisão cobriu o AutoLab. O servidor de produção, os bancos, os segredos do host e os demais projetos ficaram fora do escopo.",
        9.6, 13.4, SUAVE)
    c.showPage()


def rosca(c):
    caixa(c, MARGEM, 244, AREA, 200)
    x = MARGEM + 97
    y = ALTURA - 350
    total = len(ACHADOS)
    c.setFillColor(tinta(GRADE))
    c.circle(x, y, 61, fill=1, stroke=0)
    if total:
        inicio = 90
        for severidade, cor in SEVERIDADES.items():
            quantidade = sum(a["severidade"] == severidade for a in ACHADOS)
            if quantidade:
                angulo = 360 * quantidade / total
                c.setFillColor(tinta(cor))
                c.wedge(x - 61, y - 61, x + 61, y + 61, inicio, angulo, fill=1, stroke=0)
                inicio += angulo
    c.setFillColor(white)
    c.circle(x, y, 45, fill=1, stroke=0)
    c.setFillColor(tinta(MARINHO))
    c.setFont("Helvetica-Bold", 31)
    c.drawCentredString(x, y - 4, str(total))
    c.setFillColor(tinta(SUAVE))
    c.setFont("Helvetica", 9)
    c.drawCentredString(x, y - 20, "achados")
    texto(c, MARGEM + 182, 276, "Achados por severidade", 13, MARINHO, "Helvetica-Bold")
    por_severidade = Counter(a["severidade"] for a in ACHADOS)
    for i, (severidade, cor) in enumerate(SEVERIDADES.items()):
        pos = 302 + 26 * i
        caixa(c, MARGEM + 182, pos - 11, 11, 11, cor, 3)
        texto(c, MARGEM + 201, pos - 1, severidade, 10)
        alinhado_direita(c, LARGURA - MARGEM - 22, pos - 1,
                        str(por_severidade[severidade]), 10, MARINHO, "Helvetica-Bold")


def barras(c):
    texto(c, MARGEM, 486, "Achados por categoria", 16, MARINHO, "Helvetica-Bold")
    por_categoria = Counter(a["categoria"] for a in ACHADOS)
    esquerda = MARGEM + 211
    pista = 155
    maior = max(1, *por_categoria.values()) if por_categoria else 1
    for i, (categoria, situacao) in enumerate(CATEGORIAS, 1):
        y = 496 + i * 31
        texto(c, MARGEM, y + 1, categoria, 8.8)
        caixa(c, esquerda, y - 9, pista, 12, FUNDO, 3)
        valor = por_categoria[i]
        if valor:
            caixa(c, esquerda, y - 9, pista * valor / maior, 12, "#2563EB", 3)
        else:
            caixa(c, esquerda, y - 9, 2.6, 12, "#9CAEC0", 1)
        texto(c, esquerda + pista + 9, y + 1, str(valor), 9, MARINHO, "Helvetica-Bold")
        alinhado_direita(c, LARGURA - MARGEM, y + 1, situacao, 8.4, SUAVE)
    paragrafo(c, MARGEM, 674, AREA,
        "O anel neutro representa total zero. O marcador no início da barra indica zero achado, e a coluna da direita distingue a aplicabilidade.",
        9.5, 13.5, SUAVE)


def resumo(c):
    cabecalho(c, 2, "Resumo executivo")
    secao(c, 92, "Resumo executivo",
          "A revisão não identificou falhas verificáveis nas cinco categorias solicitadas.")
    espacamento = 11
    largura = (AREA - 2 * espacamento) / 3
    metricas = (
        (str(len(ACHADOS)), "Achados verificados"),
        ("3", "Categorias não aplicáveis"),
        ("2", "Categorias verificadas"),
    )
    for i, (valor, rotulo) in enumerate(metricas):
        x = MARGEM + i * (largura + espacamento)
        caixa(c, x, 151, largura, 73, VERDE_FUNDO if i == 0 else FUNDO)
        texto(c, x + 12, 185, valor, 27, VERDE_ESCURO if i == 0 else MARINHO, "Helvetica-Bold")
        texto(c, x + 12, 207, rotulo, 9.5, SUAVE)
    rosca(c)
    barras(c)
    c.showPage()


def rotas(c):
    cabecalho(c, 3, "Categorias 1 a 3")
    secao(c, 92, "Isolamento, autorização e IDOR",
          "Cobertura de todos os handlers de negócio e dos mapeamentos auxiliares observados.")
    y = tabela(c, 140, (("Severidade", 95), ("Arquivo:linha", 162), ("Descrição", AREA - 257)), (
        ("Não aplicável", "<b>1. Isolamento</b><br/>pom.xml:21-42<br/>service/DiagnosticoService.java:<br/>31-45 e 52-68",
         "O catálogo é global e vem de regras locais. Cada simulação usa uma sessão Drools própria em memória. Sem consultas, armazenamento ou inquilinos."),
        ("Não aplicável", "<b>2. Autorização</b><br/>frontend/src/main.tsx:<br/>148-150, 172-175, 188-192,<br/>208-213 e 231-232<br/>web/DiagnosticoController.java:<br/>38-67",
         "Os controles do navegador dependem de carregamento, catálogo e formulário. A API oferece catálogo e simulação públicos, sem mutações administrativas."),
        ("Não aplicável", "<b>3. IDOR</b><br/>web/DiagnosticoController.java:<br/>38-41, 43-67 e 79-80<br/>frontend/src/componentes.tsx:<br/>107-127",
         "Nenhum handler seleciona um registro por ID. O corpo contém nomes e valores de sintomas. IDs de resultados servem para comparar hipóteses na interface."),
    ))
    texto(c, MARGEM, y + 24, "Inventário dos mapeamentos", 15, MARINHO, "Helvetica-Bold")
    y = tabela(c, y + 36, (("Rota ou recurso", 122), ("Registro e comportamento", AREA - 122)), (
        ("GET /api/regras", "<b>web/DiagnosticoController.java:38-41.</b> Catálogo público, com 28 regras na execução local."),
        ("POST /api/diagnosticos", "<b>web/DiagnosticoController.java:43-67, 79-80.</b> Simulação JSON, com entradas nome/valor e sem persistência."),
        ("/, /** e /webjars/**", "<b>Framework e recursos estáticos.</b> Página inicial e arquivos de interface. Dockerfile:13-15 e frontend/index.html:11-12."),
        ("/error", "<b>BasicErrorController do Spring Boot.</b> Dois mapeamentos do mesmo caminho, geral e HTML. Detalhes de erro desabilitados em application.properties:13-17."),
    ))
    y = prova(c, y + 16, "DiagnosticoController.java:79-80",
        "public record Solicitacao(List<SintomaEntrada> sintomas) {}\n"
        "public record SintomaEntrada(String nome, String valor) {}",
        "O schema registra sintomas. A verificação foi feita no código e nos mapeamentos registrados durante a execução local.")
    paragrafo(c, MARGEM, y, AREA,
        "Os caminhos Java abreviados nesta página partem de src/main/java/com/example/sistemaespecialista/. "
        "application.properties está em src/main/resources/.",
        8.8, 12, SUAVE)
    c.showPage()


def segredos_xss(c):
    cabecalho(c, 4, "Categorias 4 e 5")
    secao(c, 92, "Segredos e XSS",
          "As duas categorias se aplicam à stack. A revisão encontrou zero ocorrências verificadas.")
    y = tabela(c, 140, (("Severidade", 96), ("Arquivo:linha", 168), ("Descrição", AREA - 264)), (
        ("Sem achado", "<b>4. Segredos</b><br/>compose.yaml:9-11<br/>deploy/nginx-autolab.conf:15-16<br/>.github/workflows/ci.yml:3-26",
         "GID e caminhos do socket são parâmetros operacionais. Os certificados aparecem como caminhos de exemplo. O fluxo de CI não declara credenciais."),
        ("Sem achado", "<b>4. Segredos</b><br/>mvnw:185-206<br/>mvnw.cmd:117-118<br/>frontend/src/main.tsx:10-11,35,51",
         "Wrappers leem credenciais opcionais do ambiente. URLs do frontend são fixas e públicas ou relativas à origem. Não apareceu chave embutida no bundle."),
        ("Sem achado", "<b>5. XSS</b><br/>frontend/src/main.tsx:<br/>131, 136, 153, 242, 255,<br/>258 e 260-262<br/>frontend/src/componentes.tsx:<br/>9-16, 42, 48-57, 90, 121 e 127",
         "Links fixos. Erros, hipóteses, catálogo e evidências chegam ao React como texto ou valores de controles. Sem HTML ou Markdown livre nem URLs dinâmicas em href/src."),
        ("Sem achado", "<b>5. XSS</b><br/>web/DiagnosticoController.java:<br/>33-67 e 73-81<br/>model/SintomasAceitos.java:80-114<br/>web/CabecalhosDeSeguranca.java:<br/>17-25",
         "API em JSON com DTO, limite de 16 KiB e opções permitidas para sintomas. O servidor aplica CSP e nosniff. Não há biblioteca de sanitização nem fluxo de HTML livre."),
    ))
    y = prova(c, y + 12, "frontend/src/main.tsx:242",
        '{erro && <p className="aviso erro" role="alert">{erro}</p>}',
        "O erro entra como filho textual do React. A busca no código autoral não encontrou inserção de HTML bruto nem avaliação de código a partir de dados do usuário.")
    y = evidencia(c, y, "Histórico e bundle",
        "Varredura local de formatos de segredo, atribuições e defaults nos 34 arquivos textuais do projeto "
        "e no histórico alcançável pelas referências e reflogs disponíveis. A cópia publicada cobre 7 commits e 72 blobs. "
        "As pontas remotas master e portfolio/autolab correspondem ao histórico revisado. "
        "O bundle regenerado contém 8 arquivos, sendo HTML, JavaScript, CSS e 5 fontes locais.")
    paragrafo(c, MARGEM, y, AREA,
        "Os caminhos Java abreviados partem de src/main/java/com/example/sistemaespecialista/. "
        "A inspeção de padrões não cobre segredos ausentes das referências locais ou presentes apenas no ambiente da VPS.",
        8.8, 12, SUAVE)
    c.showPage()


def conclusao(c):
    cabecalho(c, 5, "Controles e recomendações")
    secao(c, 92, "Pontos fortes e pontos fracos")
    caixa(c, MARGEM, 112, AREA, 31, VERDE_FUNDO)
    caixa(c, MARGEM + 11, 123, 9, 9, FORTE, 2)
    texto(c, MARGEM + 28, 132, "Pontos fortes verificados", 10.5, VERDE_ESCURO, "Helvetica-Bold")
    y = evidencia(c, 157, "Sessão por simulação · DiagnosticoService.java:52-68",
        "A sessão de regras é criada por chamada e descartada no <font face='Courier'>finally</font>. "
        "O catálogo vem de regras empacotadas e não recebe regras enviadas por usuários.")
    y = evidencia(c, y, "Validação e cabeçalhos · DiagnosticoController.java:33-67 e CabecalhosDeSeguranca.java:17-25",
        "Limite de corpo, JSON estrito, opções aceitas e CSP. A interface exibe hipóteses "
        "e mensagens de erro como texto.")
    y = evidencia(c, y, "Separação na configuração de VPS · compose.yaml:7-27",
        "O Compose usa rede <font face='Courier'>none</font>, raiz somente leitura, remoção de capacidades e <font face='Courier'>no-new-privileges</font>. "
        "O único bind mount declarado é o diretório dedicado do socket.")
    y = evidencia(c, y, "Confirmação em testes locais",
        "Maven em modo offline: 37 testes passaram. O build de produção do frontend foi regenerado e inspecionado.")
    texto(c, MARGEM, y + 7, "Pontos fracos e limites da revisão", 14, MARINHO, "Helvetica-Bold")
    y = paragrafo(c, MARGEM, y + 17, AREA,
        "Nenhum ponto fraco foi confirmado nas cinco categorias. A revisão é delimitada aos arquivos e ao histórico local acessível. "
        "A configuração efetiva da VPS, os bancos e a busca de CVEs nas dependências exigem verificações próprias.")
    texto(c, MARGEM, y + 26, "Recomendações priorizadas", 16, MARINHO, "Helvetica-Bold")
    y += 38
    y = evidencia(c, y, "P1 · Antes de publicar",
        "Verifique na VPS a configuração efetiva do proxy, o modo de rede e as permissões do socket. "
        "Confirme separadamente a exposição das portas dos bancos no firewall.")
    y = evidencia(c, y, "P2 · Nos próximos commits",
        "Automatize a busca por segredos em commits e no bundle, com saída redigida. "
        "Inclua arquivos de ambiente locais nas exclusões de versionamento antes de introduzi-los.")
    y = evidencia(c, y, "P3 · Se surgirem contas, registros ou papéis",
        "Defina autorização no backend e verificação de posse para cada operação privada. "
        "Adicione testes com usuários e inquilinos distintos antes de publicar essas rotas.")
    linha(c, MARGEM, LARGURA - MARGEM, y + 6)
    texto(c, MARGEM, y + 34, "ISSUES PARA O GITHUB", 17, MARINHO, "Helvetica-Bold")
    paragrafo(c, MARGEM, y + 45, AREA,
        "Nenhuma issue de vulnerabilidade a emitir. O conjunto de achados acionáveis está vazio.")
    c.showPage()


def main():
    c = Canvas(str(ARQUIVO), pagesize=A4, pageCompression=1)
    c.setTitle(TITULO)
    c.setAuthor("Auditoria do AutoLab")
    c.setSubject("Revisão de cinco categorias de segurança do SistemaEspecialista")
    for pagina in (portada, resumo, rotas, segredos_xss, conclusao):
        pagina(c)
    c.save()
    print(ARQUIVO)


if __name__ == "__main__":
    main()
