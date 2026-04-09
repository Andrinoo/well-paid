import 'package:flutter/material.dart';

enum DueUrgency {
  overdue,
  dueToday,
  dueSoon,
  upcoming,
  safe,
}

DueUrgency dueUrgencyFor(DateTime dueDate, DateTime today) {
  final d0 = DateTime(today.year, today.month, today.day);
  final due = DateTime(dueDate.year, dueDate.month, dueDate.day);
  final days = due.difference(d0).inDays;
  if (days < 0) return DueUrgency.overdue;
  if (days == 0) return DueUrgency.dueToday;
  if (days <= 2) return DueUrgency.dueSoon;
  if (days <= 5) return DueUrgency.upcoming;
  return DueUrgency.safe;
}

Color dueUrgencyAccent(DueUrgency u) {
  return switch (u) {
    DueUrgency.overdue => const Color(0xFFB00020),
    DueUrgency.dueToday => const Color(0xFFD32F2F),
    DueUrgency.dueSoon => const Color(0xFFE57373),
    DueUrgency.upcoming => const Color(0xFFF9A825),
    DueUrgency.safe => const Color(0xFF2E7D32),
  };
}
