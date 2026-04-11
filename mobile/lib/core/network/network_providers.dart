import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../storage/login_credentials_storage.dart';
import '../storage/token_storage.dart';
import 'dio_client.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

final loginCredentialsStorageProvider =
    Provider<LoginCredentialsStorage>((ref) => LoginCredentialsStorage());

/// Login, registo, logout, recuperação — **sem** Bearer nem refresh automático.
final authDioProvider = Provider<Dio>((ref) => createAuthDio());
