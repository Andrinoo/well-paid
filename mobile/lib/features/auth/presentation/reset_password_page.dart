import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/auth_notifier.dart';
import '../domain/password_policy.dart';
import 'widgets/auth_gradient_button.dart';
import 'widgets/auth_shell.dart';

class ResetPasswordPage extends ConsumerStatefulWidget {
  const ResetPasswordPage({super.key, this.initialToken});

  final String? initialToken;

  @override
  ConsumerState<ResetPasswordPage> createState() => _ResetPasswordPageState();
}

class _ResetPasswordPageState extends ConsumerState<ResetPasswordPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _token;
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  bool _busy = false;
  bool _obscure = true;
  bool _obscureConfirm = true;

  @override
  void initState() {
    super.initState();
    _token = TextEditingController(text: widget.initialToken ?? '');
  }

  @override
  void dispose() {
    _token.dispose();
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final l10n = context.l10n;
    try {
      final repo = ref.read(authRepositoryProvider);
      await repo.resetPassword(
        token: _token.text.trim(),
        newPassword: _password.text,
      );
      if (!mounted) return;
      messenger.showSnackBar(
        SnackBar(content: Text(l10n.authResetSuccess)),
      );
      context.go('/login');
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.authResetError)),
      );
    } catch (e, st) {
      debugPrint('[ResetPassword] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AuthShell(
      title: l10n.resetPasswordTitle,
      subtitle: l10n.authResetSubtitle,
      leading: IconButton(
        icon: const Icon(PhosphorIconsRegular.arrowLeft),
        color: WellPaidColors.gold,
        onPressed: _busy ? null : () => context.pop(),
      ),
      formBody: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              l10n.authPasswordPolicyHint,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.authOnCardMuted,
                  ),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _token,
              keyboardType: TextInputType.visiblePassword,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: l10n.authResetTokenLabel,
                hintText: l10n.authResetTokenHint,
              ),
              validator: (v) {
                if (v == null || v.trim().length < 10) {
                  return l10n.authResetTokenError;
                }
                return null;
              },
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _password,
              obscureText: _obscure,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: l10n.authNewPassword,
                suffixIcon: IconButton(
                  tooltip: _obscure ? l10n.authShowPassword : l10n.authHidePassword,
                  style: authFieldSuffixIconButtonStyle(),
                  iconSize: 20,
                  onPressed: () => setState(() => _obscure = !_obscure),
                  icon: Icon(
                    _obscure
                        ? PhosphorIconsRegular.eye
                        : PhosphorIconsRegular.eyeSlash,
                  ),
                ),
              ),
              validator: (v) => validatePasswordRules(v, l10n),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _confirm,
              obscureText: _obscureConfirm,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _busy ? null : _submit(),
              decoration: authFieldDecoration(
                context,
                label: l10n.authConfirmNewPassword,
                suffixIcon: IconButton(
                  tooltip:
                      _obscureConfirm ? l10n.authShowPassword : l10n.authHidePassword,
                  style: authFieldSuffixIconButtonStyle(),
                  iconSize: 20,
                  onPressed: () =>
                      setState(() => _obscureConfirm = !_obscureConfirm),
                  icon: Icon(
                    _obscureConfirm
                        ? PhosphorIconsRegular.eye
                        : PhosphorIconsRegular.eyeSlash,
                  ),
                ),
              ),
              validator: (v) {
                if (v == null || v.isEmpty) {
                  return l10n.authConfirmPasswordRequired;
                }
                if (v != _password.text) {
                  return l10n.authPasswordMismatch;
                }
                return null;
              },
            ),
            const SizedBox(height: 17),
            AuthGradientButton(
              label: l10n.authSaveNewPassword,
              busy: _busy,
              onPressed: _busy ? null : _submit,
            ),
          ],
        ),
      ),
      footer: TextButton(
        style: TextButton.styleFrom(
          foregroundColor: WellPaidColors.gold,
        ),
        onPressed: _busy ? null : () => context.push('/forgot-password'),
        child: Text(l10n.authRequestNewCode),
      ),
    );
  }
}
