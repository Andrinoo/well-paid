from __future__ import annotations

import uuid
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import MagicMock

from fastapi.testclient import TestClient

from app.api.deps import get_current_admin_user
from app.core.database import get_db
from app.api.routes import announcements as ann_route
from app.main import app
from app.schemas.announcement import AnnouncementPatch


def test_patch_announcement_does_not_resolve_target_when_field_absent(monkeypatch) -> None:
    admin = SimpleNamespace(id=uuid.uuid4(), email="admin@test.com")
    now = datetime.now(UTC)
    row = SimpleNamespace(
        id=uuid.uuid4(),
        title="Aviso",
        body="Corpo",
        kind="info",
        placement="home_feed",
        priority=0,
        cta_label=None,
        cta_url=None,
        is_active=True,
        starts_at=None,
        ends_at=None,
        created_by_user_id=None,
        target_user_id=uuid.uuid4(),
        target_user=None,
        created_at=now,
        updated_at=now,
    )
    db = MagicMock()
    db.get.side_effect = [row, SimpleNamespace(email="user@test.com")]
    refreshed = MagicMock()
    refreshed.one.return_value = row
    db.scalars.return_value = refreshed

    def _boom(*_args, **_kwargs):
        raise AssertionError("target user resolver should not be called")

    monkeypatch.setattr(ann_route, "_resolve_target_user_id", _boom)
    monkeypatch.setattr(ann_route, "_engagement_stats_for_announcements", lambda *_: {})

    app.dependency_overrides[get_current_admin_user] = lambda: admin

    def _db():
        yield db

    app.dependency_overrides[get_db] = _db
    body = AnnouncementPatch(title="Novo título")
    client = TestClient(app)
    response = client.patch(
        f"/admin/announcements/{row.id}",
        headers={"Authorization": "Bearer x"},
        json=body.model_dump(exclude_none=True),
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert row.title == "Novo título"
