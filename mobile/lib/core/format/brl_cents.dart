/// Formatação BRL a partir de centavos (sem float).
String formatBrlFromCents(int cents) {
  final negative = cents < 0;
  var v = cents.abs();
  final intPart = v ~/ 100;
  final frac = (v % 100).toString().padLeft(2, '0');
  final digits = intPart.toString();
  final buf = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) {
      buf.write('.');
    }
    buf.write(digits[i]);
  }
  final num = buf.toString();
  return '${negative ? '-' : ''}R\$ $num,$frac';
}

/// Valor para campo editável (sem "R\$", sem milhares), ex. `1234,56`.
String formatBrlInputFromCents(int cents) {
  final negative = cents < 0;
  var v = cents.abs();
  final reais = v ~/ 100;
  final frac = (v % 100).toString().padLeft(2, '0');
  return '${negative ? '-' : ''}$reais,$frac';
}
