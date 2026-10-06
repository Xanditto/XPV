export default function Modal({ titulo, onFechar, children }) {
  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div className="modal-caixa" onClick={(e) => e.stopPropagation()}>
        <div className="modal-cabecalho">
          <strong>{titulo}</strong>
          <button type="button" className="botao-remover-destaque" onClick={onFechar} aria-label="Fechar">
            ×
          </button>
        </div>
        <div className="modal-conteudo">{children}</div>
      </div>
    </div>
  )
}
