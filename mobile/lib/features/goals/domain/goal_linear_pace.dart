import 'goal_item.dart';

/// Comprimento médio de um mês (365,2425 / 12), para ritmo linear simples.
const double kGoalLinearPaceDaysPerMonth = 30.436875;

/// Estimativa no cliente: ritmo = saldo atual / tempo desde `createdAt`;
/// meses até concluir = restante / ritmo (aprox., não é garantia).
class GoalLinearPaceEstimate {
  const GoalLinearPaceEstimate({
    required this.avgCentsPerMonth,
    required this.estimatedCompletionMonthEnd,
  });

  /// Média arredondada em centavos/mês (para formatação em R$).
  final int avgCentsPerMonth;

  /// Último dia do mês civil em que, a este ritmo, se atingiria o alvo (~).
  final DateTime estimatedCompletionMonthEnd;
}

GoalLinearPaceEstimate? computeGoalLinearPaceEstimate(
  GoalItem goal, {
  DateTime? now,
}) {
  if (!goal.isActive || goal.targetCents <= 0) return null;
  final remaining = goal.targetCents - goal.currentCents;
  if (remaining <= 0) return null;
  if (goal.currentCents <= 0) return null;

  final n = (now ?? DateTime.now()).toLocal();
  final created = goal.createdAt.toLocal();
  var daysElapsed = n.difference(created).inDays;
  if (daysElapsed < 1) {
    daysElapsed = 1;
  }

  final monthsElapsed = daysElapsed / kGoalLinearPaceDaysPerMonth;
  final avgPerMonth = goal.currentCents / monthsElapsed;
  if (avgPerMonth < 1) {
    return null;
  }

  final monthsNeeded = remaining / avgPerMonth;
  final daysToAdd = (monthsNeeded * kGoalLinearPaceDaysPerMonth).ceil();
  final rawEta = n.add(Duration(days: daysToAdd));

  final lastDay = DateTime(rawEta.year, rawEta.month + 1, 0);

  return GoalLinearPaceEstimate(
    avgCentsPerMonth: avgPerMonth.round(),
    estimatedCompletionMonthEnd: lastDay,
  );
}
