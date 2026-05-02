class InvestmentOverview {
  const InvestmentOverview({
    required this.totalAllocatedCents,
    required this.totalYieldCents,
    required this.estimatedMonthlyYieldCents,
    this.ratesFallbackUsed = true,
  });

  final int totalAllocatedCents;
  final int totalYieldCents;
  final int estimatedMonthlyYieldCents;
  final bool ratesFallbackUsed;

  factory InvestmentOverview.fromJson(Map<String, dynamic> json) {
    return InvestmentOverview(
      totalAllocatedCents:
          (json['total_allocated_cents'] as num?)?.toInt() ?? 0,
      totalYieldCents: (json['total_yield_cents'] as num?)?.toInt() ?? 0,
      estimatedMonthlyYieldCents:
          (json['estimated_monthly_yield_cents'] as num?)?.toInt() ?? 0,
      ratesFallbackUsed: json['rates_fallback_used'] as bool? ?? true,
    );
  }
}

class InvestmentPosition {
  const InvestmentPosition({
    required this.id,
    required this.name,
    required this.instrumentType,
    required this.principalCents,
    required this.annualRateBps,
    this.description,
    this.maturityDate,
    this.isLiquid = true,
  });

  final String id;
  final String name;
  final String instrumentType;
  final int principalCents;
  final int annualRateBps;
  final String? description;
  final String? maturityDate;
  final bool isLiquid;

  factory InvestmentPosition.fromJson(Map<String, dynamic> json) {
    return InvestmentPosition(
      id: json['id'] as String,
      name: json['name'] as String? ?? '',
      instrumentType: json['instrument_type'] as String? ?? '',
      principalCents: (json['principal_cents'] as num?)?.toInt() ?? 0,
      annualRateBps: (json['annual_rate_bps'] as num?)?.toInt() ?? 0,
      description: json['description'] as String?,
      maturityDate: json['maturity_date']?.toString(),
      isLiquid: json['is_liquid'] as bool? ?? true,
    );
  }
}
