import { formatarNota } from '../lib/formato'

/**
 * Seletor de 0 a 5 estrelas em passos de meia estrela. Cada estrela tem uma
 * metade clicável esquerda (X,5) e direita (X inteiro) - ver CSS
 * ".estrela-botoes". Passe somenteLeitura para exibir sem permitir clique
 * (lista de avaliações já salvas).
 */
export default function SeletorEstrelas({ valor, onChange, somenteLeitura }) {
  return (
    <div className={`seletor-estrelas ${somenteLeitura ? 'seletor-estrelas-leitura' : ''}`}>
      {[1, 2, 3, 4, 5].map((posicao) => {
        const percentual = valor >= posicao ? 100 : valor >= posicao - 0.5 ? 50 : 0
        return (
          <span key={posicao} className="estrela-slot">
            <span className="estrela-fundo">★</span>
            <span className="estrela-preenchida" style={{ width: `${percentual}%` }}>
              ★
            </span>
            {!somenteLeitura && (
              <span className="estrela-botoes">
                <button
                  type="button"
                  className="estrela-botao"
                  aria-label={`${posicao - 0.5} estrela(s)`}
                  onClick={() => onChange(posicao - 0.5)}
                />
                <button
                  type="button"
                  className="estrela-botao"
                  aria-label={`${posicao} estrela(s)`}
                  onClick={() => onChange(posicao)}
                />
              </span>
            )}
          </span>
        )
      })}
      {!somenteLeitura && <span className="estrela-valor">{valor ? formatarNota(valor) : 'Escolha uma nota'}</span>}
    </div>
  )
}
