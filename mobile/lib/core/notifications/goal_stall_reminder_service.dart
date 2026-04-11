import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../../features/goals/domain/goal_item.dart';
import '../../l10n/app_localizations.dart';

/// Lembrete local quando há metas ativas tuas sem atualização há [stallDaysThreshold] dias
/// (usa [GoalItem.updatedAt], que o backend actualiza nas contribuições).
class GoalStallReminderService {
  GoalStallReminderService._();

  static const prefKey = 'pref_goal_stall_reminder_v1';
  static const notificationId = 9001;
  static const stallDaysThreshold = 21;

  static FlutterLocalNotificationsPlugin? _plugin;
  static bool _timeZoneReady = false;

  static bool get _supported =>
      !kIsWeb &&
      (defaultTargetPlatform == TargetPlatform.android ||
          defaultTargetPlatform == TargetPlatform.iOS);

  /// Chamar em [main] após [WidgetsFlutterBinding.ensureInitialized].
  static Future<void> init() async {
    if (!_supported) return;
    _plugin ??= FlutterLocalNotificationsPlugin();
    await _plugin!.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('@mipmap/ic_launcher'),
        iOS: DarwinInitializationSettings(),
      ),
    );
    tzdata.initializeTimeZones();
    try {
      final name = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(name));
    } catch (_) {
      tz.setLocalLocation(tz.UTC);
    }
    _timeZoneReady = true;
  }

  static Future<bool> isEnabled() async {
    final p = await SharedPreferences.getInstance();
    return p.getBool(prefKey) ?? false;
  }

  static Future<void> setEnabled(bool value) async {
    final p = await SharedPreferences.getInstance();
    await p.setBool(prefKey, value);
    if (!value) await cancelScheduled();
  }

  static Future<void> requestPostNotificationsPermission() async {
    if (!_supported || _plugin == null) return;
    await _plugin!
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    await _plugin!
        .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(alert: true, badge: true, sound: true);
  }

  /// Android: estado actual do canal de notificações da app. Noutras plataformas devolve `null`.
  static Future<bool?> androidNotificationsEnabled() async {
    if (!_supported ||
        defaultTargetPlatform != TargetPlatform.android ||
        _plugin == null) {
      return null;
    }
    return _plugin!
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.areNotificationsEnabled();
  }

  static Future<void> cancelScheduled() async {
    if (!_supported || _plugin == null) return;
    await _plugin!.cancel(notificationId);
  }

  /// Reagenda o lembrete (uma notificação agregada) para a próxima ocorrência das 10:00 locais.
  static Future<void> syncFromGoals(
    List<GoalItem> goals, {
    required Locale locale,
  }) async {
    if (!_supported) return;
    if (!_timeReady) await init();
    if (_plugin == null) return;

    if (!await isEnabled()) {
      await cancelScheduled();
      return;
    }

    final stalled = goals.where(_isStalled).toList();
    if (stalled.isEmpty) {
      await cancelScheduled();
      return;
    }

    final l10n = lookupAppLocalizations(locale);
    final title = l10n.goalStallNotifTitle;
    final body = _formatBody(l10n, stalled);
    final when = _nextTenAmLocal();

    const android = AndroidNotificationDetails(
      'goal_stall_v1',
      'Metas',
      channelDescription: 'Lembretes quando uma meta está parada',
      importance: Importance.defaultImportance,
      priority: Priority.defaultPriority,
    );
    final details = NotificationDetails(
      android: android,
      iOS: const DarwinNotificationDetails(),
    );

    await _plugin!.zonedSchedule(
      notificationId,
      title,
      body,
      tz.TZDateTime.from(when, tz.local),
      details,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
    );
  }

  static bool get _timeReady => _timeZoneReady && _plugin != null;

  static bool _isStalled(GoalItem g) {
    if (!g.isActive || !g.isMine) return false;
    if (g.targetCents <= 0) return false;
    if (g.currentCents >= g.targetCents) return false;
    final last = g.updatedAt.toLocal();
    final days = DateTime.now().difference(last).inDays;
    return days >= stallDaysThreshold;
  }

  static DateTime _nextTenAmLocal() {
    final n = DateTime.now();
    var t = DateTime(n.year, n.month, n.day, 10);
    if (!t.isAfter(n)) {
      t = t.add(const Duration(days: 1));
    }
    return t;
  }

  static String _formatBody(AppLocalizations l10n, List<GoalItem> stalled) {
    if (stalled.length == 1) {
      return l10n.goalStallNotifBodySingle(stalled.first.title);
    }
    if (stalled.length == 2) {
      return l10n.goalStallNotifBodyTwo(
        stalled[0].title,
        stalled[1].title,
      );
    }
    final more = stalled.length - 1;
    return l10n.goalStallNotifBodyMany(stalled.first.title, more);
  }
}
