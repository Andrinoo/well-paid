# Matriz de paridade Android → web

Esta matriz mede jornadas completas, não apenas a presença de uma página. Deve ser
atualizada junto com cada entrega do port.

| Domínio | Consulta | Criar | Editar | Excluir/estado | Casos especiais | Automação | Estado |
|---|---|---|---|---|---|---|---|
| Autenticação | Sim | Sim | senha/perfil parcial | logout | confirmação, recuperação, Turnstile | Ausente | Parcial |
| Dashboard | Sim | — | — | — | período, previsão, pendências | Ausente | Parcial |
| Despesas | Sim | Concluído | Ausente | pagar/excluir parcial | criação: parcelas, recorrência e partilha concluídas | testes unitários da criação | Parcial |
| Receitas | Sim | Sim | Ausente | excluir | recorrência | Ausente | Parcial |
| Metas | Sim | Concluído | Concluído | excluir sem saldo / arquivar com saldo | contribuição, busca, miniatura, histórico e preço | testes unitários e de miniatura | Concluído |
| Reserva | Sim | Sim | Ausente | Ausente | contribuição e múltiplos planos | Ausente | Parcial |
| Investimentos | Sim | Sim | Ausente | Ausente | tipos de ativo, cotação e cache | Ausente | Parcial |
| Listas de compras | Sim | Sim | itens parcial | lista parcial | totais, descontos, preço, partilha | Ausente | Parcial |
| Família | Sim | criar/entrar | nome parcial | Ausente | convites, membros e permissões | Ausente | Parcial |
| Valores a receber | Ausente | — | Ausente | Ausente | aceitar/recusar/liquidar | Ausente | Ausente |
| Avisos | banner parcial | — | — | — | leitura e placements | Ausente | Parcial |
| Definições | Sim | — | nome | — | privacidade, segurança e categorias | Ausente | Parcial |
| Entitlements | A validar | — | — | bloqueio | trial, módulos e Pix | testes backend | Parcial |
| Superadmin | Sim | Sim | Sim | ativar/desativar | billing e auditoria | testes backend | Parcial |

## Próxima sequência de port

1. Despesas: edição, recorrência, parcelas e divisão familiar.
2. Receitas: edição e recorrência.
3. Metas: detalhe, edição, exclusão e histórico de contribuições.
4. Reserva: edição, exclusão e detalhes dos planos.
5. Família e valores a receber.
6. Compras: edição de itens, totais, descontos e pesquisa de preços.
7. Investimentos e resiliência dos providers.
8. Definições, avisos e acabamento de acessibilidade.
