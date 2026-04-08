import 'package:flutter/material.dart';

import '../../../core/theme/well_paid_colors.dart';

enum DueUrgency { high, medium, low }

DueUrgency dueUrgencyFor(DateTime dueDate, DateTime today) {
  final d0 = DateTime(today.year, today.month, today.day);
  final due = DateTime(dueDate.year, dueDate.month, dueDate.day);
  final days = due.difference(d0).inDays;
  if (days <= 3) return DueUrgency.high;
  if (days <= 7) return DueUrgency.medium;
  return DueUrgency.low;
}

Color dueUrgencyAccent(DueUrgency u) {
  return switch (u) {
    DueUrgency.high => const Color(0xFFC62828),
    DueUrgency.medium => const Color(0xFFE65100),
    DueUrgency.low => WellPaidColors.navy.withValues(alpha: 0.45),
  };
}
