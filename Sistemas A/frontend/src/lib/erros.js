/**
 * Mensagem padrão pra qualquer erro de rede (carregar uma página ou enviar
 * um formulário): usa a mensagem específica do erro quando existe, e sempre
 * acrescenta uma dica sobre instabilidade do túnel (Cloudflare) usado nos
 * testes, já que esse é o motivo mais comum de falha nesse ambiente.
 */
export function mensagemDeErroDeConexao(err, mensagemPadrao) {
  const base = err?.message || mensagemPadrao
  return (
    `${base} Isso pode ser uma instabilidade passageira do serviço de túnel (Cloudflare) usado para ` +
    'testes - espere alguns segundos e atualize a página (F5). Se persistir, feche e abra o site novamente.'
  )
}
