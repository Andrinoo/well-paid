import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/date/calendar_month.dart';
import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../l10n/app_localizations.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/expenses_providers.dart';
import '../domain/expense_item.dart';

/// Diálogo de confirmação e chamada a [ExpensesRepository.payExpense].
Future<void> confirmAndPayExpense(
  BuildContext context,
  WidgetRef ref, {
  required ExpenseItem expense,
  void Function(WidgetRef ref)? onPaid,
}) {
  return confirmAndPayExpenseById(
    context,
    ref,
    expenseId: expense.id,
    description: expense.description,
    amountCents: expense.amountCents,
    installmentNumber: expense.installmentNumber,
    installmentTotal: expense.installmentTotal,
    earlyCheckReference: expense.dueDate ?? expense.expenseDate,
    onPaid: onPaid,
  );
}

/// Versão para pré-visualizações do dashboard ([PendingExpenseItem]).
Future<void> confirmAndPayExpenseById(
  BuildContext context,
  WidgetRef ref, {
  required String expenseId,
  required String description,
  required int amountCents,
  int installmentNumber = 1,
  int installmentTotal = 1,
  DateTime? earlyCheckReference,
  void Function(WidgetRef ref)? onPaid,
}) async {
  final l10n = context.l10n;
  final messenger = ScaffoldMessenger.maybeOf(context);

  if (earlyCheckReference != null) {
    final now = DateTime.now();
    if (expenseReferenceMonthIsAfterCurrentCalendarMonth(
      earlyCheckReference,
      now,
    )) {
      final earlyOk = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.expensePayEarlyTitle),
          content: Text(l10n.expensePayEarlyBody),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.confirm),
            ),
          ],
        ),
      );
      if (earlyOk != true || !context.mounted) return;
    }
  }

  final amount = formatBrlFromCents(amountCents);
  var body = '$description\n$amount';
  if (installmentTotal > 1) {
    body += '\n${l10n.expensePayInstallmentLine(installmentNumber, installmentTotal)}';
  }
  final ok = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(l10n.expensePayConfirmTitle),
      content: Text(body),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx, false),
          child: Text(l10n.cancel),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(ctx, true),
          child: Text(l10n.expensePay),
        ),
      ],
    ),
  );
  if (ok != true || !context.mounted) return;
  await HapticFeedback.lightImpact();
  try {
    await ref.read(expensesRepositoryProvider).payExpense(expenseId);
    ref.invalidate(dashboardOverviewProvider);
    ref.invalidate(expensesListProvider);
    ref.invalidate(toPayListProvider);
    onPaid?.call(ref);
    if (context.mounted) {
      messenger?.showSnackBar(SnackBar(content: Text(l10n.expenseMarkedPaid)));
    }
  } catch (err) {
    if (!context.mounted) return;
    messenger?.showSnackBar(
      SnackBar(content: Text(_payErrorMessage(err, l10n))),
    );
  }
}

String _payErrorMessage(Object err, AppLocalizations l10n) {
  if (err is DioException && err.response?.statusCode == 409) {
    return l10n.expensePayConflict;
  }
  return messageFromDio(err, l10n) ?? l10n.expensePayError;
}
