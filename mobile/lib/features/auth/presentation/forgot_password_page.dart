import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/auth_notifier.dart';
import '../domain/password_policy.dart';
import 'widgets/auth_gradient_button.dart';
import 'widgets/auth_shell.dart';

class ForgotPasswordPage extends ConsumerStatefulWidget {
  const ForgotPasswordPage({super.key});

  @override
  ConsumerState<ForgotPasswordPage> createState() => _ForgotPasswordPageState();
}

class _ForgotPasswordPageState extends ConsumerState<ForgotPasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _email.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    try {
      final repo = ref.read(authRepositoryProvider);
      final result = await repo.forgotPassword(_email.text.trim());
      if (!mounted) return;

      messenger.showSnackBar(SnackBar(content: Text(result.message)));

      final dev = result.devResetToken;
      if (dev != null && dev.isNotEmpty) {
        await showDialog<void>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Modo desenvolvimento'),
            content: SelectableText(
              'O backend devolveu o token para testes. Guarde-o e use no ecrã seguinte.\n\n$dev',
            ),
            actions: [
              TextButton(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: dev));
                  Navigator.pop(ctx);
                  messenger.showSnackBar(
                    const SnackBar(content: Text('Token copiado')),
                  );
                },
                child: const Text('Copiar'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('OK'),
              ),
            ],
          ),
        );
        if (mounted) {
          context.push('/reset-password', extra: dev);
        }
      } else {
        if (mounted) context.push('/reset-password');
      }
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e) ?? 'Erro ao enviar pedido')),
      );
    } catch (e, st) {
      debugPrint('[ForgotPassword] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AuthShell(
      title: 'Recuperar senha',
      subtitle: 'Indica o teu e-mail para receberes instruções.',
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
              'Enviaremos um código para redefinires a senha na app. '
              'Se não vês o e-mail, verifica o spam.',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.authOnCardMuted,
                  ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.done,
              onFieldSubmitted: (_) => _busy ? null : _submit(),
              decoration: authFieldDecoration(context, label: 'E-mail'),
              validator: validateEmailField,
            ),
            const SizedBox(height: 17),
            AuthGradientButton(
              label: 'Enviar instruções',
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
        child: const Text('Voltar ao login'),
      ),
    );
  }
}
