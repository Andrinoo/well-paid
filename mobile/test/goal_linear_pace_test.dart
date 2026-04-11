import 'package:flutter_test/flutter_test.dart';
import 'package:well_paid/features/goals/domain/goal_item.dart';
import 'package:well_paid/features/goals/domain/goal_linear_pace.dart';

void main() {
  test('no estimate when no savings yet', () {
    final g = GoalItem(
      id: '1',
      title: 't',
      targetCents: 120_00,
      currentCents: 0,
      isActive: true,
      createdAt: DateTime.utc(2025, 1, 1),
      updatedAt: DateTime.utc(2025, 6, 1),
    );
    expect(computeGoalLinearPaceEstimate(g, now: DateTime.utc(2025, 6, 15)), isNull);
  });

  test('estimate pace and eta when there is progress', () {
    final g = GoalItem(
      id: '1',
      title: 't',
      targetCents: 120_00,
      currentCents: 60_00,
      isActive: true,
      createdAt: DateTime.utc(2025, 1, 1),
      updatedAt: DateTime.utc(2025, 4, 1),
    );
    final est = computeGoalLinearPaceEstimate(g, now: DateTime.utc(2025, 4, 1));
    expect(est, isNotNull);
    expect(est!.avgCentsPerMonth, greaterThan(0));
    expect(est.estimatedCompletionMonthEnd.isAfter(DateTime(2025, 4, 1)), isTrue);
  });
}
