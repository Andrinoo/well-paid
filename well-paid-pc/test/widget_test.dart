import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:well_paid_pc/features/auth/presentation/login_page.dart';
import 'package:well_paid_pc/l10n/app_localizations.dart';

void main() {
  testWidgets('login screen builds', (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          locale: const Locale('pt'),
          supportedLocales: AppLocalizations.supportedLocales,
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          home: const LoginPage(),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('Entrar na conta'), findsOneWidget);
  });
}
