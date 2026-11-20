import { useId, useState, type RefObject } from 'react'
import { BookOpen, ChevronDown, CircleHelp, FlaskConical, Zap } from 'lucide-react'
import { rotuloValor, type Campo, type Catalogo, type Hipotese, type Pergunta, type Regra, type Respostas } from './cenarios'

export function CampoSintoma({ campo, valor, alterar }: { campo: Campo; valor: string; alterar: (valor: string) => void }) {
  const id = useId()
  return (
    <div className="campo">
      <label htmlFor={id}>{campo.rotulo}{campo.unidade ? ` (${campo.unidade})` : ''}</label>
      {campo.unidade ? (
        <input id={id} type="number" min="0" max="2147483647" step="1" placeholder="Não informado"
          value={valor} onChange={(evento) => alterar(evento.target.value)} />
      ) : (
        <select id={id} value={valor} onChange={(evento) => alterar(evento.target.value)}>
          <option value="">Não informado</option>
          {campo.opcoes.map((opcao) => <option key={opcao.valor} value={opcao.valor}>{opcao.rotulo}</option>)}
        </select>
      )}
    </div>
  )
}

export function CatalogoDialog({ dialogo, catalogo, campos, testar }: {
  dialogo: RefObject<HTMLDialogElement | null>
  catalogo: Catalogo | null
  campos: Map<string, Campo>
  testar: (regra: Regra) => void
}) {
  const [categoria, setCategoria] = useState('')
  const regras = catalogo?.regras.filter((regra) => !categoria || regra.categoria === categoria) ?? []

  return (
    <dialog ref={dialogo} className="explicacao catalogo" aria-labelledby="titulo-catalogo">
      <header className="catalogo-cabecalho">
        <div><h2 id="titulo-catalogo">Catálogo de regras</h2><p>{regras.length} {regras.length === 1 ? 'regra' : 'regras'} · Simulação educacional</p></div>
        <form method="dialog"><button className="botao texto" type="submit">Fechar</button></form>
      </header>
      <label className="campo catalogo-filtro">
        Área do veículo
        <select value={categoria} onChange={(evento) => setCategoria(evento.target.value)}>
          <option value="">Todas as áreas</option>
          {catalogo?.categorias.map((area) => <option key={area.id} value={area.id}>{area.nome}</option>)}
        </select>
      </label>
      <div className="lista-regras">
        {regras.map((regra) => (
          <details className="regra-catalogo" key={regra.id}>
            <summary><span><strong>{regra.nome}</strong><small>{catalogo?.categorias.find((area) => area.id === regra.categoria)?.nome}</small></span><ChevronDown size={17} /></summary>
            <p>{regra.titulo}</p>
            <dl className="evidencias">
              {regra.condicoes.map((condicao) => {
                const campo = campos.get(condicao.campo)
                if (!campo) return null
                const descricao = condicao.maiorQue === null
                  ? condicao.valores.map((valor) => rotuloValor(campo, valor)).join(' ou ')
                  : `Acima de ${rotuloValor(campo, String(condicao.maiorQue))}`
                return <div key={campo.nome}><dt>{campo.rotulo}</dt><dd>{descricao}</dd></div>
              })}
            </dl>
            <button className="botao teste-regra" type="button" onClick={() => testar(regra)}><FlaskConical size={16} /> Testar esta regra</button>
          </details>
        ))}
      </div>
    </dialog>
  )
}

export function Acompanhamento({ perguntas, campos, aplicar }: {
  perguntas: Pergunta[]
  campos: Map<string, Campo>
  aplicar: (respostas: Respostas) => void
}) {
  const [respostas, setRespostas] = useState<Respostas>({})
  const temResposta = Object.values(respostas).some(Boolean)

  return (
    <form className="acompanhamento" aria-labelledby="titulo-acompanhamento" onSubmit={(evento) => {
      evento.preventDefault()
      aplicar(Object.fromEntries(Object.entries(respostas).filter(([, valor]) => valor !== '')))
    }}>
      <h3 id="titulo-acompanhamento"><CircleHelp size={18} /> Perguntas de acompanhamento</h3>
      <p>Uma resposta adicional pode esclarecer estas hipóteses.</p>
      {perguntas.map((pergunta) => {
        const campo = campos.get(pergunta.campo)
        if (!campo) return null
        return (
          <div className="pergunta" key={campo.nome}>
            <CampoSintoma campo={campo} valor={respostas[campo.nome] ?? ''}
              alterar={(valor) => setRespostas((atuais) => ({ ...atuais, [campo.nome]: valor }))} />
            <small>Regras que esta resposta ajuda a avaliar: {pergunta.regrasPossiveis.join(', ')}.</small>
          </div>
        )
      })}
      <button className="botao primario" type="submit" disabled={!temResposta}><Zap size={16} /> Aplicar e simular</button>
    </form>
  )
}

function iguais(antes: Hipotese, depois: Hipotese) {
  return antes.titulo === depois.titulo && antes.evidencias.length === depois.evidencias.length &&
    antes.evidencias.every((evidencia, i) => evidencia.sintoma === depois.evidencias[i].sintoma &&
      evidencia.valor === depois.evidencias[i].valor)
}

export function Comparacao({ antes, depois }: { antes: Hipotese[]; depois: Hipotese[] }) {
  const antesPorId = new Map(antes.map((hipotese) => [hipotese.id, hipotese]))
  const depoisPorId = new Map(depois.map((hipotese) => [hipotese.id, hipotese]))
  const rotuloDepois = (hipotese: Hipotese) => {
    const anterior = antesPorId.get(hipotese.id)
    if (!anterior) return 'Nova'
    return iguais(anterior, hipotese) ? 'Mantida' : 'Atualizada'
  }

  return (
    <section className="comparacao" aria-labelledby="titulo-comparacao">
      <h3 id="titulo-comparacao"><BookOpen size={17} /> Antes e depois</h3>
      <div className="comparacao-grid">
        <div>
          <h4>Simulação anterior</h4>
          {antes.length ? <ul>{antes.map((hipotese) => (
            <li key={hipotese.id}>{hipotese.titulo}{!depoisPorId.has(hipotese.id) && <span className="situacao removida">Removida</span>}</li>
          ))}</ul> : <p>Nenhuma hipótese.</p>}
        </div>
        <div>
          <h4>Simulação atual</h4>
          {depois.length ? <ul>{depois.map((hipotese) => (
            <li key={hipotese.id}>{hipotese.titulo}<span className="situacao">{rotuloDepois(hipotese)}</span></li>
          ))}</ul> : <p>Nenhuma hipótese.</p>}
        </div>
      </div>
    </section>
  )
}
