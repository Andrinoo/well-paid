BASIC_PLAN_CENTS = 1499
BASIC_PLAN_CURRENCY = "BRL"
TRIAL_HOURS = 24
PAID_PERIOD_DAYS = 30

MODULE_DASHBOARD = "dashboard"
MODULE_PAYABLES = "payables"
MODULE_INCOMES = "incomes"
MODULE_SETTINGS = "settings"

BASIC_MODULE_IDS = (MODULE_DASHBOARD, MODULE_PAYABLES, MODULE_INCOMES)

MODULE_CATALOG: tuple[dict[str, object], ...] = (
    {
        "id": MODULE_DASHBOARD,
        "included_in_basic": True,
        "price_cents": 0,
    },
    {
        "id": MODULE_PAYABLES,
        "included_in_basic": True,
        "price_cents": 0,
    },
    {
        "id": MODULE_INCOMES,
        "included_in_basic": True,
        "price_cents": 0,
    },
)


def catalog_public() -> list[dict[str, object]]:
    return [dict(item) for item in MODULE_CATALOG]
