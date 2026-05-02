class AppLockState {
  const AppLockState({
    required this.hydrated,
    required this.pinEnabled,
    required this.biometricPreferred,
    required this.sessionUnlocked,
  });

  final bool hydrated;
  final bool pinEnabled;
  final bool biometricPreferred;
  final bool sessionUnlocked;

  AppLockState copyWith({
    bool? hydrated,
    bool? pinEnabled,
    bool? biometricPreferred,
    bool? sessionUnlocked,
  }) {
    return AppLockState(
      hydrated: hydrated ?? this.hydrated,
      pinEnabled: pinEnabled ?? this.pinEnabled,
      biometricPreferred: biometricPreferred ?? this.biometricPreferred,
      sessionUnlocked: sessionUnlocked ?? this.sessionUnlocked,
    );
  }
}
