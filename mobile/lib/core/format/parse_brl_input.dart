/// Converte texto livre (ex. `12,50`, `1.234,56`) em centavos. `null` se inválido.
int? parseInputToCents(String raw) {
  var s = raw.trim();
  if (s.isEmpty) return null;
  s = s.replaceAll(RegExp(r'[^\d,\.]'), '');
  if (s.isEmpty) return null;

  String intPart;
  var decPart = '00';

  final commaIdx = s.lastIndexOf(',');
  final dotIdx = s.lastIndexOf('.');

  if (commaIdx >= 0 && (dotIdx < 0 || commaIdx > dotIdx)) {
    final parts = s.split(',');
    intPart = parts[0].replaceAll('.', '');
    if (parts.length > 1) {
      decPart = parts[1].replaceAll(RegExp(r'[^\d]'), '');
    }
  } else if (dotIdx >= 0) {
    final after = s.substring(dotIdx + 1);
    if (after.length <= 2 && !s.substring(0, dotIdx).contains('.')) {
      final parts = s.split('.');
      intPart = parts[0];
      decPart = parts[1].replaceAll(RegExp(r'[^\d]'), '');
    } else {
      intPart = s.replaceAll('.', '');
    }
  } else {
    intPart = s;
  }

  if (intPart.isEmpty) intPart = '0';
  if (decPart.length > 2) decPart = decPart.substring(0, 2);
  decPart = '${decPart}00'.substring(0, 2);

  final a = int.tryParse(intPart);
  final b = int.tryParse(decPart);
  if (a == null || b == null) return null;
  if (a < 0) return null;
  return a * 100 + b;
}
