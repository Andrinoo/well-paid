/// Adiciona [delta] meses civis, ajustando o dia ao último dia do mês destino.
DateTime addCalendarMonths(DateTime d, int delta) {
  var y = d.year;
  var m = d.month + delta;
  while (m > 12) {
    m -= 12;
    y++;
  }
  while (m < 1) {
    m += 12;
    y--;
  }
  final lastDay = DateTime(y, m + 1, 0).day;
  final day = d.day > lastDay ? lastDay : d.day;
  return DateTime(y, m, day);
}

bool expenseReferenceMonthIsAfterCurrentCalendarMonth(
  DateTime reference,
  DateTime now,
) {
  final r = DateTime(reference.year, reference.month);
  final c = DateTime(now.year, now.month);
  return r.isAfter(c);
}
