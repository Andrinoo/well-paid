import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/dio_client.dart';
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
    try {
      await ref.read(authNotifierProvider.notifier).login(
            _email.text.trim(),
            _password.text,
          );
      if (mounted) context.go('/home');
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e) ?? 'Erro ao entrar')),
      );
    } catch (e, st) {
      debugPrint('[Login] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AuthShell(
      title: 'Entrar na conta',
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
              decoration: authFieldDecoration(context, label: 'E-mail'),
              validator: validateEmailField,
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
                label: 'Senha',
                suffixIcon: IconButton(
                  tooltip: _obscure ? 'Mostrar senha' : 'Ocultar senha',
                  style: authFieldSuffixIconButtonStyle(),
                  iconSize: 20,
                  onPressed: () => setState(() => _obscure = !_obscure),
                  icon: Icon(
                    _obscure
                        ? Icons.visibility_outlined
                        : Icons.visibility_off_outlined,
                  ),
                ),
              ),
              validator: (v) {
                if (v == null || v.isEmpty) return 'Informe a senha';
                return null;
              },
            ),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                style: TextButton.styleFrom(
                  foregroundColor: WellPaidColors.brandBlue,
                  visualDensity: VisualDensity.compact,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 0),
                ),
                onPressed: _busy ? null : () => context.push('/forgot-password'),
                child: const Text(
                  'Esqueceu a senha?',
                  style: TextStyle(fontSize: 13),
                ),
              ),
            ),
            const SizedBox(height: 4),
            AuthGradientButton(
              label: 'Entrar',
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
                'Ainda sem conta?',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: WellPaidColors.authOnCardMuted,
                      fontSize: 13,
                    ),
              ),
              TextButton(
                style: TextButton.styleFrom(
                  foregroundColor: WellPaidColors.brandBlue,
                  visualDensity: VisualDensity.compact,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 0),
                ),
                onPressed: _busy ? null : () => context.push('/register'),
                child: const Text(
                  'Criar conta',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: Text(
              'Copyright © 2026 Andrino Cabral. All rights reserved.',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.authOnCardMuted.withValues(alpha: 0.85),
                    fontSize: 11,
                    height: 1.45,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}
