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
    try {
      final repo = ref.read(authRepositoryProvider);
      await repo.resetPassword(
        token: _token.text.trim(),
        newPassword: _password.text,
      );
      if (!mounted) return;
      messenger.showSnackBar(
        const SnackBar(content: Text('Senha atualizada. Pode entrar.')),
      );
      context.go('/login');
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e) ?? 'Erro ao redefinir')),
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
    return AuthShell(
      title: 'Redefinir senha',
      subtitle: 'Cola o código que recebeste por e-mail.',
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
              controller: _token,
              keyboardType: TextInputType.visiblePassword,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(
                context,
                label: 'Código de recuperação',
                hintText: 'Cole o token completo',
              ),
              validator: (v) {
                if (v == null || v.trim().length < 10) {
                  return 'Cole o código recebido por e-mail';
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
                label: 'Nova senha',
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
              controller: _confirm,
              obscureText: _obscureConfirm,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _busy ? null : _submit(),
              decoration: authFieldDecoration(
                context,
                label: 'Confirmar nova senha',
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
              label: 'Guardar nova senha',
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
        onPressed: _busy ? null : () => context.push('/forgot-password'),
        child: const Text('Pedir novo código'),
      ),
    );
  }
}
