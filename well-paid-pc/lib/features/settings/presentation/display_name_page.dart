import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../profile/application/user_profile_providers.dart';

class DisplayNamePage extends ConsumerStatefulWidget {
  const DisplayNamePage({super.key});

  @override
  ConsumerState<DisplayNamePage> createState() => _DisplayNamePageState();
}

class _DisplayNamePageState extends ConsumerState<DisplayNamePage> {
  final _ctrl = TextEditingController();
  bool _busy = false;
  bool _seeded = false;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final async = ref.watch(userMeProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.pcDisplayNameTitle),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Text(messageFromDio(e, l10n) ?? '$e'),
        ),
        data: (me) {
          if (!_seeded) {
            _ctrl.text = me.displayName ?? '';
            _seeded = true;
          }
          return Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(me.email, style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 16),
                TextField(
                  controller: _ctrl,
                  decoration: InputDecoration(
                    labelText: l10n.pcDisplayNameTitle,
                  ),
                ),
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _busy
                      ? null
                      : () async {
                          setState(() => _busy = true);
                          try {
                            await ref
                                .read(userProfileRepositoryProvider)
                                .patchProfile(
                                  displayName: _ctrl.text.trim().isEmpty
                                      ? null
                                      : _ctrl.text.trim(),
                                );
                            ref.invalidate(userMeProvider);
                            if (!context.mounted) return;
                            context.pop();
                          } catch (e) {
                            if (!context.mounted) return;
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(
                                  messageFromDio(e, l10n) ?? '$e',
                                ),
                              ),
                            );
                          } finally {
                            if (mounted) setState(() => _busy = false);
                          }
                        },
                  child: _busy
                      ? const SizedBox(
                          height: 22,
                          width: 22,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Text(l10n.emergencyReserveSave),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
