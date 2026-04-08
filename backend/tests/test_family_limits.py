from app.services.family_limits import MAX_FAMILY_MEMBERS, family_has_room


def test_family_has_room() -> None:
    assert family_has_room(0) is True
    assert family_has_room(MAX_FAMILY_MEMBERS - 1) is True
    assert family_has_room(MAX_FAMILY_MEMBERS) is False


def test_max_constant() -> None:
    assert MAX_FAMILY_MEMBERS == 5
