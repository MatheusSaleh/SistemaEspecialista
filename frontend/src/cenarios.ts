import { Activity, BatteryCharging, CalendarClock, CircleGauge, Fuel, ScanLine, ThermometerSun } from 'lucide-react'

export type Respostas = Record<string, string>
export type Categoria = { id: string; nome: string }
export type Campo = {
  nome: string
  rotulo: string
  categoria: string
  unidade: string | null
  opcoes: { valor: string; rotulo: string }[]
}
export type Regra = {
  id: string
  nome: string
  titulo: string
  categoria: string
  condicoes: { campo: string; valores: string[]; maiorQue: number | null }[]
  exemplo: Respostas
}
export type Catalogo = { categorias: Categoria[]; campos: Campo[]; regras: Regra[] }
export type Hipotese = { id: string; titulo: string; regra: string; evidencias: { sintoma: string; valor: string }[] }
export type Pergunta = { campo: string; regrasPossiveis: string[] }
export type Simulacao = { hipoteses: Hipotese[]; perguntas: Pergunta[] }

export const camposRapidos = ['motor_gira_mas_nao_pega', 'nivel_combustivel']

export const cenarios: { id: string; titulo: string; detalhe: string; icone: typeof Fuel; respostas: Respostas; categoria?: string }[] = [
  { id: 'partida', titulo: 'Motor não pega', detalhe: 'Compare dois níveis de combustível', icone: Fuel, respostas: { motor_gira_mas_nao_pega: 'sim', nivel_combustivel: 'baixo' } },
  { id: 'suspensao', titulo: 'Ruído na suspensão', detalhe: 'Quilometragem elevada', icone: CircleGauge, categoria: 'suspensao', respostas: { ruido_suspensao: 'sim', quilometragem: '120000' } },
  { id: 'sem-alertas', titulo: 'Sem alertas', detalhe: 'Nenhuma regra acionada', icone: ScanLine, respostas: { motor_gira_mas_nao_pega: 'nao', nivel_combustivel: 'medio' } },
  { id: 'eletrica', titulo: 'Clique ao dar partida', detalhe: 'Responda se as luzes enfraquecem', icone: BatteryCharging, categoria: 'partida', respostas: { motor_nao_liga: 'sim', clique_partida: 'sim', bateria_nova: 'nao', terminais_oxidados: 'nao' } },
  { id: 'temperatura', titulo: 'Temperatura alta', detalhe: 'Combine a temperatura e a luz de alerta', icone: ThermometerSun, categoria: 'arrefecimento', respostas: { temperatura_motor: 'alta' } },
  { id: 'revisao', titulo: 'Prazo da revisão', detalhe: 'Compare 365 e 366 dias', icone: CalendarClock, categoria: 'manutencao', respostas: { ultimo_checkup: '365' } },
]

export const iconesHipoteses: Record<string, typeof Activity> = {
  combustivel: Fuel,
  suspensao: CircleGauge,
  temperatura: ThermometerSun,
  revisao: CalendarClock,
}

export function rotuloValor(campo: Campo, valor: string) {
  if (campo.unidade) return `${Number(valor).toLocaleString('pt-BR')} ${campo.unidade}`
  return campo.opcoes.find((opcao) => opcao.valor === valor)?.rotulo ?? valor
}

export function entradaDaApi(respostas: Respostas) {
  return { sintomas: Object.entries(respostas)
    .filter(([, valor]) => valor !== '')
    .map(([nome, valor]) => ({ nome, valor })) }
}
