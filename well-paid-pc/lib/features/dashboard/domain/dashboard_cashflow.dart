import 'dashboard_overview.dart';

/// Parâmetros de `GET /dashboard/cashflow` (Ordems §6.2.1).
class DashboardCashflowRequest {
  const DashboardCashflowRequest({
    required this.isDynamicWindow,
    this.startYear,
    this.startMonth,
    this.endYear,
    this.endMonth,
    this.forecastMonths = 3,
  }) : assert(
          isDynamicWindow ||
              (startYear != null &&
                  startMonth != null &&
                  endYear != null &&
                  endMonth != null),
          'Com janela fixa, indique start_year, start_month, end_year e end_month.',
        );

  /// Se true, o servidor ignora início/fim e usa os últimos 8 meses civis.
  final bool isDynamicWindow;
  final int? startYear;
  final int? startMonth;
  final int? endYear;
  final int? endMonth;
  final int forecastMonths;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is DashboardCashflowRequest &&
          runtimeType == other.runtimeType &&
          isDynamicWindow == other.isDynamicWindow &&
          startYear == other.startYear &&
          startMonth == other.startMonth &&
          endYear == other.endYear &&
          endMonth == other.endMonth &&
          forecastMonths == other.forecastMonths;

  @override
  int get hashCode => Object.hash(
        isDynamicWindow,
        startYear,
        startMonth,
        endYear,
        endMonth,
        forecastMonths,
      );
}

/// Resposta de `/dashboard/cashflow`: séries paralelas a [months] (centavos).
class DashboardCashflow {
  const DashboardCashflow({
    required this.isDynamicWindow,
    required this.forecastMonths,
    required this.months,
    required this.incomeCents,
    required this.expensePaidCents,
    required this.expenseForecastCents,
  });

  final bool isDynamicWindow;
  final int forecastMonths;
  final List<PeriodMonth> months;
  final List<int> incomeCents;
  final List<int> expensePaidCents;
  final List<int> expenseForecastCents;

  factory DashboardCashflow.fromJson(Map<String, dynamic> json) {
    final monthsJson = json['months'] as List<dynamic>;
    final income = json['income_cents'] as List<dynamic>;
    final paid = json['expense_paid_cents'] as List<dynamic>;
    final forecast = json['expense_forecast_cents'] as List<dynamic>;

    return DashboardCashflow(
      isDynamicWindow: json['dynamic'] as bool,
      forecastMonths: (json['forecast_months'] as num).toInt(),
      months: monthsJson
          .map((e) => PeriodMonth.fromJson(e as Map<String, dynamic>))
          .toList(),
      incomeCents: income.map((e) => (e as num).toInt()).toList(),
      expensePaidCents: paid.map((e) => (e as num).toInt()).toList(),
      expenseForecastCents: forecast.map((e) => (e as num).toInt()).toList(),
    );
  }

  /// Soma de [expenseForecastCents] em todo o período (útil para rodapé F4).
  int get totalForecastCents =>
      expenseForecastCents.fold<int>(0, (a, b) => a + b);

  /// Soma receitas − despesas pagas no período (útil para rodapé F4).
  int get periodBalanceCents {
    var s = 0;
    for (var i = 0; i < months.length; i++) {
      s += (i < incomeCents.length ? incomeCents[i] : 0) -
          (i < expensePaidCents.length ? expensePaidCents[i] : 0);
    }
    return s;
  }
}
