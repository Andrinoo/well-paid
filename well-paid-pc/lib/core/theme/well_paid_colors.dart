import 'package:flutter/material.dart';

/// Cores da app (home / geral).
abstract final class WellPaidColors {
  /// Home e áreas claras (mantém identidade anterior).
  static const Color navy = Color(0xFF1B2C41);
  static const Color navyMid = Color(0xFF22344D);
  static const Color navyDeep = Color(0xFF141C2A);
  static const Color gold = Color(0xFFC9A94E);
  static const Color goldPressed = Color(0xFFB8943D);
  static const Color cream = Color(0xFFF5F1E8);
  static const Color creamMuted = Color(0xFFEAE6DD);

  // --- Autenticação (logo “well paid” + fundo preto) ---

  /// Fundo do ecrã de login (igual ao preto do PNG do logo; evita halo / discrepância).
  /// Com uma versão do logo em PNG transparente, podes voltar a um tom tipo #040301.
  static const Color loginBackground = Color(0xFF000000);

  /// Azul / roxo / verde da marca (referências do branding).
  static const Color brandBlue = Color(0xFF1E90FF);
  static const Color brandPurple = Color(0xFF9400D3);
  static const Color brandGreen = Color(0xFF32CD32);

  /// Cartão sobre fundo preto — alinhado ao navy/cream da app autenticada.
  static const Color authCard = Color(0xFF141C2A);
  static const Color authCardBorder = Color(0xFFC9A94E);
  static const Color authFieldFill = Color(0xFF1B2C41);
  static const Color authOnCard = Color(0xFFF5F1E8);
  static const Color authOnCardMuted = Color(0xFFADA59A);
  static const Color authHint = Color(0xFF7A756D);

  static const TextStyle authInputTextStyle = TextStyle(
    color: authOnCard,
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.25,
  );
}
