import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/format/brl_cents.dart';
import '../../../core/l10n/context_l10n.dart';
import '../../expenses/presentation/pay_expense_flow.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../domain/dashboard_overview.dart';
import 'category_donut_chart.dart';
import 'dashboard_cashflow_chart_card.dart';
import 'due_urgency.dart';

/// Dashboard: carrossel de destaques no topo + secções expansíveis.
class DashboardScrollContent extends ConsumerStatefulWidget {
  const DashboardScrollContent({super.key, required this.data});

  final DashboardOverview data;

  @override
  ConsumerState<DashboardScrollContent> createState() => _DashboardScrollContentState();
}

class _DashboardScrollContentState extends ConsumerState<DashboardScrollContent> {
  String? _expandedSectionKey;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final today = DateTime.now();
    final data = widget.data;
    final reduceMotion = MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    final fadeDuration = Duration(milliseconds: reduceMotion ? 120 : 180);
    final sheetDuration = Duration(milliseconds: reduceMotion ? 160 : 220);

    final sectionContent = <String, Widget>{
      'toPay': Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (data.pendingPreview.isEmpty)
            Text(
              l10n.dashNothingPending,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.65),
                  ),
            )
          else
            ...data.pendingPreview.map(
              (e) => _PendingTile(
                item: e,
                onPay: e.isMine
                    ? () => confirmAndPayExpenseById(
                          context,
                          ref,
                          expenseId: e.id,
                          description: e.description,
                          amountCents: e.amountCents,
                        )
                    : null,
              ),
            ),
          const Divider(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                l10n.dashPendingTotal,
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: WellPaidColors.navy,
                    ),
              ),
              Text(
                formatBrlFromCents(data.pendingTotalCents),
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: WellPaidColors.navy,
                    ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => context.push('/expenses?status=pending'),
              child: Text(l10n.dashSeeAll),
            ),
          ),
        ],
      ),
      'upcoming': Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (data.upcomingDue.isEmpty)
            Text(
              l10n.dashNoUpcomingInNextMonth,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.65),
                  ),
            )
          else
            ...data.upcomingDue.map((e) {
              final due = e.dueDate;
              final u = due != null
                  ? dueUrgencyFor(due, today)
                  : DueUrgency.safe;
              return _UpcomingTile(item: e, urgency: u);
            }),
          const SizedBox(height: 4),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => context.push('/expenses'),
              child: Text(l10n.dashSeeMore),
            ),
          ),
        ],
      ),
      'goals': Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (data.goalsPreview.isEmpty) ...[
            Text(
              l10n.dashNoActiveGoals,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.7),
                  ),
            ),
          ] else
            ...data.goalsPreview.map((g) => _GoalRow(goal: g)),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: () => context.push('/goals'),
            child: Text(l10n.dashSeeGoals),
          ),
        ],
      ),
    };
    final now = DateTime.now();
    final hasCriticalToPay = data.pendingPreview.any((e) {
      final due = e.dueDate;
      if (due == null) return false;
      final u = dueUrgencyFor(due, now);
      return u == DueUrgency.overdue || u == DueUrgency.dueToday;
    });
    final hasAnyToPay = data.pendingPreview.isNotEmpty;

    final baseDashboard = ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
      children: [
        _DashboardHeroCarousel(
          data: widget.data,
          onConfigureReserve: () => context.push('/emergency-reserve'),
        ),
        const SizedBox(height: 14),
        _SectionChartCard(
          title: l10n.dashByCategory,
          icon: Icons.pie_chart_outline,
          accent: WellPaidColors.goldPressed,
          child: CategoryDonutChart(
            categories: data.spendingByCategory,
            monthExpenseTotalCents: data.monthExpenseTotalCents,
            period: data.period,
          ),
        ),
        const SizedBox(height: 10),
        const DashboardCashflowChartCard(),
        const SizedBox(height: 10),
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: [
            _DashboardActionCard(
              title: l10n.dashToPay,
              icon: Icons.credit_card_outlined,
              accent: const Color(0xFFCC8A00),
              badge: hasCriticalToPay
                  ? _ActionBadge.alert
                  : hasAnyToPay
                      ? _ActionBadge.warning
                      : _ActionBadge.none,
              isFeatured: hasAnyToPay,
              onTap: () => _toggleExpanded('toPay'),
            ),
            _DashboardActionCard(
              title: l10n.dashUpcomingDue,
              icon: Icons.event_note_outlined,
              accent: const Color(0xFFB76E00),
              onTap: () => _toggleExpanded('upcoming'),
            ),
            _DashboardActionCard(
              title: l10n.dashGoals,
              icon: Icons.flag_outlined,
              accent: WellPaidColors.navy,
              onTap: () => _toggleExpanded('goals'),
            ),
          ],
        ),
      ],
    );

    return Stack(
      children: [
        IgnorePointer(
          ignoring: _expandedSectionKey != null,
          child: AnimatedOpacity(
            duration: fadeDuration,
            opacity: _expandedSectionKey == null ? 1 : 0.12,
            curve: Curves.easeOutCubic,
            child: baseDashboard,
          ),
        ),
        if (_expandedSectionKey != null)
          Positioned.fill(
            child: GestureDetector(
              onTap: () => setState(() => _expandedSectionKey = null),
              child: ColoredBox(color: Colors.black.withValues(alpha: 0.12)),
            ),
          ),
        if (_expandedSectionKey != null)
          Positioned.fill(
            child: SafeArea(
              minimum: const EdgeInsets.fromLTRB(12, 8, 12, 14),
              child: AnimatedSwitcher(
                duration: sheetDuration,
                switchInCurve: Curves.easeOutCubic,
                switchOutCurve: Curves.easeInCubic,
                transitionBuilder: (child, animation) {
                  return FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: Tween<Offset>(
                        begin: const Offset(0, 0.05),
                        end: Offset.zero,
                      ).animate(animation),
                      child: child,
                    ),
                  );
                },
                child: _ExpandedSectionPanel(
                  key: ValueKey<String>('focus_${_expandedSectionKey!}'),
                  title: _expandedSectionTitle(_expandedSectionKey!),
                  child: sectionContent[_expandedSectionKey!]!,
                  onClose: () => setState(() => _expandedSectionKey = null),
                ),
              ),
            ),
          ),
      ],
    );
  }

  void _toggleExpanded(String key) {
    setState(() {
      _expandedSectionKey = _expandedSectionKey == key ? null : key;
    });
  }

  String _expandedSectionTitle(String key) {
    final l10n = context.l10n;
    return switch (key) {
      'toPay' => l10n.dashToPay,
      'upcoming' => l10n.dashUpcomingDue,
      'goals' => l10n.dashGoals,
      _ => '',
    };
  }
}

/// Resumo do mês e reserva em [PageView] horizontal com indicadores.
class _DashboardHeroCarousel extends StatefulWidget {
  const _DashboardHeroCarousel({
    required this.data,
    required this.onConfigureReserve,
  });

  final DashboardOverview data;
  final VoidCallback onConfigureReserve;

  @override
  State<_DashboardHeroCarousel> createState() => _DashboardHeroCarouselState();
}

class _DashboardHeroCarouselState extends State<_DashboardHeroCarousel> {
  late final PageController _pageController;
  int _pageIndex = 0;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(viewportFraction: 0.9);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final h = 232.0;
    final reserveBalance = widget.data.emergencyReserveBalanceCents;
    final reserveMonthly = widget.data.emergencyReserveMonthlyTargetCents;
    final reserveMonthsCovered = reserveMonthly > 0
        ? (reserveBalance / reserveMonthly).clamp(0, 99).toDouble()
        : 0.0;
    final reserveProgressToYear = reserveMonthly > 0
        ? (reserveBalance / (reserveMonthly * 12)).clamp(0.0, 1.0)
        : 0.0;
    final reserveProgressPct = (reserveProgressToYear * 100).round();
    final reserveAccent = Color.lerp(
          const Color(0xFF2E7D32),
          const Color(0xFF00A86B),
          reserveProgressToYear,
        ) ??
        const Color(0xFF2E7D32);
    final reserveMomentumLabel = reserveProgressPct >= 100
        ? l10n.dashEmergencyReserveStageDone
        : reserveProgressPct >= 75
            ? l10n.dashEmergencyReserveStageStrong
            : reserveProgressPct >= 50
                ? l10n.dashEmergencyReserveStageMid
                : reserveProgressPct >= 25
                    ? l10n.dashEmergencyReserveStageStart
                    : l10n.dashEmergencyReserveStageFirst;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(
          height: h,
          child: PageView(
            controller: _pageController,
            onPageChanged: (i) => setState(() => _pageIndex = i),
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: _HeroSlide(
                  accent: const Color(0xFF00A86B),
                  icon: Icons.dashboard_outlined,
                  title: l10n.dashMonthSummary,
                  isVibrant: true,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _ReserveBalanceHighlight(
                        title: l10n.dashBalance,
                        value: formatBrlFromCents(widget.data.monthBalanceCents),
                        accent: const Color(0xFF00A86B),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: _ReserveMiniMetric(
                              label: l10n.dashIncome,
                              value: formatBrlFromCents(widget.data.monthIncomeCents),
                              accent: const Color(0xFF00A86B),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: _ReserveMiniMetric(
                              label: l10n.dashExpenses,
                              value: formatBrlFromCents(
                                widget.data.monthExpenseTotalCents,
                              ),
                              accent: const Color(0xFF00A86B),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: _HeroSlide(
                  accent: reserveAccent,
                  icon: Icons.shield_outlined,
                  title: l10n.dashEmergencyReserve,
                  isVibrant: true,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _ReserveBalanceHighlight(
                        title: l10n.dashEmergencyReserveBalance,
                        value: formatBrlFromCents(reserveBalance),
                        accent: reserveAccent,
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: _ReserveMiniMetric(
                              label: l10n.dashEmergencyReserveMonthly,
                              value: formatBrlFromCents(reserveMonthly),
                              accent: reserveAccent,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: _ReserveMiniMetric(
                              label: l10n.dashEmergencyReserveTimesTarget,
                              value: '${reserveMonthsCovered.toStringAsFixed(1)}x',
                              accent: reserveAccent,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(7),
                        child: LinearProgressIndicator(
                          value: reserveProgressToYear,
                          minHeight: 10,
                          backgroundColor: Colors.white.withValues(alpha: 0.45),
                          color: reserveAccent,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        l10n.dashEmergencyReserveAnnualProgress(
                          reserveProgressPct,
                        ),
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.labelMedium?.copyWith(
                              color: WellPaidColors.navy.withValues(alpha: 0.84),
                              fontWeight: FontWeight.w700,
                            ),
                      ),
                      const SizedBox(height: 2),
                      _heroMetric(
                        context,
                        l10n.dashEmergencyReserveMonthly,
                        reserveMomentumLabel,
                      ),
                      Text(
                        l10n.dashEmergencyReserveFootnote,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: WellPaidColors.navy.withValues(alpha: 0.58),
                              height: 1.2,
                              fontSize: 11.5,
                            ),
                      ),
                      const SizedBox(height: 6),
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton(
                          style: TextButton.styleFrom(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            minimumSize: Size.zero,
                            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                          onPressed: widget.onConfigureReserve,
                          child: Text(l10n.dashEmergencyReserveConfigure),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(2, (i) {
            final on = i == _pageIndex;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              curve: Curves.easeOutCubic,
              margin: const EdgeInsets.symmetric(horizontal: 4),
              width: on ? 22 : 7,
              height: 7,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(4),
                color: on
                    ? WellPaidColors.navy.withValues(alpha: 0.85)
                    : WellPaidColors.navy.withValues(alpha: 0.2),
              ),
            );
          }),
        ),
      ],
    );
  }
}

Widget _heroMetric(
  BuildContext context,
  String k,
  String v, {
  bool emphasize = false,
}) {
  return Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(
            k,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: WellPaidColors.navy.withValues(alpha: 0.82),
                ),
          ),
        ),
        const SizedBox(width: 8),
        Text(
          v,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                fontWeight: emphasize ? FontWeight.w800 : FontWeight.w700,
                color: WellPaidColors.navy,
              ),
        ),
      ],
    ),
  );
}

class _HeroSlide extends StatelessWidget {
  const _HeroSlide({
    required this.accent,
    required this.icon,
    required this.title,
    required this.child,
    this.isVibrant = false,
  });

  final Color accent;
  final IconData icon;
  final String title;
  final Widget child;
  final bool isVibrant;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      label: title,
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: BorderSide(color: accent.withValues(alpha: 0.22)),
        ),
        clipBehavior: Clip.antiAlias,
        child: DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                isVibrant
                    ? accent.withValues(alpha: 0.22)
                    : WellPaidColors.creamMuted.withValues(alpha: 0.95),
                isVibrant
                    ? const Color(0xFFFFFFFF).withValues(alpha: 0.9)
                    : accent.withValues(alpha: 0.07),
              ],
            ),
          ),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        color: accent.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(11),
                      ),
                      child: Icon(icon, size: 21, color: accent),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        title,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w800,
                              color: WellPaidColors.navy,
                            ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Expanded(
                  child: SingleChildScrollView(
                    physics: const ClampingScrollPhysics(),
                    child: child,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ReserveBalanceHighlight extends StatelessWidget {
  const _ReserveBalanceHighlight({
    required this.title,
    required this.value,
    required this.accent,
  });

  final String title;
  final String value;
  final Color accent;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: Colors.white.withValues(alpha: 0.7),
        border: Border.all(
          color: accent.withValues(alpha: 0.36),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 9, 10, 9),
        child: Row(
          children: [
            Container(
              width: 30,
              height: 30,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(9),
                color: accent.withValues(alpha: 0.2),
              ),
              child: Icon(
                Icons.savings_outlined,
                size: 18,
                color: accent,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.68),
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    value,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: WellPaidColors.navy,
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ReserveMiniMetric extends StatelessWidget {
  const _ReserveMiniMetric({
    required this.label,
    required this.value,
    required this.accent,
  });

  final String label;
  final String value;
  final Color accent;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(10),
        color: accent.withValues(alpha: 0.1),
        border: Border.all(
          color: accent.withValues(alpha: 0.28),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: WellPaidColors.navy.withValues(alpha: 0.62),
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(height: 2),
            Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: WellPaidColors.navy,
                    fontWeight: FontWeight.w800,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionChartCard extends StatelessWidget {
  const _SectionChartCard({
    required this.title,
    required this.icon,
    required this.accent,
    required this.child,
  });

  final String title;
  final IconData icon;
  final Color accent;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: WellPaidColors.creamMuted.withValues(alpha: 0.85),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: accent.withValues(alpha: 0.18)),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, size: 20, color: accent),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: WellPaidColors.navy,
                        ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            child,
          ],
        ),
      ),
    );
  }
}

class _DashboardActionCard extends StatelessWidget {
  const _DashboardActionCard({
    required this.title,
    required this.icon,
    required this.accent,
    required this.onTap,
    this.badge = _ActionBadge.none,
    this.isFeatured = false,
  });

  final String title;
  final IconData icon;
  final Color accent;
  final VoidCallback onTap;
  final _ActionBadge badge;
  final bool isFeatured;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final cardWidth = isFeatured ? (width - 16 - 16) : (width - 16 - 16 - 10) / 2;
    final badgeColor = switch (badge) {
      _ActionBadge.alert => const Color(0xFFB00020),
      _ActionBadge.warning => const Color(0xFFF9A825),
      _ActionBadge.none => Colors.transparent,
    };
    return SizedBox(
      width: cardWidth.clamp(140, isFeatured ? 1200 : 280).toDouble(),
      child: Card(
        elevation: 0,
        color: badge == _ActionBadge.none
            ? WellPaidColors.creamMuted.withValues(alpha: 0.85)
            : badgeColor.withValues(alpha: 0.08),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: BorderSide(
            color: badge == _ActionBadge.none
                ? accent.withValues(alpha: 0.18)
                : badgeColor.withValues(alpha: 0.45),
          ),
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 10, 12),
            child: Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, size: 18, color: accent),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: WellPaidColors.navy,
                        ),
                  ),
                ),
                if (badge != _ActionBadge.none) ...[
                  const SizedBox(width: 6),
                  Container(
                    width: 9,
                    height: 9,
                    decoration: BoxDecoration(
                      color: badgeColor,
                      shape: BoxShape.circle,
                    ),
                  ),
                ],
                Icon(
                  Icons.chevron_right_rounded,
                  color: WellPaidColors.navy.withValues(alpha: 0.65),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

enum _ActionBadge { none, warning, alert }

class _ExpandedSectionPanel extends StatelessWidget {
  const _ExpandedSectionPanel({
    super.key,
    required this.title,
    required this.child,
    required this.onClose,
  });

  final String title;
  final Widget child;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: WellPaidColors.navy.withValues(alpha: 0.12)),
      ),
      child: SizedBox.expand(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: WellPaidColors.navy,
                          ),
                    ),
                  ),
                  IconButton(
                    onPressed: onClose,
                    icon: const Icon(Icons.close_rounded),
                    tooltip: context.l10n.close,
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Expanded(
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  child: child,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PendingTile extends StatelessWidget {
  const _PendingTile({required this.item, this.onPay});

  final PendingExpenseItem item;
  final Future<void> Function()? onPay;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final due = item.dueDate;
    final urgency = due == null
        ? DueUrgency.safe
        : dueUrgencyFor(due, DateTime.now());
    final accent = dueUrgencyAccent(urgency);
    final dueStr = due == null
        ? '—'
        : '${due.day.toString().padLeft(2, '0')}/${due.month.toString().padLeft(2, '0')}/${due.year}';

    return Semantics(
      label: l10n.dashPendingItemA11y(
        item.description,
        formatBrlFromCents(item.amountCents),
        dueStr,
      ),
      child: Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: DecoratedBox(
          decoration: BoxDecoration(
            border: Border(left: BorderSide(color: accent, width: 4)),
            color: accent.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 8, 10),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.description,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              fontWeight: FontWeight.w600,
                              color: WellPaidColors.navy,
                            ),
                      ),
                      Text(
                        '${l10n.dashDueShort} $dueStr${item.isMine ? '' : l10n.dashFamilySuffix}',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: accent,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  formatBrlFromCents(item.amountCents),
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: WellPaidColors.navy,
                      ),
                ),
                if (onPay != null)
                  Padding(
                    padding: const EdgeInsets.only(left: 2),
                    child: IconButton(
                      tooltip: l10n.dashMarkPaidTooltip,
                      onPressed: () => unawaited(onPay!()),
                      icon: const Icon(Icons.payment_outlined, size: 22),
                      color: WellPaidColors.goldPressed,
                      constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
                      padding: EdgeInsets.zero,
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _UpcomingTile extends StatelessWidget {
  const _UpcomingTile({required this.item, required this.urgency});

  final PendingExpenseItem item;
  final DueUrgency urgency;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final due = item.dueDate!;
    final dueStr =
        '${due.day.toString().padLeft(2, '0')}/${due.month.toString().padLeft(2, '0')}/${due.year}';
    final accent = dueUrgencyAccent(urgency);

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: DecoratedBox(
        decoration: BoxDecoration(
          border: Border(
            left: BorderSide(color: accent, width: 4),
          ),
          color: accent.withValues(alpha: 0.06),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 10, 10),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                            color: WellPaidColors.navy,
                          ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${l10n.dashDueVerb} $dueStr',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: accent,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ],
                ),
              ),
              Text(
                formatBrlFromCents(item.amountCents),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: WellPaidColors.navy,
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _GoalRow extends StatelessWidget {
  const _GoalRow({required this.goal});

  final GoalSummaryItem goal;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final p = goal.targetCents > 0
        ? (goal.currentCents / goal.targetCents).clamp(0.0, 1.0)
        : 0.0;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            goal.isMine ? goal.title : '${goal.title}${l10n.dashGoalFamilySuffix}',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: p,
              minHeight: 8,
              backgroundColor: WellPaidColors.navy.withValues(alpha: 0.1),
              color: WellPaidColors.gold,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '${formatBrlFromCents(goal.currentCents)} / ${formatBrlFromCents(goal.targetCents)}',
            style: Theme.of(context).textTheme.labelSmall,
          ),
        ],
      ),
    );
  }
}
