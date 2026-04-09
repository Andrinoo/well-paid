import 'package:flutter/material.dart';

import '../../../l10n/app_localizations.dart';
import '../../../core/theme/well_paid_colors.dart';

/// Alinhado com a validação do backend (`app.core.security`).
final passwordPolicyRegExp = RegExp(
  r'^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,}$',
);

String? validatePasswordRules(String? v, AppLocalizations l10n) {
  if (v == null || v.isEmpty) return l10n.authPasswordRequired;
  if (!passwordPolicyRegExp.hasMatch(v)) {
    return l10n.authPasswordRulesError;
  }
  return null;
}

String? validateEmailField(String? v, AppLocalizations l10n) {
  if (v == null || v.trim().isEmpty) return l10n.authEmailRequired;
  final t = v.trim();
  if (!t.contains('@') || !t.contains('.')) return l10n.authEmailInvalid;
  return null;
}

/// Ícone à direita do campo (senha) — alvo de toque menor, proporção mais leve.
ButtonStyle authFieldSuffixIconButtonStyle() {
  return IconButton.styleFrom(
    visualDensity: VisualDensity.compact,
    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
    padding: const EdgeInsets.all(6),
    minimumSize: const Size(36, 36),
  );
}

InputDecoration authFieldDecoration(
  BuildContext context, {
  required String label,
  Widget? suffixIcon,
  String? hintText,
}) {
  return InputDecoration(
    labelText: label,
    hintText: hintText,
    hintStyle: TextStyle(color: WellPaidColors.authHint, fontSize: 13),
    suffixIcon: suffixIcon,
    isDense: true,
    filled: true,
    fillColor: WellPaidColors.authFieldFill,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: BorderSide(
        color: Colors.white.withValues(alpha: 0.08),
      ),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: BorderSide(
        color: Colors.white.withValues(alpha: 0.1),
      ),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: const BorderSide(
        color: WellPaidColors.brandBlue,
        width: 1.5,
      ),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: BorderSide(
        color: Colors.red.shade300.withValues(alpha: 0.9),
      ),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: BorderSide(
        color: Colors.red.shade200,
        width: 1.5,
      ),
    ),
    labelStyle: TextStyle(
      color: WellPaidColors.authOnCardMuted,
      fontSize: 13,
    ),
    floatingLabelStyle: const TextStyle(
      color: WellPaidColors.brandBlue,
      fontWeight: FontWeight.w600,
      fontSize: 13,
    ),
    errorStyle: TextStyle(
      color: Colors.red.shade200,
      fontSize: 11,
    ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
  );
}
