import uuid
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from app.services.family_scope import family_peer_user_ids


def test_family_scope_defaults_to_user_only_when_sharing_disabled() -> None:
    db = MagicMock()
    user_id = uuid.uuid4()

    with patch(
        "app.services.family_scope.get_settings",
        return_value=SimpleNamespace(family_data_sharing_enabled=False),
    ):
        assert family_peer_user_ids(db, user_id) == [user_id]


def test_family_scope_reads_members_when_sharing_enabled() -> None:
    db = MagicMock()
    user_id = uuid.uuid4()
    peer_id = uuid.uuid4()

    with (
        patch(
            "app.services.family_scope.get_settings",
            return_value=SimpleNamespace(family_data_sharing_enabled=True),
        ),
        patch("app.services.family_scope.session_has_table", return_value=True),
    ):
        db.scalar.return_value = SimpleNamespace(family_id="fam-1")
        db.scalars.return_value.all.return_value = [user_id, peer_id]
        out = family_peer_user_ids(db, user_id)

    assert out == [user_id, peer_id]
