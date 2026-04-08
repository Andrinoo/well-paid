class AuthState {
  const AuthState({
    required this.hydrated,
    this.accessToken,
    this.refreshToken,
  });

  final bool hydrated;
  final String? accessToken;
  final String? refreshToken;

  bool get isAuthenticated =>
      accessToken != null && accessToken!.isNotEmpty;
}
