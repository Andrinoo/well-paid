import 'package:flutter/material.dart';

/// Urgência por dias até ao vencimento (calendário civil, meia-noite a meia-noite).
///
/// Bandas (alinhadas com “A pagar” e listas):
/// - [overdue]: já passou o vencimento → vermelho crítico
/// - [dueToday]: vence hoje → vermelho mais claro que atrasado
/// - [dueSoon]: falta 1–3 dias → âmbar (atenção sem pânico de “hoje”)
/// - [upcoming]: falta 4–10 dias → verde mais claro (há margem)
/// - [safe]: falta 11+ dias → verde firme
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
  if (days <= 3) return DueUrgency.dueSoon;
  if (days <= 10) return DueUrgency.upcoming;
  return DueUrgency.safe;
}

/// Cor de destaque (barra lateral, legenda, chips visuais).
Color dueUrgencyAccent(DueUrgency u) {
  return switch (u) {
    DueUrgency.overdue => const Color(0xFFB00020),
    DueUrgency.dueToday => const Color(0xFFE53935),
    DueUrgency.dueSoon => const Color(0xFFF9A825),
    DueUrgency.upcoming => const Color(0xFF7CB342),
    DueUrgency.safe => const Color(0xFF2E7D32),
  };
}

/// Cor para **texto** em fundo claro (cream): tons ligeiramente mais escuros para leitura confortável.
Color dueUrgencyOnLightBackground(DueUrgency u) {
  return switch (u) {
    DueUrgency.overdue => const Color(0xFF8E0018),
    DueUrgency.dueToday => const Color(0xFFC62828),
    DueUrgency.dueSoon => const Color(0xFFE65100),
    DueUrgency.upcoming => const Color(0xFF558B2F),
    DueUrgency.safe => const Color(0xFF1B5E20),
  };
}

/// Peso do texto: destaque só quando a ação é urgente; estados calmos ficam mais leves.
FontWeight dueUrgencyValueWeight(DueUrgency u) {
  return switch (u) {
    DueUrgency.overdue => FontWeight.w800,
    DueUrgency.dueToday => FontWeight.w800,
    DueUrgency.dueSoon => FontWeight.w700,
    DueUrgency.upcoming => FontWeight.w600,
    DueUrgency.safe => FontWeight.w500,
  };
}
