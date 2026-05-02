import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/announcements_providers.dart';

class AnnouncementsPage extends ConsumerWidget {
  const AnnouncementsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(announcementsListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(PhosphorIconsRegular.arrowLeft),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.pcNavAnnouncements),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(messageFromDio(e, l10n) ?? '$e'),
          ),
        ),
        data: (items) {
          if (items.isEmpty) {
            return Center(child: Text(l10n.pcAnnouncementsEmpty));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: items.length,
            separatorBuilder: (_, __) => const SizedBox(height: 12),
            itemBuilder: (context, i) {
              final a = items[i];
              return Card(
                color: WellPaidColors.creamMuted.withValues(alpha: 0.9),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              a.title,
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                                fontSize: 16,
                              ),
                            ),
                          ),
                          if (a.userReadAt == null)
                            Icon(
                              PhosphorIconsRegular.circle,
                              size: 10,
                              color: WellPaidColors.gold,
                            ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        a.body,
                        style: TextStyle(
                          color: WellPaidColors.navy.withValues(alpha: 0.85),
                          height: 1.35,
                        ),
                      ),
                      if (a.ctaUrl != null && a.ctaUrl!.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        SelectableText(
                          a.ctaUrl!,
                          style: TextStyle(
                            color: WellPaidColors.navy.withValues(alpha: 0.65),
                            fontSize: 13,
                          ),
                        ),
                      ],
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          TextButton.icon(
                            onPressed: () async {
                              try {
                                await ref
                                    .read(announcementsRepositoryProvider)
                                    .markRead(a.id);
                                ref.invalidate(announcementsListProvider);
                              } catch (e) {
                                if (!context.mounted) return;
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      messageFromDio(e, l10n) ?? '$e',
                                    ),
                                  ),
                                );
                              }
                            },
                            icon: const Icon(PhosphorIconsRegular.check, size: 18),
                            label: const Text('Lido'),
                          ),
                          TextButton.icon(
                            onPressed: () async {
                              try {
                                await ref
                                    .read(announcementsRepositoryProvider)
                                    .hide(a.id);
                                ref.invalidate(announcementsListProvider);
                              } catch (e) {
                                if (!context.mounted) return;
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      messageFromDio(e, l10n) ?? '$e',
                                    ),
                                  ),
                                );
                              }
                            },
                            icon: const Icon(PhosphorIconsRegular.eyeSlash, size: 18),
                            label: const Text('Ocultar'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
