"""Totais de lista de compras (centavos inteiros).

`line_amount_cents` em cada item é o preço **unitário**; a contribuição da linha
para o total é unitário × quantidade (quantidade ≥ 1).
"""


def sum_line_extensions_cents(extensions: list[tuple[int | None, int]]) -> int:
    """Soma (unit_cents * qty) por linha; ignora linhas sem preço unitário."""
    total = 0
    for cents_raw, qty_raw in extensions:
        if cents_raw is None:
            continue
        cents = int(cents_raw)
        qty = max(1, int(qty_raw))
        total += cents * qty
    return total


def resolve_checkout_total_cents(
    *,
    line_extensions: list[tuple[int | None, int]],
    total_cents_override: int | None,
    discount_cents: int | None = None,
) -> int:
    """Define o total da compra.

    Prioridade: ``total_cents_override`` (valor pago na caixa, ignora soma).
    Senão: soma das linhas menos ``discount_cents`` (≥ 0), ou só a soma.
    """
    summed = sum_line_extensions_cents(line_extensions)
    if total_cents_override is not None:
        if total_cents_override <= 0:
            raise ValueError("shopping_list_total_invalid")
        return int(total_cents_override)
    discount = int(discount_cents) if discount_cents is not None else 0
    if discount < 0:
        raise ValueError("shopping_list_discount_invalid")
    if discount > 0:
        total = summed - discount
        if total <= 0:
            raise ValueError("shopping_list_total_after_discount_invalid")
        return total
    if summed <= 0:
        raise ValueError("shopping_list_total_empty")
    return summed
