import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:well_paid/features/auth/domain/jwt_access_expiry.dart';

void main() {
  test('readJwtExpSeconds parses exp', () {
    final payload = base64Url.encode(utf8.encode('{"exp":1735689600}'));
    final token = 'x.$payload.y';
    expect(readJwtExpSeconds(token), 1735689600);
  });

  test('accessTokenNeedsProactiveRefresh when past exp', () {
    final past = DateTime.now().toUtc().subtract(const Duration(hours: 1));
    final exp = past.millisecondsSinceEpoch ~/ 1000;
    final payload = base64Url.encode(utf8.encode('{"exp":$exp}'));
    final token = 'x.$payload.y';
    expect(accessTokenNeedsProactiveRefresh(token), isTrue);
  });
}
