# Metas — paridade Android e web

| Capacidade do Android | Implementação web |
|---|---|
| Criar meta manual | Disponível |
| Título e valor-alvo | Disponível |
| Valor inicial | Disponível |
| Descrição e data-alvo | Disponível |
| Pesquisar produtos | Disponível, até 12 resultados |
| Aplicar nome, preço, link e miniatura da pesquisa | Disponível |
| Miniatura com fallback | Proxy → HTTPS original → ícone padrão |
| Link manual de produto | Disponível |
| Meta ativa/arquivada | Disponível |
| Meta familiar | Disponível quando o modo família está ativo |
| Rastreamento automático de preço | Disponível |
| Editar meta | Disponível para o proprietário |
| Atualizar preço de referência | Disponível para o proprietário |
| Registrar aporte | Disponível |
| Nota no aporte | Disponível |
| Histórico de aportes | Disponível |
| Excluir meta sem saldo | Disponível com confirmação |
| Meta com saldo | Exclusão bloqueada; oferece arquivamento |
| Visualizar meta compartilhada | Disponível sem ações de proprietário |

## Causa das miniaturas quebradas

O componente web convertia toda URL em uma chamada ao proxy de miniaturas. O
backend bloqueia corretamente hosts fora da sua allowlist; nesses casos o navegador
recebia uma resposta de erro e o componente mantinha uma imagem quebrada.

A web agora tenta, nesta ordem:

1. proxy controlado pela API;
2. URL HTTPS original com `referrerPolicy=no-referrer`;
3. ícone padrão de meta.

URLs não HTTPS não são carregadas diretamente.
