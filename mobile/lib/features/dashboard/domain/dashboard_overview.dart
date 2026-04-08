/// Período mensal no payload `period` de `/dashboard/overview`.
class PeriodMonth {
  const PeriodMonth({required this.year, required this.month});

  final int year;
  final int month;

  factory PeriodMonth.fromJson(Map<String, dynamic> json) {
    return PeriodMonth(
      year: json['year'] as int,
      month: json['month'] as int,
    );
  }
}

class CategorySpend {
  const CategorySpend({
    required this.categoryKey,
    required this.name,
    required this.amountCents,
    this.shareBps,
  });

  final String categoryKey;
  final String name;
  final int amountCents;
  final int? shareBps;

  factory CategorySpend.fromJson(Map<String, dynamic> json) {
    return CategorySpend(
      categoryKey: json['category_key'] as String,
      name: json['name'] as String,
      amountCents: json['amount_cents'] as int,
      shareBps: json['share_bps'] as int?,
    );
  }
}

class PendingExpenseItem {
  const PendingExpenseItem({
    required this.id,
    required this.description,
    required this.amountCents,
    this.dueDate,
    this.isMine = true,
  });

  final String id;
  final String description;
  final int amountCents;
  final DateTime? dueDate;
  final bool isMine;

  factory PendingExpenseItem.fromJson(Map<String, dynamic> json) {
    final raw = json['due_date'];
    return PendingExpenseItem(
      id: json['id'] as String,
      description: json['description'] as String,
      amountCents: json['amount_cents'] as int,
      dueDate: raw == null ? null : DateTime.parse(raw as String),
      isMine: json['is_mine'] as bool? ?? true,
    );
  }
}

class GoalSummaryItem {
  const GoalSummaryItem({
    required this.id,
    required this.title,
    required this.currentCents,
    required this.targetCents,
    this.isMine = true,
  });

  final String id;
  final String title;
  final int currentCents;
  final int targetCents;
  final bool isMine;

  factory GoalSummaryItem.fromJson(Map<String, dynamic> json) {
    return GoalSummaryItem(
      id: json['id'] as String,
      title: json['title'] as String,
      currentCents: json['current_cents'] as int,
      targetCents: json['target_cents'] as int,
      isMine: json['is_mine'] as bool? ?? true,
    );
  }
}

class DashboardOverview {
  const DashboardOverview({
    required this.period,
    required this.monthIncomeCents,
    required this.monthExpenseTotalCents,
    required this.monthBalanceCents,
    required this.spendingByCategory,
    required this.pendingTotalCents,
    required this.pendingPreview,
    required this.upcomingDue,
    required this.goalsPreview,
  });

  final PeriodMonth period;
  final int monthIncomeCents;
  final int monthExpenseTotalCents;
  final int monthBalanceCents;
  final List<CategorySpend> spendingByCategory;
  final int pendingTotalCents;
  final List<PendingExpenseItem> pendingPreview;
  final List<PendingExpenseItem> upcomingDue;
  final List<GoalSummaryItem> goalsPreview;

  factory DashboardOverview.fromJson(Map<String, dynamic> json) {
    return DashboardOverview(
      period: PeriodMonth.fromJson(json['period'] as Map<String, dynamic>),
      monthIncomeCents: json['month_income_cents'] as int,
      monthExpenseTotalCents: json['month_expense_total_cents'] as int,
      monthBalanceCents: json['month_balance_cents'] as int,
      spendingByCategory: (json['spending_by_category'] as List<dynamic>)
          .map((e) => CategorySpend.fromJson(e as Map<String, dynamic>))
          .toList(),
      pendingTotalCents: json['pending_total_cents'] as int,
      pendingPreview: (json['pending_preview'] as List<dynamic>)
          .map((e) => PendingExpenseItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      upcomingDue: (json['upcoming_due'] as List<dynamic>)
          .map((e) => PendingExpenseItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      goalsPreview: (json['goals_preview'] as List<dynamic>)
          .map((e) => GoalSummaryItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
