import 'package:flutter/material.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/incomes_providers.dart';
import '../domain/income_item.dart';

class IncomeListPage extends ConsumerStatefulWidget {
  const IncomeListPage({super.key});

  @override
  ConsumerState<IncomeListPage> createState() => _IncomeListPageState();
}

class _IncomeListPageState extends ConsumerState<IncomeListPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final p = ref.read(dashboardPeriodProvider);
      ref.read(incomeListFiltersProvider.notifier).state = IncomeListFilters(
        year: p.year,
        month: p.month,
      );
    });
  }

  void _shiftMonth(int delta) {
    final f = ref.read(incomeListFiltersProvider);
    var y = f.year;
    var m = f.month + delta;
    while (m > 12) {
      m -= 12;
      y++;
    }
    while (m < 1) {
      m += 12;
      y--;
    }
    ref.read(incomeListFiltersProvider.notifier).state = IncomeListFilters(
      year: y,
      month: m,
    );
  }

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final f = ref.watch(incomeListFiltersProvider);
    final async = ref.watch(incomesListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: Navigator.of(context).canPop()
            ? IconButton(
                icon: const Icon(PhosphorIconsRegular.arrowLeft),
                onPressed: () => context.pop(),
              )
            : null,
        title: Text(l10n.incomesTitle),
        actions: [
          IconButton(
            tooltip: l10n.incomesRefresh,
            onPressed: () {
              ref.invalidate(incomesListProvider);
              ref.invalidate(dashboardOverviewProvider);
            },
            icon: const Icon(PhosphorIconsRegular.arrowsClockwise),
          ),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Material(
            color: WellPaidColors.creamMuted.withValues(alpha: 0.6),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    tooltip: l10n.periodPrevMonth,
                    onPressed: () => _shiftMonth(-1),
                    icon: const Icon(PhosphorIconsRegular.caretLeft),
                    color: WellPaidColors.navy,
                  ),
                  Text(
                    '${f.month.toString().padLeft(2, '0')}/${f.year}',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: WellPaidColors.navy,
                        ),
                  ),
                  IconButton(
                    tooltip: l10n.periodNextMonth,
                    onPressed: () => _shiftMonth(1),
                    icon: const Icon(PhosphorIconsRegular.caretRight),
                    color: WellPaidColors.navy,
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 0),
            child: Card(
              elevation: 0,
              color: WellPaidColors.creamMuted.withValues(alpha: 0.9),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: BorderSide(
                  color: WellPaidColors.navy.withValues(alpha: 0.1),
                ),
              ),
              child: Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                child: Row(
                  children: [
                    Expanded(
                      child: FilledButton.tonalIcon(
                        onPressed: () => context.push('/incomes/new'),
                        icon: const Icon(PhosphorIconsRegular.coins),
                        label: Text(l10n.incomesAddLong),
                        style: FilledButton.styleFrom(
                          foregroundColor: WellPaidColors.navy,
                        ),
                      ),
                    ),
                    IconButton(
                      tooltip: l10n.incomesRefreshList,
                      onPressed: () {
                        ref.invalidate(incomesListProvider);
                        ref.invalidate(dashboardOverviewProvider);
                      },
                      icon: const Icon(PhosphorIconsRegular.arrowsClockwise),
                      color: WellPaidColors.navy,
                    ),
                  ],
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
            child: Text(
              l10n.incomesListHint,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.62),
                    height: 1.35,
                  ),
            ),
          ),
          Expanded(
            child: async.when(
              skipLoadingOnReload: true,
              loading: () => ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                children: [
                  LinearProgressIndicator(
                    minHeight: 3,
                    color: WellPaidColors.gold,
                    backgroundColor: WellPaidColors.navy.withValues(alpha: 0.08),
                  ),
                  const SizedBox(height: 20),
                  ...List.generate(
                    5,
                    (i) => Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: Container(
                        height: 52,
                        decoration: BoxDecoration(
                          color: WellPaidColors.navy.withValues(alpha: 0.06),
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              error: (e, _) => Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    messageFromDio(e, l10n) ?? l10n.incomesLoadError,
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              data: (items) {
                if (items.isEmpty) {
                  return ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    children: [
                      const SizedBox(height: 48),
                      Center(child: Text(l10n.incomesEmpty)),
                    ],
                  );
                }
                return RefreshIndicator(
                  color: WellPaidColors.navy,
                  onRefresh: () async {
                    ref.invalidate(incomesListProvider);
                    await ref.read(incomesListProvider.future);
                  },
                  child: ListView.separated(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    itemCount: items.length,
                    separatorBuilder: (context, index) => const Divider(height: 1),
                    itemBuilder: (context, i) {
                      final item = items[i];
                      return _IncomeTile(
                        item: item,
                        dmY: _dmY,
                        onTap: () => context.push('/incomes/${item.id}'),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _IncomeTile extends StatelessWidget {
  const _IncomeTile({
    required this.item,
    required this.dmY,
    required this.onTap,
  });

  final IncomeItem item;
  final String Function(DateTime d) dmY;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Semantics(
      label: '${item.description}, ${formatBrlFromCents(item.amountCents)}',
      button: true,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.description,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                            fontWeight: FontWeight.w600,
                            color: WellPaidColors.navy,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      item.isMine
                          ? item.categoryName
                          : l10n.expenseTileFamilyCategory(item.categoryName),
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.65),
                          ),
                    ),
                    Text(
                      l10n.incomeTileDateLine(dmY(item.incomeDate)),
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.55),
                          ),
                    ),
                  ],
                ),
              ),
              Text(
                formatBrlFromCents(item.amountCents),
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: const Color(0xFF2E7D32),
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
