from __future__ import annotations

from app.services.goal_product_search import _shopping_blocks_to_rows
from app.services.thumbnail_proxy import is_allowed_thumbnail_url, normalize_thumbnail_url


def test_normalize_protocol_relative_google_thumb() -> None:
    raw = "//encrypted-tbn0.gstatic.com/shopping?q=tbn:abc"
    assert normalize_thumbnail_url(raw) == "https://encrypted-tbn0.gstatic.com/shopping?q=tbn:abc"


def test_normalize_rejects_javascript() -> None:
    assert normalize_thumbnail_url("javascript:alert(1)") is None


def test_allowlist_blocks_private_and_unknown_hosts() -> None:
    assert not is_allowed_thumbnail_url("https://127.0.0.1/x.png")
    assert not is_allowed_thumbnail_url("https://evil.example/x.png")
    assert is_allowed_thumbnail_url("https://encrypted-tbn3.gstatic.com/shopping?q=tbn:x")


def test_shopping_rows_normalize_thumbnail() -> None:
    rows = _shopping_blocks_to_rows(
        [
            {
                "title": "Fone",
                "link": "https://www.google.com/shopping",
                "extracted_price": 99.9,
                "thumbnail": "//encrypted-tbn0.gstatic.com/img.jpg",
            }
        ],
        5,
    )
    assert len(rows) == 1
    assert rows[0]["thumbnail"] == "https://encrypted-tbn0.gstatic.com/img.jpg"
