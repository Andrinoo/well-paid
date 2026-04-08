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
    try {
      await ref.read(authNotifierProvider.notifier).register(
            email: _email.text.trim(),
            password: _password.text,
            fullName: _name.text.trim().isEmpty ? null : _name.text.trim(),
            phone: _phone.text.trim().isEmpty ? null : _phone.text.trim(),
          );
      if (mounted) context.go('/home');
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e) ?? 'Erro ao registar')),
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
    return AuthShell(
      title: 'Criar conta',
      subtitle: 'Começa em poucos passos.',
      leading: IconButton(
        icon: const Icon(Icons.arrow_back_rounded),
        color: WellPaidColors.brandBlue,
        onPressed: _busy ? null : () => context.pop(),
      ),
      formBody: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              passwordPolicyHint,
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
                label: 'Nome (opcional)',
              ),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(context, label: 'E-mail'),
              validator: validateEmailField,
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: 'Telemóvel (opcional)',
              ),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _password,
              obscureText: _obscure,
              textInputAction: TextInputAction.next,
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
              validator: validatePasswordRules,
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _confirmPassword,
              obscureText: _obscureConfirm,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _busy ? null : _submit(),
              decoration: authFieldDecoration(
                context,
                label: 'Confirmar senha',
                suffixIcon: IconButton(
                  tooltip: _obscureConfirm ? 'Mostrar senha' : 'Ocultar senha',
                  style: authFieldSuffixIconButtonStyle(),
                  iconSize: 20,
                  onPressed: () =>
                      setState(() => _obscureConfirm = !_obscureConfirm),
                  icon: Icon(
                    _obscureConfirm
                        ? Icons.visibility_outlined
                        : Icons.visibility_off_outlined,
                  ),
                ),
              ),
              validator: (v) {
                if (v == null || v.isEmpty) {
                  return 'Confirme a senha';
                }
                if (v != _password.text) {
                  return 'As senhas não coincidem';
                }
                return null;
              },
            ),
            const SizedBox(height: 17),
            AuthGradientButton(
              label: 'Registar',
              busy: _busy,
              onPressed: _busy ? null : _submit,
            ),
          ],
        ),
      ),
      footer: TextButton(
        style: TextButton.styleFrom(
          foregroundColor: WellPaidColors.brandBlue,
        ),
        onPressed: _busy ? null : () => context.pop(),
        child: const Text('Já tenho conta — entrar'),
      ),
    );
  }
}
