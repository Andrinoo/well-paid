import '../domain/expense_item.dart';
import '../../dashboard/presentation/due_urgency.dart';

/// Lista “A pagar” com metadados da origem dos dados.
class ToPaySnapshot {
  const ToPaySnapshot({
    required this.items,
    required this.servedFromLocalCache,
  });

  final List<ExpenseItem> items;
  final bool servedFromLocalCache;
}

/// Filtro rápido no ecrã A pagar.
enum ToPayQuickFilter {
  all,
  overdue,
  thisWeek,
}

bool isToPayOverdue(ExpenseItem e, DateTime today) {
  return dueUrgencyForExpense(e, today) == DueUrgency.overdue;
}

/// Segunda-feira–domingo da semana civil que contém [today].
bool isToPayDueThisCalendarWeek(ExpenseItem e, DateTime today) {
  final anchor = e.dueDate ?? e.expenseDate;
  final a = DateTime(anchor.year, anchor.month, anchor.day);
  final start = today.subtract(Duration(days: today.weekday - 1));
  final weekStart = DateTime(start.year, start.month, start.day);
  final weekEnd = weekStart.add(const Duration(days: 6));
  return !a.isBefore(weekStart) && !a.isAfter(weekEnd);
}

List<ExpenseItem> applyToPayQuickFilter(
  List<ExpenseItem> sorted,
  ToPayQuickFilter filter,
  DateTime today,
) {
  switch (filter) {
    case ToPayQuickFilter.all:
      return sorted;
    case ToPayQuickFilter.overdue:
      return sorted.where((e) => isToPayOverdue(e, today)).toList();
    case ToPayQuickFilter.thisWeek:
      return sorted
          .where(
            (e) =>
                !isToPayOverdue(e, today) &&
                isToPayDueThisCalendarWeek(e, today),
          )
          .toList();
  }
}

int sumAmountCents(Iterable<ExpenseItem> items) =>
    items.fold<int>(0, (a, e) => a + e.amountCents);

/// Data usada para ordenação cronológica: vencimento, ou competência se não houver vencimento.
DateTime toPaySortKey(ExpenseItem e) {
  final d = e.dueDate ?? e.expenseDate;
  return DateTime(d.year, d.month, d.day);
}

/// Ordenação cronológica por vencimento (parcelas do mesmo plano ficam 1, 2, 3… no mesmo dia).
int compareToPayChronological(ExpenseItem a, ExpenseItem b) {
  final c = toPaySortKey(a).compareTo(toPaySortKey(b));
  if (c != 0) return c;
  final ga = a.installmentGroupId;
  final gb = b.installmentGroupId;
  if (ga != null && ga.isNotEmpty && ga == gb) {
    final n = a.installmentNumber.compareTo(b.installmentNumber);
    if (n != 0) return n;
  }
  return a.description.compareTo(b.description);
}

/// Cor de alerta pela distância em dias até ao vencimento (ou à competência se não houver vencimento).
DueUrgency dueUrgencyForExpense(ExpenseItem e, DateTime today) {
  final anchor = e.dueDate ?? e.expenseDate;
  return dueUrgencyFor(anchor, today);
}
