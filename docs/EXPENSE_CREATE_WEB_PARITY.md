# Inserção de despesas — paridade Android e web

**Escopo:** somente criação de despesas. Edição, pagamento e exclusão pertencem a
outras jornadas.

| Capacidade do Android | Implementação web |
|---|---|
| Despesa única | Disponível |
| Marcar como já paga | Disponível |
| Vencimento opcional na despesa única | Disponível |
| Data da despesa | Disponível |
| Plano parcelado de 2 a 999 parcelas | Disponível |
| Juros mensais de 0% a 100% | Disponível |
| Primeiro vencimento como âncora do parcelamento | Disponível |
| Recorrência mensal, semanal ou anual | Disponível |
| Primeiro vencimento obrigatório em parcelas/recorrência | Disponível |
| Categoria | Disponível |
| Visibilidade no modo família | Disponível quando habilitada no perfil |
| Seleção de outro membro | Disponível |
| Divisão por valor | Disponível, incluindo extremos 0/100 |
| Contraparte calculada no modo valor | Disponível e somente leitura |
| Divisão por percentual | Disponível, incluindo extremos 0/100 |
| Percentual da contraparte | Disponível e somente leitura |
| Estimativa em reais da divisão percentual | Disponível |
| Conversão ao alternar valor ↔ percentual | Disponível |
| Descrição limitada a 500 caracteres | Disponível |
| Ajuda contextual | Disponível |
| Feedback durante gravação | Disponível |

## Regras preservadas no payload

- Parcelas enviam `expense_date` igual ao primeiro `due_date` e `start_date=null`.
- Recorrência envia `start_date` igual à data da despesa.
- Parcelas e recorrências exigem `due_date`.
- Divisão por valor sempre soma exatamente `amount_cents`.
- Divisão percentual sempre soma 10.000 basis points e usa o mesmo arredondamento
  do backend e do Android.
- `is_family` e `is_shared` somente são enviados ativos quando o modo família está
  habilitado no perfil.
