import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
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
    final f = ref.watch(incomeListFiltersProvider);
    final async = ref.watch(incomesListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Proventos'),
        actions: [
          IconButton(
            tooltip: 'Atualizar',
            onPressed: () {
              ref.invalidate(incomesListProvider);
              ref.invalidate(dashboardOverviewProvider);
            },
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/incomes/new'),
        icon: const Icon(Icons.add),
        label: const Text('Novo'),
        backgroundColor: WellPaidColors.gold,
        foregroundColor: WellPaidColors.navy,
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
                    tooltip: 'Mês anterior',
                    onPressed: () => _shiftMonth(-1),
                    icon: const Icon(Icons.chevron_left),
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
                    tooltip: 'Próximo mês',
                    onPressed: () => _shiftMonth(1),
                    icon: const Icon(Icons.chevron_right),
                    color: WellPaidColors.navy,
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Text(
              'Valores em reais com centavos exactos; competência pelo dia do provento.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.62),
                    height: 1.35,
                  ),
            ),
          ),
          Expanded(
            child: async.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    messageFromDio(e) ?? 'Erro ao carregar.',
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              data: (items) {
                if (items.isEmpty) {
                  return ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    children: const [
                      SizedBox(height: 48),
                      Center(child: Text('Nenhum provento neste mês.')),
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
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 88),
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
                          : '${item.categoryName} · Família',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.65),
                          ),
                    ),
                    Text(
                      'Data ${dmY(item.incomeDate)}',
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
