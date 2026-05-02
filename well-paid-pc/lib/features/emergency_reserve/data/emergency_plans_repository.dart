import 'package:dio/dio.dart';

class EmergencyReservePlanItem {
  const EmergencyReservePlanItem({
    required this.id,
    required this.title,
    required this.balanceCents,
    required this.monthlyTargetCents,
    required this.status,
    required this.trackingStart,
    this.paceStatus,
    this.targetEndDate,
    this.planDurationMonths,
  });

  final String id;
  final String title;
  final int balanceCents;
  final int monthlyTargetCents;
  final String status;
  final DateTime trackingStart;
  final String? paceStatus;
  final DateTime? targetEndDate;
  final int? planDurationMonths;

  factory EmergencyReservePlanItem.fromJson(Map<String, dynamic> json) {
    DateTime? end;
    final te = json['target_end_date'];
    if (te is String) end = DateTime.tryParse(te);

    DateTime tracking = DateTime.now();
    final ts = json['tracking_start'];
    if (ts is String) tracking = DateTime.tryParse(ts) ?? tracking;

    return EmergencyReservePlanItem(
      id: json['id'].toString(),
      title: json['title'] as String? ?? '',
      balanceCents: (json['balance_cents'] as num?)?.toInt() ?? 0,
      monthlyTargetCents: (json['monthly_target_cents'] as num?)?.toInt() ?? 0,
      status: json['status'] as String? ?? '',
      trackingStart: tracking,
      paceStatus: json['pace_status'] as String?,
      targetEndDate: end,
      planDurationMonths: (json['plan_duration_months'] as num?)?.toInt(),
    );
  }
}

class EmergencyReserveMonthRow {
  const EmergencyReserveMonthRow({
    required this.year,
    required this.month,
    required this.expectedCents,
    required this.depositedCents,
    required this.shortfallCents,
    required this.paceStatus,
  });

  final int year;
  final int month;
  final int expectedCents;
  final int depositedCents;
  final int shortfallCents;
  final String paceStatus;

  factory EmergencyReserveMonthRow.fromJson(Map<String, dynamic> json) {
    return EmergencyReserveMonthRow(
      year: (json['year'] as num?)?.toInt() ?? 0,
      month: (json['month'] as num?)?.toInt() ?? 0,
      expectedCents: (json['expected_cents'] as num?)?.toInt() ?? 0,
      depositedCents: (json['deposited_cents'] as num?)?.toInt() ?? 0,
      shortfallCents: (json['shortfall_cents'] as num?)?.toInt() ?? 0,
      paceStatus: json['pace_status'] as String? ?? '',
    );
  }
}

class EmergencyPlansRepository {
  EmergencyPlansRepository(this._dio);

  final Dio _dio;

  Future<List<EmergencyReservePlanItem>> listPlans() async {
    final res = await _dio.get<List<dynamic>>('/emergency-reserve/plans');
    final list = res.data ?? [];
    return list
        .map((e) =>
            EmergencyReservePlanItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<EmergencyReserveMonthRow>> listPlanMonths(String planId) async {
    final res = await _dio.get<List<dynamic>>(
      '/emergency-reserve/plans/$planId/months',
    );
    final list = res.data ?? [];
    return list
        .map((e) =>
            EmergencyReserveMonthRow.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<EmergencyReservePlanItem> createPlan({
    required String title,
    required int monthlyTargetCents,
    int? planDurationMonths,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/emergency-reserve/plans',
      data: {
        'title': title,
        'monthly_target_cents': monthlyTargetCents,
        if (planDurationMonths != null)
          'plan_duration_months': planDurationMonths,
      },
    );
    return EmergencyReservePlanItem.fromJson(res.data!);
  }
}
