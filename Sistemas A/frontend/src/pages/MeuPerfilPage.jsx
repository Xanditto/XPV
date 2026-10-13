import { useEffect, useState } from 'react'
import { buscarPerfil, listarDestaques } from '../lib/api'
import LayoutApp from '../components/LayoutApp'
import { CardDestaque } from '../components/Destaques'

export default function MeuPerfilPage() {
  const [usuario, setUsuario] = useState(null)
  const [destaques, setDestaques] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    Promise.all([buscarPerfil(), listarDestaques()])
      .then(([dadosUsuario, destaquesAtuais]) => {
        setUsuario(dadosUsuario)
        setDestaques(destaquesAtuais)
      })
      .catch((err) => setErro(err.message || 'Não foi possível carregar o perfil.'))
      .finally(() => setCarregando(false))
  }, [])

  return (
    <LayoutApp>
      {carregando ? (
        <p className="mensagem">Carregando...</p>
      ) : (
        <>
          {erro && <p className="mensagem mensagem-erro">{erro}</p>}

          <div className="perfil-hero">
            <img
              src={usuario?.avatar || 'https://placehold.co/160x160?text=XPV'}
              alt="Foto de perfil"
              className="perfil-avatar-grande"
            />
            <div className="perfil-info">
              <h1 className="perfil-nome">{usuario?.nickname}</h1>
              {usuario?.descricao && <p className="perfil-descricao">{usuario.descricao}</p>}
            </div>
          </div>

          <h2 className="subtitulo-secao">Destaques</h2>
          {destaques.length === 0 ? (
            <p className="mensagem">Nenhum destaque escolhido ainda. Adicione em "Editar perfil".</p>
          ) : (
            <div className="lista-destaques">
              {destaques.map((destaque) => (
                <CardDestaque key={destaque.id} destaque={destaque} />
              ))}
            </div>
          )}
        </>
      )}
    </LayoutApp>
  )
}
