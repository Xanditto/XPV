import { useEffect, useMemo, useState } from 'react'
import { listarContas, listarJogos, sincronizarJogos } from '../lib/api'
import LayoutApp from '../components/LayoutApp'

function formatarHoras(horas) {
  if (horas < 1) return `${Math.round(horas * 60)} min`
  return `${horas.toFixed(1)} h`
}

function porcentagemConquistas(jogo) {
  if (jogo.conquistasTotais === 0) return -1
  return jogo.conquistasObtidas / jogo.conquistasTotais
}

const ORDENACOES = {
  horas: {
    rotulo: 'Horas jogadas',
    comparar: (a, b) => b.horasJogadas - a.horasJogadas,
  },
  conquistas: {
    rotulo: 'Porcentagem de conquistas',
    comparar: (a, b) => porcentagemConquistas(b) - porcentagemConquistas(a),
  },
  alfabetica: {
    rotulo: 'Ordem alfabética',
    comparar: (a, b) => a.nome.localeCompare(b.nome, 'pt-BR'),
  },
}

function IconeSteam() {
  return (
    <svg
      className="icone-steam"
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="currentColor"
      aria-label="Steam"
      role="img"
    >
      <title>Steam</title>
      <path d="M11.979 0C5.678 0 .511 4.86.022 11.037l6.432 2.658c.545-.371 1.203-.59 1.912-.59.063 0 .125.004.188.006l2.861-4.142V8.91c0-2.495 2.028-4.524 4.524-4.524 2.494 0 4.524 2.03 4.524 4.524s-2.03 4.524-4.524 4.524h-.105l-4.076 2.911c0 .052.004.105.004.159 0 1.875-1.515 3.396-3.39 3.396-1.635 0-3.016-1.173-3.331-2.727L.436 15.27C1.862 20.307 6.486 24 11.979 24c6.627 0 11.999-5.373 11.999-12S18.605.001 11.979.001zM7.54 18.21l-1.473-.61c.262.543.714.999 1.314 1.25 1.297.539 2.793-.076 3.334-1.375.263-.63.264-1.319.005-1.949s-.75-1.121-1.377-1.383c-.624-.26-1.29-.249-1.878-.03l1.523.63c.956.4 1.409 1.5 1.009 2.455-.397.957-1.497 1.41-2.457 1.012zM19.981 8.91c0-1.662-1.353-3.015-3.015-3.015-1.665 0-3.015 1.353-3.015 3.015 0 1.665 1.35 3.015 3.015 3.015 1.663 0 3.015-1.35 3.015-3.015zm-5.293-.003c0-1.259 1.02-2.281 2.278-2.281 1.26 0 2.28 1.022 2.28 2.281s-1.02 2.28-2.28 2.28c-1.258.001-2.278-1.021-2.278-2.28z" />
    </svg>
  )
}

function IconeConquista100() {
  return (
    <svg
      className="icone-conquista-100"
      viewBox="0 0 24 24"
      width="14"
      height="14"
      aria-label="100% das conquistas"
      role="img"
    >
      <title>100% das conquistas</title>
      <circle cx="12" cy="12" r="11" fill="#66c0f4" />
      <path
        d="M12 4.5l2.18 4.42 4.88.71-3.53 3.44.83 4.85L12 15.9l-4.36 2.02.83-4.85-3.53-3.44 4.88-.71z"
        fill="#ffd700"
      />
    </svg>
  )
}

export default function BibliotecaPage() {
  const [conta, setConta] = useState(null)
  const [jogos, setJogos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [ordenacao, setOrdenacao] = useState('horas')

  useEffect(() => {
    listarContas()
      .then(async (contas) => {
        const contaSteam = contas.find((c) => c.plataforma === 'Steam')
        setConta(contaSteam || null)
        if (!contaSteam || contaSteam.bibliotecaPublica !== true) {
          return []
        }
        const jogosAtuais = await listarJogos(contaSteam.id)
        // Se a biblioteca já deveria ter jogos mas ainda não sincronizamos
        // nenhum, sincroniza automaticamente uma vez (sem precisar de botão).
        if (jogosAtuais.length === 0) {
          return sincronizarJogos(contaSteam.id)
        }
        return jogosAtuais
      })
      .then(setJogos)
      .catch((err) => setErro(err.message || 'Não foi possível carregar a biblioteca.'))
      .finally(() => setCarregando(false))
  }, [])

  const jogosOrdenados = useMemo(
    () => [...jogos].sort(ORDENACOES[ordenacao].comparar),
    [jogos, ordenacao],
  )

  return (
    <LayoutApp>
      <div className="cabecalho-biblioteca">
        <h1 className="titulo-pagina">Biblioteca de Jogos</h1>
        {jogos.length > 0 && (
          <label className="campo-ordenacao">
            <span>Ordenar por</span>
            <select value={ordenacao} onChange={(e) => setOrdenacao(e.target.value)}>
              {Object.entries(ORDENACOES).map(([chave, { rotulo }]) => (
                <option key={chave} value={chave}>
                  {rotulo}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      {carregando ? (
        <p className="mensagem">Carregando...</p>
      ) : !conta ? (
        <p className="mensagem">
          Você ainda não vinculou uma conta Steam. Vincule uma em "Contas vinculadas" para ver sua biblioteca.
        </p>
      ) : conta.bibliotecaPublica !== true ? (
        <p className="mensagem mensagem-erro">
          Sua biblioteca de jogos Steam está privada, então não conseguimos exibi-la aqui. Torne-a
          pública em "Detalhes do jogo", nas configurações de privacidade da Steam.
        </p>
      ) : (
        <>
          <p className="subtitulo-pagina">
            {jogos.length > 0 ? `${jogos.length} jogos.` : 'Seus jogos da Steam.'}
          </p>

          {erro && <p className="mensagem mensagem-erro">{erro}</p>}

          {jogosOrdenados.length === 0 ? (
            <p className="mensagem">Nenhum jogo encontrado.</p>
          ) : (
            <div className="lista-jogos">
              {jogosOrdenados.map((jogo) => {
                const completo = jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais
                return (
                  <div key={jogo.appId} className="linha-jogo">
                    <img
                      src={jogo.imagem || 'https://placehold.co/32x32?text=%20'}
                      alt={jogo.nome}
                      className="icone-jogo"
                    />
                    <span className="nome-jogo">
                      <IconeSteam />
                      {jogo.nome}
                    </span>
                    {jogo.conquistasTotais > 0 && (
                      <span className={`conquistas-jogo ${completo ? 'conquistas-completo' : 'conquistas-incompleto'}`}>
                        {completo && <IconeConquista100 />}
                        {jogo.conquistasObtidas}/{jogo.conquistasTotais}
                      </span>
                    )}
                    <span className="horas-jogo">{formatarHoras(jogo.horasJogadas)}</span>
                  </div>
                )
              })}
            </div>
          )}
        </>
      )}

      {erro && !conta && <p className="mensagem mensagem-erro">{erro}</p>}
    </LayoutApp>
  )
}
