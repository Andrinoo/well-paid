import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../dashboard/application/dashboard_providers.dart';
import '../application/expenses_providers.dart';
import '../domain/expense_item.dart';

class ExpenseListPage extends ConsumerStatefulWidget {
  const ExpenseListPage({super.key, this.initialStatus});

  final String? initialStatus;

  @override
  ConsumerState<ExpenseListPage> createState() => _ExpenseListPageState();
}

class _ExpenseListPageState extends ConsumerState<ExpenseListPage> {
  @override
  void initState() {
    super.initState();
    final s = widget.initialStatus;
    if (s == 'pending' || s == 'paid') {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        final dash = ref.read(dashboardPeriodProvider);
        ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
          year: dash.year,
          month: dash.month,
          status: s,
        );
      });
    }
  }

  void _shiftMonth(int delta) {
    final f = ref.read(expenseListFiltersProvider);
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
    ref.read(expenseListFiltersProvider.notifier).state = ExpenseListFilters(
      year: y,
      month: m,
      status: f.status,
    );
  }

  Future<void> _pay(ExpenseItem e) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      await ref.read(expensesRepositoryProvider).payExpense(e.id);
      ref.invalidate(expensesListProvider);
      ref.invalidate(dashboardOverviewProvider);
      if (mounted) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Despesa marcada como paga.')),
        );
      }
    } catch (err) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(content: Text(messageFromDio(err) ?? 'Erro ao pagar.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final f = ref.watch(expenseListFiltersProvider);
    final async = ref.watch(expensesListProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Despesas'),
        actions: [
          IconButton(
            tooltip: 'Atualizar',
            onPressed: () => ref.invalidate(expensesListProvider),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/expenses/new'),
        icon: const Icon(Icons.add),
        label: const Text('Nova'),
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
                        onPressed: () => context.push('/expenses/new'),
                        icon: const Icon(Icons.add_circle_outline),
                        label: const Text('Nova despesa'),
                        style: FilledButton.styleFrom(
                          foregroundColor: WellPaidColors.navy,
                        ),
                      ),
                    ),
                    IconButton(
                      tooltip: 'Atualizar lista',
                      onPressed: () => ref.invalidate(expensesListProvider),
                      icon: const Icon(Icons.refresh),
                      color: WellPaidColors.navy,
                    ),
                  ],
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Wrap(
              spacing: 8,
              children: [
                FilterChip(
                  label: const Text('Todas'),
                  selected: f.status == null,
                  onSelected: (_) {
                    ref.read(expenseListFiltersProvider.notifier).state =
                        ExpenseListFilters(
                      year: f.year,
                      month: f.month,
                    );
                  },
                ),
                FilterChip(
                  label: const Text('Pendentes'),
                  selected: f.status == 'pending',
                  onSelected: (_) {
                    ref.read(expenseListFiltersProvider.notifier).state =
                        ExpenseListFilters(
                      year: f.year,
                      month: f.month,
                      status: 'pending',
                    );
                  },
                ),
                FilterChip(
                  label: const Text('Pagas'),
                  selected: f.status == 'paid',
                  onSelected: (_) {
                    ref.read(expenseListFiltersProvider.notifier).state =
                        ExpenseListFilters(
                      year: f.year,
                      month: f.month,
                      status: 'paid',
                    );
                  },
                ),
              ],
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
                      Center(child: Text('Nenhuma despesa neste filtro.')),
                    ],
                  );
                }
                return RefreshIndicator(
                  color: WellPaidColors.navy,
                  onRefresh: () async {
                    ref.invalidate(expensesListProvider);
                    await ref.read(expensesListProvider.future);
                  },
                  child: ListView.separated(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 88),
                    itemCount: items.length,
                    separatorBuilder: (context, _) => const Divider(height: 1),
                    itemBuilder: (context, i) {
                      final e = items[i];
                      return _ExpenseTile(
                        item: e,
                        onTap: () => context.push('/expenses/${e.id}'),
                        onPay: e.isPending ? () => _pay(e) : null,
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

class _ExpenseTile extends StatelessWidget {
  const _ExpenseTile({
    required this.item,
    required this.onTap,
    this.onPay,
  });

  final ExpenseItem item;
  final VoidCallback onTap;
  final VoidCallback? onPay;

  String _dmY(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label:
          '${item.description}, ${formatBrlFromCents(item.amountCents)}, ${item.status}',
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
                      item.categoryName,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.65),
                          ),
                    ),
                    if (item.isInstallmentPlan || item.recurringLabelPt != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Wrap(
                          spacing: 6,
                          runSpacing: 4,
                          children: [
                            if (item.isInstallmentPlan)
                              Chip(
                                label: Text(
                                  'Parcela ${item.installmentNumber}/'
                                  '${item.installmentTotal}',
                                ),
                                visualDensity: VisualDensity.compact,
                                padding: EdgeInsets.zero,
                                labelStyle: const TextStyle(fontSize: 11),
                              ),
                            if (item.recurringLabelPt != null)
                              Chip(
                                label: Text(
                                  item.recurringLabelPt!,
                                  style: const TextStyle(fontSize: 11),
                                ),
                                visualDensity: VisualDensity.compact,
                                padding: EdgeInsets.zero,
                              ),
                          ],
                        ),
                      ),
                    Text(
                      'Data ${_dmY(item.expenseDate)} · ${item.isPending ? 'Pendente' : 'Paga'}',
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                            color: WellPaidColors.navy.withValues(alpha: 0.55),
                          ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    formatBrlFromCents(item.amountCents),
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: WellPaidColors.navy,
                        ),
                  ),
                  if (onPay != null)
                    TextButton(
                      onPressed: onPay,
                      child: const Text('Pagar'),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
