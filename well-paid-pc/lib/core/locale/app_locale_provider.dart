import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _kInterfaceLocale = 'app_interface_locale';

/// `pt` = português (Brasil); `en` = inglês (EUA) na UI.
final appLocaleProvider =
    AsyncNotifierProvider<AppLocaleNotifier, Locale>(AppLocaleNotifier.new);

class AppLocaleNotifier extends AsyncNotifier<Locale> {
  @override
  Future<Locale> build() async {
    final prefs = await SharedPreferences.getInstance();
    return _decode(prefs.getString(_kInterfaceLocale));
  }

  Locale _decode(String? raw) {
    if (raw == 'en') return const Locale('en');
    return const Locale('pt');
  }

  Future<void> setLocale(Locale locale) async {
    final prefs = await SharedPreferences.getInstance();
    final code = locale.languageCode == 'en' ? 'en' : 'pt';
    await prefs.setString(_kInterfaceLocale, code);
    state = AsyncData(_decode(code));
  }
}
