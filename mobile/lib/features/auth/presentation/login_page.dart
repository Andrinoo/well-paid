import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/app_release_label.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/network/network_providers.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/auth_notifier.dart';
import '../domain/password_policy.dart';
import 'widgets/auth_gradient_button.dart';
import 'widgets/auth_shell.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _busy = false;
  bool _obscure = true;
  bool _remember = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadSavedCredentials());
  }

  Future<void> _loadSavedCredentials() async {
    final store = ref.read(loginCredentialsStorageProvider);
    final creds = await store.read();
    if (!mounted) return;
    if (creds.email != null && creds.email!.isNotEmpty) {
      setState(() {
        _email.text = creds.email!;
        if (creds.password != null && creds.password!.isNotEmpty) {
          _password.text = creds.password!;
          _remember = true;
        }
      });
    }
  }

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final l10n = context.l10n;
    try {
      final email = _email.text.trim();
      await ref.read(authNotifierProvider.notifier).login(
            email,
            _password.text,
          );
      final credStore = ref.read(loginCredentialsStorageProvider);
      if (_remember) {
        await credStore.save(email: email, password: _password.text);
      } else {
        await credStore.clear();
      }
      if (mounted) context.go('/home');
    } on DioException catch (e, st) {
      logDioException(e, st);
      final msg = messageFromDio(e, l10n) ?? l10n.authLoginError;
      final email = _email.text.trim();
      if (e.response?.statusCode == 403 && email.isNotEmpty) {
        messenger.showSnackBar(
          SnackBar(
            content: Text(msg),
            action: SnackBarAction(
              label: l10n.authVerifyEmailAction,
              onPressed: () {
                context.push(
                  '/verify-email?email=${Uri.encodeComponent(email)}',
                );
              },
            ),
          ),
        );
      } else {
        messenger.showSnackBar(SnackBar(content: Text(msg)));
      }
    } catch (e, st) {
      debugPrint('[Login] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return AuthShell(
      title: l10n.authLoginTitle,
      formBody: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              autofillHints: const [AutofillHints.email],
              decoration: authFieldDecoration(context, label: l10n.authEmail),
              validator: (v) => validateEmailField(v, l10n),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _password,
              obscureText: _obscure,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _busy ? null : _submit(),
              autofillHints: const [AutofillHints.password],
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
              validator: (v) {
                if (v == null || v.isEmpty) return l10n.authPasswordRequired;
                return null;
              },
            ),
            const SizedBox(height: 2),
            CheckboxListTile(
              value: _remember,
              onChanged: _busy
                  ? null
                  : (v) => setState(() => _remember = v ?? false),
              title: Text(
                l10n.authRememberCredentials,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: WellPaidColors.authOnCardMuted,
                      height: 1.25,
                      fontSize: 12.5,
                    ),
              ),
              contentPadding: EdgeInsets.zero,
              dense: true,
              visualDensity: VisualDensity.compact,
              controlAffinity: ListTileControlAffinity.leading,
              activeColor: WellPaidColors.gold,
              checkColor: WellPaidColors.navyDeep,
              side: BorderSide(
                color: WellPaidColors.cream.withValues(alpha: 0.4),
              ),
            ),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                style: TextButton.styleFrom(
                  foregroundColor: WellPaidColors.gold,
                  visualDensity: VisualDensity.compact,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 0),
                ),
                onPressed: _busy ? null : () => context.push('/forgot-password'),
                child: Text(
                  l10n.authForgotPassword,
                  style: const TextStyle(fontSize: 13),
                ),
              ),
            ),
            const SizedBox(height: 4),
            AuthGradientButton(
              label: l10n.authEnter,
              busy: _busy,
              onPressed: _busy ? null : _submit,
            ),
          ],
        ),
      ),
      footer: Column(
        children: [
          Wrap(
            alignment: WrapAlignment.center,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 4,
            runSpacing: 4,
            children: [
              Text(
                l10n.authNoAccountYet,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.authOnCardMuted,
                      fontSize: 13,
                    ),
              ),
              TextButton(
                style: TextButton.styleFrom(
                  foregroundColor: WellPaidColors.gold,
                  visualDensity: VisualDensity.compact,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 0),
                ),
                onPressed: _busy ? null : () => context.push('/register'),
                child: Text(
                  l10n.authCreateAccount,
                  style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: Text(
              l10n.authCopyright,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.authOnCardMuted.withValues(alpha: 0.85),
                    fontSize: 11,
                    height: 1.45,
                  ),
            ),
          ),
          const SizedBox(height: 10),
          SelectableText(
            kAppReleaseLabel,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: WellPaidColors.authOnCardMuted.withValues(alpha: 0.65),
                  fontSize: 10,
                  letterSpacing: 0.2,
                ),
          ),
        ],
      ),
    );
  }
}
