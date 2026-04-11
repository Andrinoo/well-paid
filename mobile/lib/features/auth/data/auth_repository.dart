import 'package:dio/dio.dart';

import '../../../core/config/api_config.dart';
import '../domain/token_pair.dart';

class RegisterResult {
  RegisterResult({
    required this.message,
    required this.email,
    this.devVerificationToken,
    this.devVerificationCode,
  });

  final String message;
  final String email;
  final String? devVerificationToken;
  final String? devVerificationCode;
}

class ResendVerificationResult {
  ResendVerificationResult({
    required this.message,
    this.devVerificationToken,
    this.devVerificationCode,
  });

  final String message;
  final String? devVerificationToken;
  final String? devVerificationCode;
}

class ForgotPasswordResult {
  ForgotPasswordResult({required this.message, this.devResetToken});

  final String message;
  final String? devResetToken;
}

class AuthRepository {
  AuthRepository(this._dio);

  final Dio _dio;

  Future<RegisterResult> register({
    required String email,
    required String password,
    String? fullName,
    String? phone,
  }) async {
    final res = await _dio.postUri<Map<String, dynamic>>(
      ApiConfig.apiUri('/auth/register'),
      data: {
        'email': email,
        'password': password,
        if (fullName != null && fullName.trim().isNotEmpty)
          'full_name': fullName.trim(),
        if (phone != null && phone.trim().isNotEmpty) 'phone': phone.trim(),
      },
    );
    final d = res.data!;
    return RegisterResult(
      message: d['message'] as String? ?? '',
      email: d['email'] as String? ?? email.trim(),
      devVerificationToken: d['dev_verification_token'] as String?,
      devVerificationCode: d['dev_verification_code'] as String?,
    );
  }

  Future<TokenPair> verifyEmail({
    String? token,
    String? email,
    String? code,
  }) async {
    final body = <String, dynamic>{};
    final t = token?.trim();
    if (t != null && t.isNotEmpty) {
      body['token'] = t;
    } else {
      body['email'] = email!.trim();
      body['code'] = code!.trim();
    }
    final res = await _dio.postUri<Map<String, dynamic>>(
      ApiConfig.apiUri('/auth/verify-email'),
      data: body,
    );
    return TokenPair.fromJson(res.data!);
  }

  Future<ResendVerificationResult> resendVerification(String email) async {
    final res = await _dio.postUri<Map<String, dynamic>>(
      ApiConfig.apiUri('/auth/resend-verification'),
      data: {'email': email.trim()},
    );
    final d = res.data!;
    return ResendVerificationResult(
      message: d['message'] as String? ?? '',
      devVerificationToken: d['dev_verification_token'] as String?,
      devVerificationCode: d['dev_verification_code'] as String?,
    );
  }

  Future<TokenPair> login({
    required String email,
    required String password,
  }) async {
    final res = await _dio.postUri<Map<String, dynamic>>(
      ApiConfig.apiUri('/auth/login'),
      data: {'email': email, 'password': password},
    );
    return TokenPair.fromJson(res.data!);
  }

  Future<TokenPair> refresh({required String refreshToken}) async {
    final res = await _dio.postUri<Map<String, dynamic>>(
      ApiConfig.apiUri('/auth/refresh'),
      data: {'refresh_token': refreshToken},
    );
    return TokenPair.fromJson(res.data!);
  }

  Future<void> logout(String refreshToken) async {
    await _dio.postUri<void>(
      ApiConfig.apiUri('/auth/logout'),
      data: {'refresh_token': refreshToken},
    );
  }

  Future<ForgotPasswordResult> forgotPassword(String email) async {
    final res = await _dio.postUri<Map<String, dynamic>>(
      ApiConfig.apiUri('/auth/forgot-password'),
      data: {'email': email.trim()},
    );
    final data = res.data!;
    return ForgotPasswordResult(
      message: data['message'] as String? ?? 'Pedido enviado.',
      devResetToken: data['dev_reset_token'] as String?,
    );
  }

  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    await _dio.postUri<void>(
      ApiConfig.apiUri('/auth/reset-password'),
      data: {'token': token.trim(), 'new_password': newPassword},
    );
  }
}
