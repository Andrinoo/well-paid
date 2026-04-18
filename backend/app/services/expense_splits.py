"""Divisão de despesas partilhadas (valor ou %) e estados por membro."""

from __future__ import annotations

import uuid
from datetime import UTC, date, datetime
from typing import Literal

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.models.expense_share import ExpenseShare
from app.schemas.dashboard import ExpenseStatus

SplitMode = Literal["amount", "percent"]


class ExpenseSplitValidationError(ValueError):
    pass


def share_resolved(status: str) -> bool:
    return status in ("paid", "waived", "covered_by_peer")


def _allocate_cents_from_percent_bps(total: int, bps_a: int, bps_b: int) -> tuple[int, int]:
    if bps_a + bps_b != 10000:
        raise ExpenseSplitValidationError("Percentagens devem somar 100%")
    a = (total * bps_a + 5000) // 10000
    b = total - a
    if a < 0 or b < 0:
        raise ExpenseSplitValidationError("Partes inválidas")
    return a, b


def build_two_party_shares(
    *,
    owner_id: uuid.UUID,
    peer_id: uuid.UUID,
    amount_cents: int,
    split_mode: SplitMode,
    owner_share_cents: int | None,
    peer_share_cents: int | None,
    owner_percent_bps: int | None,
    peer_percent_bps: int | None,
) -> list[dict]:
    """Returns kwargs dicts for ExpenseShare rows (without expense_id)."""
    if amount_cents <= 0:
        raise ExpenseSplitValidationError("amount_cents deve ser positivo")
    if split_mode == "amount":
        if owner_share_cents is None or peer_share_cents is None:
            raise ExpenseSplitValidationError("Modo valor: indica a parte de cada um em centavos")
        o, p = int(owner_share_cents), int(peer_share_cents)
        if o + p != amount_cents:
            raise ExpenseSplitValidationError(
                f"A soma das partes ({o}+{p}) deve igualar o total ({amount_cents})"
            )
        return [
            {
                "user_id": owner_id,
                "share_cents": o,
                "share_percent_bps": None,
                "status": "pending",
            },
            {
                "user_id": peer_id,
                "share_cents": p,
                "share_percent_bps": None,
                "status": "pending",
            },
        ]
    # percent
    if owner_percent_bps is None or peer_percent_bps is None:
        raise ExpenseSplitValidationError("Modo percentagem: indica % de cada um (soma 100%)")
    ob, pb = int(owner_percent_bps), int(peer_percent_bps)
    o, p = _allocate_cents_from_percent_bps(amount_cents, ob, pb)
    return [
        {
            "user_id": owner_id,
            "share_cents": o,
            "share_percent_bps": ob,
            "status": "pending",
        },
        {
            "user_id": peer_id,
            "share_cents": p,
            "share_percent_bps": pb,
            "status": "pending",
        },
    ]


def clone_share_rows_from_expense(anchor) -> list[dict]:
    """Copia partes de uma despesa âncora (mesmos utilizadores e valores relativos)."""
    shares = list(getattr(anchor, "expense_shares", []) or [])
    if not shares:
        return []
    return [
        {
            "user_id": s.user_id,
            "share_cents": int(s.share_cents),
            "share_percent_bps": s.share_percent_bps,
            "status": "pending",
        }
        for s in shares
    ]


def replace_expense_shares(
    db: Session,
    expense_id: uuid.UUID,
    rows: list[dict],
) -> None:
    db.execute(delete(ExpenseShare).where(ExpenseShare.expense_id == expense_id))
    for r in rows:
        db.add(
            ExpenseShare(
                expense_id=expense_id,
                user_id=r["user_id"],
                share_cents=r["share_cents"],
                share_percent_bps=r.get("share_percent_bps"),
                status=r.get("status", "pending"),
                paid_at=r.get("paid_at"),
                covered_by_user_id=r.get("covered_by_user_id"),
                covered_at=r.get("covered_at"),
                declined_at=r.get("declined_at"),
                decline_reason=r.get("decline_reason"),
            )
        )


def mark_all_shares_paid_for_expense(
    db: Session,
    expense_id: uuid.UUID,
    *,
    now: datetime,
) -> list[ExpenseShare]:
    """Quando a linha de despesa nasce como paga (ex. competência passada)."""
    rows = list(db.scalars(select(ExpenseShare).where(ExpenseShare.expense_id == expense_id)).all())
    for s in rows:
        s.status = "paid"
        s.paid_at = now
    return rows


def scale_pending_share_amounts(
    db: Session,
    expense_id: uuid.UUID,
    *,
    new_total: int,
    old_total: int,
) -> None:
    """Escala centavos das partes quando o total muda e nenhuma parte está quitada."""
    if old_total <= 0 or new_total <= 0:
        return
    rows = list(
        db.scalars(
            select(ExpenseShare).where(ExpenseShare.expense_id == expense_id)
        ).all()
    )
    if len(rows) < 2:
        return
    if any(s.status == "declined" for s in rows):
        raise ExpenseSplitValidationError(
            "Esta despesa tem uma parte recusada; o criador deve usar Assumir despesa ou rever a situação."
        )
    if any(share_resolved(s.status) for s in rows):
        raise ExpenseSplitValidationError(
            "Não alteres o montante total com partes já quitadas; ajusta manualmente as partes."
        )
    acc = 0
    for i, s in enumerate(rows):
        if i == len(rows) - 1:
            s.share_cents = new_total - acc
        else:
            nc = int(round(int(s.share_cents) * new_total / old_total))
            s.share_cents = nc
            acc += nc


def sync_expense_row_from_shares(
    expense,
    shares: list[ExpenseShare],
    *,
    now: datetime | None = None,
) -> None:
    """Atualiza status/paid_at da despesa quando todas as partes estão resolvidas."""
    if not getattr(expense, "is_shared", False) or not shares:
        return
    now = now or datetime.now(UTC)
    if len(shares) < 2:
        return
    if all(share_resolved(s.status) for s in shares):
        expense.status = ExpenseStatus.PAID.value
        if expense.paid_at is None:
            expense.paid_at = now
    elif expense.status == ExpenseStatus.PAID.value:
        expense.status = ExpenseStatus.PENDING.value
        expense.paid_at = None


def compute_share_extras(
    *,
    viewer_id: uuid.UUID,
    expense,
    shares: list[ExpenseShare],
    today: date,
) -> dict:
    """Campos derivados para API (alertas, partes)."""
    out = {
        "split_mode": expense.split_mode if hasattr(expense, "split_mode") else None,
        "my_share_cents": None,
        "other_user_share_cents": None,
        "my_share_paid": False,
        "other_share_paid": False,
        "shared_expense_payment_alert": False,
        "shared_expense_peer_declined_alert": False,
        "my_share_declined": False,
        "other_user_id": expense.shared_with_user_id,
    }
    if not expense.is_shared or not shares or expense.shared_with_user_id is None:
        return out
    mine = next((s for s in shares if s.user_id == viewer_id), None)
    other = next(
        (s for s in shares if s.user_id != viewer_id),
        None,
    )
    if mine is None or other is None:
        return out
    out["owner_percent_bps"] = None
    out["peer_percent_bps"] = None
    if getattr(expense, "split_mode", None) == "percent" and expense.shared_with_user_id is not None:
        os = next((s for s in shares if s.user_id == expense.owner_user_id), None)
        ps = next((s for s in shares if s.user_id == expense.shared_with_user_id), None)
        if os is not None and os.share_percent_bps is not None:
            out["owner_percent_bps"] = int(os.share_percent_bps)
        if ps is not None and ps.share_percent_bps is not None:
            out["peer_percent_bps"] = int(ps.share_percent_bps)
    out["my_share_cents"] = int(mine.share_cents)
    out["other_user_share_cents"] = int(other.share_cents)
    out["my_share_paid"] = share_resolved(mine.status)
    out["other_share_paid"] = share_resolved(other.status)
    if mine.status == "declined":
        out["my_share_declined"] = True
    if other.status == "declined" and viewer_id == expense.owner_user_id:
        out["shared_expense_peer_declined_alert"] = True
    due = expense.due_date
    if (
        due is not None
        and today >= due
        and not out["my_share_paid"]
        and out["other_share_paid"]
    ):
        out["shared_expense_payment_alert"] = True
    return out
