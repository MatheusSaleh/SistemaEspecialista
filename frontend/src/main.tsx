import { StrictMode, useEffect, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { Activity, BookOpen, ChevronDown, CircleCheck, CircleHelp, Code2, FlaskConical, Github, RotateCcw, ScanLine, SlidersHorizontal, Zap } from 'lucide-react'
import { camposRapidos, cenarios, entradaDaApi, iconesHipoteses, rotuloValor, type Catalogo, type Hipotese, type Regra, type Respostas, type Simulacao } from './cenarios'
import { Acompanhamento, CampoSintoma, CatalogoDialog, Comparacao } from './componentes'
import '@fontsource-variable/dm-sans/wght.css'
import '@fontsource-variable/space-grotesk/wght.css'
import './style.css'

const repositorio = 'https://github.com/MatheusSaleh/SistemaEspecialista'
const endpoint = '/api/diagnosticos'

type Relatorio = Simulacao & { anteriores: Hipotese[] | null }

function App() {
  const [respostas, setRespostas] = useState<Respostas>({})
  const [cenario, setCenario] = useState('')
  const [extras, setExtras] = useState(false)
  const [categoriaAtiva, setCategoriaAtiva] = useState('')
  const [relatorio, setRelatorio] = useState<Relatorio | null>(null)
  const [pendente, setPendente] = useState(false)
  const [desatualizado, setDesatualizado] = useState(false)
  const [erro, setErro] = useState('')
  const [catalogo, setCatalogo] = useState<Catalogo | null>(null)
  const [erroCatalogo, setErroCatalogo] = useState(false)
  const [tentativa, setTentativa] = useState(0)
  const formRef = useRef<HTMLFormElement>(null)
  const resultadoRef = useRef<HTMLHeadingElement>(null)
  const explicacaoRef = useRef<HTMLDialogElement>(null)
  const catalogoRef = useRef<HTMLDialogElement>(null)
  const camposPorNome = new Map(catalogo?.campos.map((campo) => [campo.nome, campo]))

  useEffect(() => {
    const cancelar = new AbortController()
    fetch('/api/regras', { signal: AbortSignal.any([cancelar.signal, AbortSignal.timeout(12000)]) })
      .then((resposta) => {
        if (!resposta.ok) throw new Error('Catálogo indisponível')
        return resposta.json() as Promise<Catalogo>
      })
      .then(setCatalogo)
      .catch(() => {
        if (!cancelar.signal.aborted) setErroCatalogo(true)
      })
    return () => cancelar.abort()
  }, [tentativa])

  async function simular(entrada: Respostas, anteriores: Hipotese[] | null) {
    setPendente(true)
    setErro('')
    try {
      const resposta = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(entradaDaApi(entrada)),
        signal: AbortSignal.timeout(10000),
      })
      if (resposta.status === 400) {
        const dados = await resposta.json()
        throw new Error(dados.mensagem ?? 'Revise os valores informados.')
      }
      if (!resposta.ok) throw new Error('O simulador está indisponível. Tente novamente em instantes.')

      const dados: Simulacao = await resposta.json()
      setRelatorio({ ...dados, anteriores })
      setDesatualizado(false)
    } catch (falha) {
      setErro(falha instanceof TypeError || (falha instanceof DOMException && falha.name === 'TimeoutError')
        ? 'O simulador está indisponível. Tente novamente em instantes.'
        : falha instanceof Error ? falha.message : 'Não foi possível concluir a simulação.')
    } finally {
      setPendente(false)
      if (window.matchMedia('(max-width: 760px)').matches) resultadoRef.current?.focus()
    }
  }

  function carregar(id: string) {
    const caso = cenarios.find((item) => item.id === id)
    if (!caso) return
    setCenario(id)
    iniciarCaso(caso.respostas, caso.categoria)
  }

  function iniciarCaso(entrada: Respostas, categoria?: string) {
    setRespostas(entrada)
    setExtras(Boolean(categoria))
    setCategoriaAtiva(categoria ?? '')
    setRelatorio(null)
    setDesatualizado(false)
    void simular(entrada, null)
  }

  function testarRegra(regra: Regra) {
    catalogoRef.current?.close()
    setCenario('')
    iniciarCaso(regra.exemplo, regra.categoria)
    formRef.current?.scrollIntoView({ block: 'start' })
  }

  function limpar() {
    setRespostas({})
    setCenario('')
    setExtras(false)
    setCategoriaAtiva('')
    setErro('')
    setRelatorio(null)
    setDesatualizado(false)
  }

  function alterar(nome: string, valor: string) {
    setRespostas((atuais) => ({ ...atuais, [nome]: valor }))
    setCenario('')
    setErro('')
    setDesatualizado(Boolean(relatorio))
  }

  function aplicarAcompanhamento(novasRespostas: Respostas) {
    const entrada = { ...respostas, ...novasRespostas }
    setRespostas(entrada)
    setCenario('')
    setDesatualizado(true)
    void simular(entrada, relatorio?.hipoteses ?? null)
  }

  const removidas = relatorio?.anteriores?.filter((anterior) => !relatorio.hipoteses.some((atual) => atual.id === anterior.id)) ?? []
  const temRespostas = Object.values(respostas).some(Boolean)

  return (
    <>
      <header className="topo">
        <div className="container topo-inner">
          <a href="#" className="marca"><span className="marca-icone"><Activity size={22} /></span>Auto<span>Lab</span></a>
          <nav aria-label="Navegação principal">
            <button className="link-tecnico" type="button" aria-haspopup="dialog" onClick={() => explicacaoRef.current?.showModal()}>
              <Code2 size={17} /> Como funciona
            </button>
            <a className="link-github" href={repositorio}><Github size={17} /> GitHub</a>
          </nav>
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="container hero-inner">
            <div className="hero-copy">
              <p className="sobretitulo"><FlaskConical size={16} /> LABORATÓRIO DE DIAGNÓSTICO AUTOMOTIVO</p>
              <h1>Do sintoma à <em>hipótese.</em></h1>
              <p>Explore um caso automotivo, altere uma resposta e descubra quais regras explicam o resultado.</p>
              <div className="acoes-hero">
                <button className="botao primario" type="button" disabled={pendente || !catalogo} onClick={() => {
                  carregar('partida')
                  if (window.matchMedia('(min-width: 761px)').matches) formRef.current?.scrollIntoView({ block: 'nearest' })
                }}><Zap size={17} /> Experimentar caso pronto</button>
                <a className="secundario" href="#simulador" onClick={() => requestAnimationFrame(() => formRef.current?.focus({ preventScroll: true }))}>Montar meu caso</a>
              </div>
            </div>
            <div className="hero-visual" aria-hidden="true">
              <div className="radar">
                <span className="radar-eixo radar-horizontal"></span>
                <span className="radar-eixo radar-vertical"></span>
                <span className="medidor"><Activity size={47} strokeWidth={1.6} /></span>
                <i className="ponto ponto-a"></i><i className="ponto ponto-b"></i><i className="ponto ponto-c"></i>
              </div>
              <div className="selo-visual"><span></span> Motor de inferência <b>Drools</b></div>
            </div>
          </div>
        </section>

        <div className="container bancada">
          <section className="casos" aria-labelledby="titulo-casos">
            <div className="casos-cabecalho">
              <h2 id="titulo-casos">Cenários de exemplo</h2>
              <button className="botao botao-catalogo" type="button" aria-haspopup="dialog" disabled={!catalogo || pendente}
                onClick={() => catalogoRef.current?.showModal()}>
                <BookOpen size={17} /> {catalogo ? `Catálogo · ${catalogo.regras.length} regras` : 'Catálogo de regras'}
              </button>
            </div>
            {!catalogo && <div className="status" role="status">
              {erroCatalogo ? (
                <div className="aviso erro">Não foi possível carregar o simulador.
                  <button className="botao texto" type="button" onClick={() => {
                    setErroCatalogo(false)
                    setTentativa((atual) => atual + 1)
                  }}>Tentar novamente</button>
                </div>
              ) : 'Carregando o catálogo de regras…'}
            </div>}
            <div className="casos-grid">
              {cenarios.map(({ id, titulo, detalhe, icone: Icone }) => (
                <button type="button" key={id} className="caso" aria-pressed={cenario === id} disabled={pendente || !catalogo} onClick={() => carregar(id)}>
                  <span className="caso-icone"><Icone size={20} /></span>
                  <span className="caso-texto"><strong>{titulo}</strong><small>{detalhe}</small></span>
                </button>
              ))}
            </div>
          </section>

          <div className="simulador" id="simulador">
            <form ref={formRef} tabIndex={-1} className="painel" onSubmit={(evento) => {
              evento.preventDefault()
              void simular(respostas, desatualizado ? relatorio?.hipoteses ?? null : relatorio?.anteriores ?? null)
            }}>
              <header className="painel-cabecalho">
                <span className="painel-icone"><SlidersHorizontal size={20} /></span>
                <div><h2>Sintomas do veículo</h2><p>Selecione o que você observou.</p></div>
              </header>
              <fieldset className="campos" disabled={pendente || !catalogo}>
                {camposRapidos.map((nome) => {
                  const campo = camposPorNome.get(nome)
                  return campo && <CampoSintoma key={nome} campo={campo} valor={respostas[nome] ?? ''}
                    alterar={(valor) => alterar(nome, valor)} />
                })}
                <details className="extras" open={extras} onToggle={(evento) => setExtras(evento.currentTarget.open)}>
                  <summary>Mais sintomas e condições <ChevronDown size={17} /></summary>
                  <div className="extras-areas">
                    {catalogo?.categorias.map((area) => (
                      <details className="area-sintomas" key={area.id} open={categoriaAtiva === area.id} onToggle={(evento) => {
                        const aberta = evento.currentTarget.open
                        setCategoriaAtiva((atual) => aberta ? area.id : atual === area.id ? '' : atual)
                      }}>
                        <summary>{area.nome}<ChevronDown size={16} /></summary>
                        <div className="extras-grid">
                          {catalogo.campos.filter((campo) => campo.categoria === area.id && !camposRapidos.includes(campo.nome))
                            .map((campo) => <CampoSintoma key={campo.nome} campo={campo} valor={respostas[campo.nome] ?? ''}
                              alterar={(valor) => alterar(campo.nome, valor)} />)}
                        </div>
                      </details>
                    ))}
                  </div>
                </details>
                <div className="acoes-form">
                  <button className="botao primario" type="submit" disabled={!temRespostas || pendente}><Zap size={17} /> {pendente ? 'Simulando…' : 'Simular diagnóstico'}</button>
                  <button className="botao texto" type="button" disabled={!temRespostas} onClick={limpar}><RotateCcw size={15} /> Limpar</button>
                </div>
              </fieldset>
            </form>

            <section className="painel painel-resultado" aria-labelledby="titulo-resultado" aria-busy={pendente}>
              <header className="painel-cabecalho">
                <span className="painel-icone"><Activity size={21} /></span>
                <div><h2 ref={resultadoRef} tabIndex={-1} id="titulo-resultado">Resultado da simulação</h2><p>Hipóteses e as regras que as geraram.</p></div>
              </header>
              {erro && <p className="aviso erro" role="alert">{erro}</p>}
              <div role="status" className="status">
                {pendente ? 'Avaliando as regras…' : desatualizado ? 'Respostas alteradas. Simule novamente para atualizar o resultado.' : relatorio ? `${relatorio.hipoteses.length} ${relatorio.hipoteses.length === 1 ? 'hipótese encontrada' : 'hipóteses encontradas'}` : null}
              </div>
              {relatorio && !desatualizado && !pendente ? (
                <div className="relatorio">
                  {removidas.length > 0 && <p className="aviso"><CircleCheck size={18} /> Saiu do resultado após a mudança: {removidas.map(({ titulo }) => titulo).join(', ')}.</p>}
                  {relatorio.hipoteses.length === 0 ? (
                    <div className="estado"><ScanLine size={28} /><strong>Nenhuma regra acionada</strong><p>A base de regras não encontrou uma hipótese para essas respostas.</p></div>
                  ) : relatorio.hipoteses.map((hipotese, indice) => {
                    const Icone = iconesHipoteses[hipotese.id] ?? Activity
                    return (
                      <article className="hipotese" key={hipotese.id}>
                        <h3><Icone size={19} /> {hipotese.titulo}</h3>
                        <details key={JSON.stringify(hipotese.evidencias)} open={indice === 0 ? true : undefined}>
                          <summary>Por que apareceu? <ChevronDown size={15} /></summary>
                          <p className="regra"><Code2 size={16} /><span>Regra: <strong>{hipotese.regra}</strong></span></p>
                          <dl className="evidencias">
                            {hipotese.evidencias.map(({ sintoma, valor }) => {
                              const campo = camposPorNome.get(sintoma)
                              return <div key={sintoma}><dt>{campo?.rotulo ?? sintoma}</dt><dd>{campo ? rotuloValor(campo, valor) : valor}</dd></div>
                            })}
                          </dl>
                        </details>
                      </article>
                    )
                  })}
                  {relatorio.anteriores && <Comparacao antes={relatorio.anteriores} depois={relatorio.hipoteses} />}
                  {relatorio.perguntas.length > 0 && <Acompanhamento perguntas={relatorio.perguntas} campos={camposPorNome} aplicar={aplicarAcompanhamento} />}
                </div>
              ) : !relatorio && !pendente && !erro && (
                <div className="estado"><ScanLine size={30} /><strong>Aguardando uma simulação</strong><p>Escolha um cenário de exemplo ou preencha os sintomas.</p></div>
              )}
            </section>
          </div>
          <p className="nota"><CircleHelp size={17} /> Simulação educacional com cenários fictícios. Procure avaliação profissional para ocorrências reais.</p>
        </div>
      </main>

      <footer>
        <div className="container rodape">
          <div><h2>Sobre o AutoLab</h2><p>Projeto de portfólio sobre sistemas especialistas e motores de regras.</p></div>
          <div className="tecnologias" aria-label="Tecnologias"><span>React + TypeScript</span><span>Spring Boot</span><span>Drools</span></div>
        </div>
      </footer>

      <CatalogoDialog dialogo={catalogoRef} catalogo={catalogo} campos={camposPorNome} testar={testarRegra} />
      <dialog ref={explicacaoRef} className="explicacao" aria-labelledby="titulo-explicacao">
        <h2 id="titulo-explicacao">Como funciona</h2>
        <ol>
          <li><strong>Você informa os sintomas.</strong> O formulário em React envia as respostas à API em Spring Boot.</li>
          <li><strong>O Drools avalia as regras.</strong> Cada regra gera uma hipótese quando as condições dela são atendidas.</li>
          <li><strong>Você confere a explicação.</strong> Em “Por que apareceu?”, veja o nome da regra e as respostas que a acionaram.</li>
        </ol>
        <p>No caso inicial, mudar o nível de combustível de <strong>Baixo</strong> para <strong>Normal</strong> remove a hipótese de combustível e mantém a de ignição.</p>
        <form method="dialog">
          <button className="botao primario" type="submit">Fechar</button>
        </form>
      </dialog>
    </>
  )
}

createRoot(document.getElementById('root')!).render(<StrictMode><App /></StrictMode>)
