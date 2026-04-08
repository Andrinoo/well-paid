import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/well_paid_colors.dart';
import '../../../family/application/family_providers.dart';

/// Partilha na família (Ordems §4.5) — só com ≥2 membros.
class ExpenseShareFormSection extends ConsumerWidget {
  const ExpenseShareFormSection({
    super.key,
    required this.isShared,
    required this.sharedWithUserId,
    required this.onSharedChanged,
    required this.onPeerChanged,
    this.enabled = true,
  });

  final bool isShared;
  final String? sharedWithUserId;
  final ValueChanged<bool> onSharedChanged;
  final ValueChanged<String?> onPeerChanged;
  final bool enabled;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(familyMeProvider);
    return async.when(
      loading: () => const SizedBox.shrink(),
      error: (error, stackTrace) => const SizedBox.shrink(),
      data: (me) {
        final others =
            me.family?.members.where((m) => !m.isSelf).toList() ?? [];
        final canShare = others.isNotEmpty;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Partilhar na família'),
              subtitle: Text(
                canShare
                    ? 'Visível para o agregado; podes indicar com quem dividir.'
                    : 'Junta-te a uma família (convite) para usar partilha.',
                style: TextStyle(
                  fontSize: 12,
                  color: WellPaidColors.navy.withValues(alpha: 0.65),
                ),
              ),
              value: isShared,
              onChanged: (enabled && canShare)
                  ? (v) {
                      onSharedChanged(v);
                      if (!v) onPeerChanged(null);
                    }
                  : null,
            ),
            if (isShared && canShare) ...[
              const SizedBox(height: 4),
              DropdownButtonFormField<String?>(
                decoration: const InputDecoration(
                  labelText: 'Dividir com',
                ),
                // ignore: deprecated_member_use
                value: sharedWithUserId,
                items: [
                  const DropdownMenuItem<String?>(
                    value: null,
                    child: Text('Toda a família'),
                  ),
                  ...others.map(
                    (m) => DropdownMenuItem<String?>(
                      value: m.userId,
                      child: Text(
                        m.fullName?.trim().isNotEmpty == true
                            ? m.fullName!.trim()
                            : m.email,
                      ),
                    ),
                  ),
                ],
                onChanged: enabled ? onPeerChanged : null,
              ),
            ],
          ],
        );
      },
    );
  }
}
