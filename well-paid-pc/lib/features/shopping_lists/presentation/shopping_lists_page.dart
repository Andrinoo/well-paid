import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/shopping_lists_providers.dart';
import '../domain/shopping_list_models.dart';

class ShoppingListsPage extends ConsumerWidget {
  const ShoppingListsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final async = ref.watch(shoppingListsProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.shoppingListsTitle),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          final repo = ref.read(shoppingListsRepositoryProvider);
          try {
            final created = await repo.createList();
            if (!context.mounted) return;
            ref.invalidate(shoppingListsProvider);
            context.push('/shopping-lists/${created.id}');
          } catch (e) {
            if (!context.mounted) return;
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(
                  messageFromDio(e, l10n) ?? l10n.shoppingListErrorLoad,
                ),
              ),
            );
          }
        },
        icon: const Icon(PhosphorIconsRegular.plus),
        label: Text(l10n.shoppingListsNewList),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              messageFromDio(e, l10n) ?? l10n.shoppingListErrorLoad,
              textAlign: TextAlign.center,
            ),
          ),
        ),
        data: (lists) {
          if (lists.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  l10n.shoppingListsEmpty,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: WellPaidColors.navy.withValues(alpha: 0.75),
                  ),
                ),
              ),
            );
          }
          final drafts = lists.where((e) => e.isDraft).toList();
          final done = lists.where((e) => e.isCompleted).toList();
          return ListView(
            padding: const EdgeInsets.only(bottom: 88),
            children: [
              if (drafts.isNotEmpty) ...[
                _SectionHeader(title: l10n.shoppingListsActiveSection),
                ...drafts.map((s) => _SummaryTile(summary: s)),
              ],
              if (done.isNotEmpty) ...[
                _SectionHeader(title: l10n.shoppingListsHistorySection),
                ...done.map((s) => _SummaryTile(summary: s)),
              ],
            ],
          );
        },
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w700,
          color: WellPaidColors.navy.withValues(alpha: 0.55),
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}

class _SummaryTile extends ConsumerWidget {
  const _SummaryTile({required this.summary});

  final ShoppingListSummary summary;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final title = (summary.title != null && summary.title!.trim().isNotEmpty)
        ? summary.title!.trim()
        : l10n.shoppingListUntitled;
    final subtitle = StringBuffer();
    subtitle.write(l10n.shoppingListItemsCount(summary.itemsCount));
    if (summary.isCompleted && summary.totalCents != null) {
      subtitle.write(' · ${formatBrlFromCents(summary.totalCents!)}');
    }
    return ListTile(
      title: Text(title),
      subtitle: Text(subtitle.toString()),
      trailing: summary.isDraft
          ? Icon(PhosphorIconsRegular.pencilSimple, color: WellPaidColors.navy.withValues(alpha: 0.45))
          : Icon(PhosphorIconsRegular.checkCircle, color: WellPaidColors.gold.withValues(alpha: 0.9)),
      onTap: () {
        context.push('/shopping-lists/${summary.id}');
      },
    );
  }
}
