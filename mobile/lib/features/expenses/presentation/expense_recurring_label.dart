import '../../../l10n/app_localizations.dart';
import '../domain/expense_item.dart';

String? expenseRecurringLabel(ExpenseItem e, AppLocalizations l10n) {
  switch (e.recurringFrequency) {
    case 'monthly':
      return l10n.expenseRecurringMonthly;
    case 'weekly':
      return l10n.expenseRecurringWeekly;
    case 'yearly':
      return l10n.expenseRecurringYearly;
    default:
      return null;
  }
}
