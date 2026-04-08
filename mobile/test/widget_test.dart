import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:well_paid/features/auth/presentation/login_page.dart';

void main() {
  testWidgets('login screen builds', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(
          home: LoginPage(),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('Entrar na conta'), findsOneWidget);
  });
}
