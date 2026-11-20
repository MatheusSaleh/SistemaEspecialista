import { Activity, CircleGauge, Fuel, ScanLine } from 'lucide-react'

export type Respostas = Record<string, string>
type Opcao = [valor: string, rotulo: string]
type Campo = { nome: string; rotulo: string; opcoes?: Opcao[]; unidade?: string }

const booleanos: Opcao[] = [['sim', 'Sim'], ['nao', 'Não']]
const painel: Opcao[] = [['acesa', 'Acesa'], ['apagada', 'Apagada']]

export const ignicao: Campo = {
  nome: 'motor_gira_mas_nao_pega',
  rotulo: 'O motor gira, mas não pega?',
  opcoes: booleanos,
}

export const camposPartida: Campo[] = [
  ignicao,
  { nome: 'nivel_combustivel', rotulo: 'Nível de combustível', opcoes: [['baixo', 'Baixo'], ['medio', 'Normal'], ['cheio', 'Cheio']] },
]

export const camposExtras: Campo[] = [
  { nome: 'motor_nao_liga', rotulo: 'O motor fica sem girar na partida?', opcoes: booleanos },
  { nome: 'bateria_nova', rotulo: 'A bateria é nova?', opcoes: booleanos },
  { nome: 'ruido_suspensao', rotulo: 'Há ruído na suspensão?', opcoes: booleanos },
  { nome: 'quilometragem', rotulo: 'Quilometragem', unidade: 'km' },
  { nome: 'luz_oleo', rotulo: 'Luz do óleo', opcoes: painel },
  { nome: 'freio_mole', rotulo: 'O pedal de freio está mole?', opcoes: booleanos },
  { nome: 'vazamento_fluido', rotulo: 'Há vazamento de fluido de freio?', opcoes: booleanos },
  { nome: 'temperatura_motor', rotulo: 'Temperatura do motor', opcoes: [['normal', 'Normal'], ['alta', 'Alta']] },
  { nome: 'luz_alerta', rotulo: 'Luz de alerta de temperatura', opcoes: painel },
  { nome: 'ruido_motor', rotulo: 'Ruído do motor', opcoes: [['normal', 'Normal'], ['estranho', 'Estranho']] },
  { nome: 'consumo_excessivo_combustivel', rotulo: 'O consumo de combustível aumentou?', opcoes: booleanos },
  { nome: 'luz_injecao', rotulo: 'Luz da injeção', opcoes: painel },
  { nome: 'resposta_acelerador', rotulo: 'Resposta do acelerador', opcoes: [['normal', 'Normal'], ['lenta', 'Lenta']] },
  { nome: 'ultimo_checkup', rotulo: 'Tempo desde a última revisão', unidade: 'dias' },
  { nome: 'clima', rotulo: 'Clima', opcoes: [['frio', 'Frio'], ['quente', 'Quente'], ['umido', 'Úmido'], ['seco', 'Seco']] },
  { nome: 'tipo_combustivel', rotulo: 'Tipo de combustível', opcoes: [['etanol', 'Etanol'], ['gasolina', 'Gasolina']] },
]

export const cenarios: { id: string; titulo: string; detalhe: string; icone: typeof Fuel; respostas: Respostas }[] = [
  { id: 'partida', titulo: 'Motor não pega', detalhe: 'Partida e combustível', icone: Fuel, respostas: { motor_gira_mas_nao_pega: 'sim', nivel_combustivel: 'baixo' } },
  { id: 'suspensao', titulo: 'Ruído na suspensão', detalhe: 'Quilometragem elevada', icone: CircleGauge, respostas: { ruido_suspensao: 'sim', quilometragem: '120000' } },
  { id: 'sem-alertas', titulo: 'Sem alertas', detalhe: 'Nenhuma regra acionada', icone: ScanLine, respostas: { motor_gira_mas_nao_pega: 'nao', nivel_combustivel: 'medio' } },
]

export const iconesHipoteses: Record<string, typeof Activity> = {
  combustivel: Fuel,
  suspensao: CircleGauge,
}

const campos = [...camposPartida, ...camposExtras]
export function descreverEvidencia(nome: string, valor: string) {
  const campo = campos.find((item) => item.nome === nome)
  const rotulo = campo?.rotulo ?? nome
  const resposta = campo?.opcoes?.find(([opcao]) => opcao === valor)?.[1]
  const numero = campo?.unidade ? `${Number(valor).toLocaleString('pt-BR')} ${campo.unidade}` : valor
  return { rotulo, resposta: resposta ?? numero }
}
