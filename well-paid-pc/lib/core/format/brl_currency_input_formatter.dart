import 'package:flutter/services.dart';

import 'brl_cents.dart';

/// Formatadores para campos de valor em real: formatação automática `1.234,56`.
///
/// Aceita digitação ou colagem; só os dígitos contam (estilo caixa: a cada dígito
/// o valor em centavos é `anterior × 10 + dígito`; apagar remove o último dígito
/// da cadeia numérica implícita).
final List<TextInputFormatter> kBrCurrencyInputFormatters = [
  BrCurrencyInputFormatter(),
];

class BrCurrencyInputFormatter extends TextInputFormatter {
  BrCurrencyInputFormatter({this.maxDigitCount = 12});

  /// Teto de dígitos na cadeia (valor inteiro em centavos).
  final int maxDigitCount;

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.isEmpty) {
      return const TextEditingValue();
    }

    var digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    if (digits.length > maxDigitCount) {
      digits = digits.substring(0, maxDigitCount);
    }
    if (digits.isEmpty) {
      return const TextEditingValue();
    }

    final cents = int.tryParse(digits);
    if (cents == null) {
      return oldValue;
    }

    final formatted = formatBrlInputFromCents(cents);
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}
