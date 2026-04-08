"""Limites do núcleo família (MVP)."""

MAX_FAMILY_MEMBERS = 5


def family_has_room(member_count: int) -> bool:
    return member_count < MAX_FAMILY_MEMBERS
