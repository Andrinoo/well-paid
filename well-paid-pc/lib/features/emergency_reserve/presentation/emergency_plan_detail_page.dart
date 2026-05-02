import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phosphor_flutter/phosphor_flutter.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/emergency_plans_providers.dart';
import '../data/emergency_plans_repository.dart';

class EmergencyPlanDetailPage extends ConsumerWidget {
  const EmergencyPlanDetailPage({super.key, required this.planId});

  final String planId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final plansAsync = ref.watch(emergencyPlansListProvider);
    final monthsAsync = ref.watch(emergencyPlanMonthsProvider(planId));

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(PhosphorIconsRegular.arrowLeft),
            onPressed: () => context.pop(),
          ),
          title: Text(l10n.pcEmergencyPlanTitle),
          bottom: TabBar(
            tabs: [
              Tab(text: l10n.emergencyReserveTitle),
              Tab(text: l10n.pcPlanMonthsTitle),
            ],
          ),
        ),
        body: plansAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => Center(
            child: Text(messageFromDio(e, l10n) ?? '$e'),
          ),
          data: (plans) {
            EmergencyReservePlanItem? plan;
            for (final p in plans) {
              if (p.id == planId) {
                plan = p;
                break;
              }
            }
            if (plan == null) {
              return Center(child: Text(l10n.pcPlansEmpty));
            }
            return TabBarView(
              children: [
                ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    Text(
                      plan.title,
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: WellPaidColors.navy,
                          ),
                    ),
                    const SizedBox(height: 12),
                    _infoRow('Estado', plan.status),
                    _infoRow('Saldo', formatBrlFromCents(plan.balanceCents)),
                    _infoRow(
                      'Meta mensal',
                      formatBrlFromCents(plan.monthlyTargetCents),
                    ),
                    if (plan.paceStatus != null)
                      _infoRow('Ritmo', plan.paceStatus!),
                    if (plan.planDurationMonths != null)
                      _infoRow(
                        'Duração (meses)',
                        '${plan.planDurationMonths}',
                      ),
                  ],
                ),
                monthsAsync.when(
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Center(
                    child: Text(messageFromDio(e, l10n) ?? '$e'),
                  ),
                  data: (rows) {
                    if (rows.isEmpty) {
                      return Center(
                        child: Text(l10n.emergencyReserveAccrualListEmpty),
                      );
                    }
                    return ListView.builder(
                      padding: const EdgeInsets.all(12),
                      itemCount: rows.length,
                      itemBuilder: (context, i) {
                        final r = rows[i];
                        return Card(
                          child: ListTile(
                            title: Text('${r.month}/${r.year}'),
                            subtitle: Text(
                              'Esperado ${formatBrlFromCents(r.expectedCents)} · '
                              'Depositado ${formatBrlFromCents(r.depositedCents)} · '
                              '${r.paceStatus}',
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  static Widget _infoRow(String k, String v) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              k,
              style: TextStyle(
                color: WellPaidColors.navy.withValues(alpha: 0.65),
              ),
            ),
          ),
          Expanded(child: Text(v)),
        ],
      ),
    );
  }
}
