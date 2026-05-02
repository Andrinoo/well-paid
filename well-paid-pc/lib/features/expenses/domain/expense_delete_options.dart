/// Alinhado com query params do DELETE /expenses/{id}.
enum ExpenseDeleteTarget {
  series,
  occurrence,
}

enum ExpenseDeleteScope {
  all,
  futureUnpaid,
}

String expenseDeleteTargetApi(ExpenseDeleteTarget t) {
  switch (t) {
    case ExpenseDeleteTarget.series:
      return 'series';
    case ExpenseDeleteTarget.occurrence:
      return 'occurrence';
  }
}

String expenseDeleteScopeApi(ExpenseDeleteScope s) {
  switch (s) {
    case ExpenseDeleteScope.all:
      return 'all';
    case ExpenseDeleteScope.futureUnpaid:
      return 'future_unpaid';
  }
}
