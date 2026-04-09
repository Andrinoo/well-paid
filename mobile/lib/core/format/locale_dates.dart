import 'package:flutter/widgets.dart';
import 'package:intl/intl.dart';

/// Tag BCP 47 para `intl` (meses por extenso coerentes com o idioma da UI).
String intlDateTagForUi(BuildContext context) {
  final loc = Localizations.localeOf(context);
  if (loc.languageCode == 'en') return 'en_US';
  return 'pt_BR';
}

String formatMonthYearUi(BuildContext context, DateTime d) {
  final tag = intlDateTagForUi(context);
  return DateFormat.yMMMM(tag).format(DateTime(d.year, d.month));
}

String formatDateTimeUi(BuildContext context, DateTime d) {
  final tag = intlDateTagForUi(context);
  return DateFormat.yMMMd(tag).add_Hm().format(d.toLocal());
}
