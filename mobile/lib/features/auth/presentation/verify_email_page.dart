import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
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

class VerifyEmailPage extends ConsumerStatefulWidget {
  const VerifyEmailPage({
    super.key,
    this.initialEmail,
    this.initialToken,
  });

  final String? initialEmail;
  final String? initialToken;

  @override
  ConsumerState<VerifyEmailPage> createState() => _VerifyEmailPageState();
}

class _VerifyEmailPageState extends ConsumerState<VerifyEmailPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _email;
  final _code = TextEditingController();
  bool _busy = false;
  bool _autoFromLink = false;
  String? _autoError;

  @override
  void initState() {
    super.initState();
    _email = TextEditingController(text: widget.initialEmail ?? '');
    final t = widget.initialToken?.trim();
    if (t != null && t.length >= 8) {
      _autoFromLink = true;
      WidgetsBinding.instance.addPostFrameCallback((_) => _verifyWithToken(t));
    }
  }

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    super.dispose();
  }

  Future<void> _verifyWithToken(String token) async {
    if (!mounted) return;
    setState(() {
      _busy = true;
      _autoError = null;
    });
    final messenger = ScaffoldMessenger.of(context);
    final l10n = context.l10n;
    try {
      final repo = ref.read(authRepositoryProvider);
      final pair = await repo.verifyEmail(token: token);
      if (!mounted) return;
      await ref.read(authNotifierProvider.notifier).completeEmailVerification(pair);
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(l10n.authVerifyEmailSuccess)));
      context.go('/home');
    } on DioException catch (e, st) {
      logDioException(e, st);
      if (!mounted) return;
      setState(() {
        _autoFromLink = false;
        _autoError = messageFromDio(e, l10n) ?? l10n.authVerifyEmailError;
      });
    } catch (e, st) {
      debugPrint('[VerifyEmail] $e\n$st');
      if (!mounted) return;
      setState(() {
        _autoFromLink = false;
        _autoError = e.toString();
      });
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _submitCode() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final l10n = context.l10n;
    try {
      final repo = ref.read(authRepositoryProvider);
      final pair = await repo.verifyEmail(
        email: _email.text.trim(),
        code: _code.text.trim(),
      );
      if (!mounted) return;
      await ref.read(authNotifierProvider.notifier).completeEmailVerification(pair);
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(l10n.authVerifyEmailSuccess)));
      context.go('/home');
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.authVerifyEmailError)),
      );
    } catch (e, st) {
      debugPrint('[VerifyEmail] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _resend() async {
    final email = _email.text.trim();
    if (email.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(context.l10n.authEmailRequired)),
      );
      return;
    }
    setState(() => _busy = true);
    final messenger = ScaffoldMessenger.of(context);
    final l10n = context.l10n;
    try {
      final repo = ref.read(authRepositoryProvider);
      final r = await repo.resendVerification(email);
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(r.message)));
      final devT = r.devVerificationToken;
      final devC = r.devVerificationCode;
      if (devT != null && devT.isNotEmpty && devC != null && devC.isNotEmpty) {
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
    } on DioException catch (e, st) {
      logDioException(e, st);
      messenger.showSnackBar(
        SnackBar(content: Text(messageFromDio(e, l10n) ?? l10n.authResendVerificationError)),
      );
    } catch (e, st) {
      debugPrint('[ResendVerification] $e\n$st');
      messenger.showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    if (_autoFromLink && _busy) {
      return AuthShell(
        title: l10n.authVerifyEmailTitle,
        subtitle: l10n.authVerifyEmailFromLink,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          color: WellPaidColors.brandBlue,
          onPressed: () => setState(() {
            _autoFromLink = false;
            _busy = false;
          }),
        ),
        formBody: const Center(
          child: Padding(
            padding: EdgeInsets.only(top: 48),
            child: CircularProgressIndicator(),
          ),
        ),
      );
    }

    return AuthShell(
      title: l10n.authVerifyEmailTitle,
      subtitle: l10n.authVerifyEmailSubtitle,
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
            if (_autoError != null) ...[
              Text(
                _autoError!,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.orangeAccent,
                    ),
              ),
              const SizedBox(height: 12),
            ],
            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              decoration: authFieldDecoration(context, label: l10n.authEmail),
              validator: (v) => validateEmailField(v, l10n),
            ),
            const SizedBox(height: 11),
            TextFormField(
              controller: _code,
              keyboardType: TextInputType.number,
              textInputAction: TextInputAction.done,
              maxLength: 6,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              onFieldSubmitted: (_) => _busy ? null : _submitCode(),
              decoration: authFieldDecoration(
                context,
                label: l10n.authVerificationCodeLabel,
                hintText: l10n.authVerificationCodeHint,
              ),
              validator: (v) {
                if (v == null || v.trim().length != 6) {
                  return l10n.authVerificationCodeError;
                }
                return null;
              },
            ),
            const SizedBox(height: 17),
            AuthGradientButton(
              label: l10n.authVerifyEmailButton,
              busy: _busy,
              onPressed: _busy ? null : _submitCode,
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: _busy ? null : _resend,
              child: Text(l10n.authResendVerification),
            ),
          ],
        ),
      ),
      footer: TextButton(
        style: TextButton.styleFrom(
          foregroundColor: WellPaidColors.brandBlue,
        ),
        onPressed: _busy ? null : () => context.go('/login'),
        child: Text(l10n.authBackToLogin),
      ),
    );
  }
}
