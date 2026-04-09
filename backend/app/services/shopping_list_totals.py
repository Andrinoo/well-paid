"""Totais de lista de compras (centavos inteiros)."""


def sum_line_amounts_cents(line_amounts: list[int | None]) -> int:
    return sum(int(x) for x in line_amounts if x is not None)


def resolve_checkout_total_cents(
    *,
    line_amounts: list[int | None],
    total_cents_override: int | None,
) -> int:
    summed = sum_line_amounts_cents(line_amounts)
    if total_cents_override is not None:
        if total_cents_override <= 0:
            raise ValueError("shopping_list_total_invalid")
        return int(total_cents_override)
    if summed <= 0:
        raise ValueError("shopping_list_total_empty")
    return summed
