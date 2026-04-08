from __future__ import annotations

import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.goal import Goal
from app.models.user import User
from app.schemas.goal import GoalContribute, GoalCreate, GoalResponse, GoalUpdate

router = APIRouter(prefix="/goals", tags=["goals"])


def _owned_goal(db: Session, goal_id: uuid.UUID, owner_id: uuid.UUID) -> Goal | None:
    return db.query(Goal).filter(Goal.id == goal_id, Goal.owner_user_id == owner_id).first()


@router.get("", response_model=list[GoalResponse])
def list_goals(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[GoalResponse]:
    rows = (
        db.query(Goal)
        .filter(Goal.owner_user_id == user.id)
        .order_by(Goal.is_active.desc(), Goal.updated_at.desc())
        .all()
    )
    return rows


@router.post("", response_model=GoalResponse, status_code=status.HTTP_201_CREATED)
def create_goal(
    body: GoalCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = Goal(
        owner_user_id=user.id,
        title=body.title.strip(),
        target_cents=body.target_cents,
        current_cents=body.current_cents,
        is_active=body.is_active,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


@router.put("/{goal_id}", response_model=GoalResponse)
def update_goal(
    goal_id: uuid.UUID,
    body: GoalUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    data = body.model_dump(exclude_unset=True)
    if "title" in data and data["title"] is not None:
        data["title"] = data["title"].strip()
    for k, v in data.items():
        setattr(row, k, v)
    db.commit()
    db.refresh(row)
    return row


@router.delete("/{goal_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_goal(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    db.delete(row)
    db.commit()


@router.post("/{goal_id}/contribute", response_model=GoalResponse)
def contribute_goal(
    goal_id: uuid.UUID,
    body: GoalContribute,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    row.current_cents = int(row.current_cents) + int(body.amount_cents)
    db.commit()
    db.refresh(row)
    return row
