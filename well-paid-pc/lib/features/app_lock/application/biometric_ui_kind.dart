import 'package:flutter/widgets.dart';
import 'package:local_auth/local_auth.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

/// Tipo de sensor disponível para personalizar ícone e texto (Face ID / digital / ambos).
enum AppBiometricUiKind {
  unavailable,
  face,
  fingerprint,
  mixed,
  generic,
}

Future<AppBiometricUiKind> detectBiometricUiKind(LocalAuthentication la) async {
  try {
    final supported = await la.isDeviceSupported();
    if (!supported) return AppBiometricUiKind.unavailable;
    if (!await la.canCheckBiometrics) {
      return AppBiometricUiKind.unavailable;
    }
    final types = await la.getAvailableBiometrics();
    if (types.isEmpty) return AppBiometricUiKind.generic;

    final hasFace = types.contains(BiometricType.face);
    final hasPrint = types.contains(BiometricType.fingerprint) ||
        types.contains(BiometricType.iris);

    if (hasFace && hasPrint) return AppBiometricUiKind.mixed;
    if (hasFace) return AppBiometricUiKind.face;
    if (hasPrint) return AppBiometricUiKind.fingerprint;
    return AppBiometricUiKind.generic;
  } catch (_) {
    return AppBiometricUiKind.unavailable;
  }
}

/// Ícone alinhado ao tipo de sensor (rosto vs digital).
IconData biometricPhosphorIcon(AppBiometricUiKind kind) {
  switch (kind) {
    case AppBiometricUiKind.face:
      return PhosphorIconsRegular.scanSmiley;
    case AppBiometricUiKind.fingerprint:
      return PhosphorIconsRegular.fingerprint;
    case AppBiometricUiKind.mixed:
      return PhosphorIconsRegular.scan;
    case AppBiometricUiKind.generic:
    case AppBiometricUiKind.unavailable:
      return PhosphorIconsRegular.fingerprint;
  }
}
