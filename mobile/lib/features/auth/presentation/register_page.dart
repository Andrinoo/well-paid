import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/auth_notifier.dart';
import '../domain/password_policy.dart';
import 'widgets/auth_gradient_button.dart';
import 'widgets/auth_shell.dart';

class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  bool _busy = false;
  bool _obscure = true;
  bool _obscureConfirm = true;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _phone.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final l10n = context.l10n;
    try {
      final r = await ref.read(authNotifierProvider.notifier).register(
            email: _email.text.trim(),
            password: _password.text,
            fullName: _name.text.trim().isEmpty ? null : _name.text.trim(),
            phone: _phone.text.trim().isEmpty ? null : _phone.text.trim(),
          );
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(r.message)));
      final devT = r.devVerificationToken;
      final devC = r.devVerificationCode;
      if (devT != null &&
          devT.isNotEmpty &&
          devC != null &&
          devC.isNotEmpty) {
        await showDialog<void>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(l10n.authDevModeTitle),
            content: SelectableText(l10n.authDevVerificationHint(devT, devC)),
            actions: [
              TextButton(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: '$devT\n$devC'));
                  Navigator.pop(ctx);
                  messenger.showSnackBar(
                    SnackBar(content: Text(l10n.tokenCopied)),
                  );
                },
                child: Text(l10n.copy),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(ctx),
                child: Text(l10n.ok),
              ),
            ],
          ),
        );
      }
      if (!mounted) return;
      var loc =
          '/verify-email?email=${Uri.encodeComponent(r.email)}';
      if (devT != null && devT.isNotEmpty) {
        loc += '&token=${Uri.encodeComponent(devT)}';
      }
      context.go(loc);
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.authRegisterError)),
      );
    } catch (e, st) {
      debugPrint('[Register] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AuthShell(
      title: l10n.authRegisterTitle,
      subtitle: l10n.authRegisterSubtitle,
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
              controller: _name,
              textCapitalization: TextCapitalization.words,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: l10n.authNameOptional,
              ),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(context, label: l10n.authEmail),
              validator: (v) => validateEmailField(v, l10n),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: l10n.authPhoneOptional,
              ),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _password,
              obscureText: _obscure,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: l10n.authPassword,
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
              controller: _confirmPassword,
              obscureText: _obscureConfirm,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _busy ? null : _submit(),
              decoration: authFieldDecoration(
                context,
                label: l10n.authConfirmPassword,
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
              label: l10n.authRegisterButton,
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
        onPressed: _busy ? null : () => context.pop(),
        child: Text(l10n.authAlreadyHaveAccount),
      ),
    );
  }
}
