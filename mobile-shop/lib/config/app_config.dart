import 'package:flutter/foundation.dart';

/// Build-time configuration. Values are injected with --dart-define, e.g.:
///
///   flutter build apk \
///     --dart-define=SHOP_API_BASE_URL=https://shop.example.uz \
///     --dart-define=SHOP_TENANT_ID=1 \
///     --dart-define=SHOP_PHONE=+998901234567 \
///     --dart-define=SHOP_TELEGRAM=https://t.me/yourshop
class AppConfig {
  AppConfig._();

  /// Backend base URL, without a trailing slash.
  static const String apiBaseUrl = String.fromEnvironment(
    'SHOP_API_BASE_URL',
    // Android emulator reaches the host machine's localhost via 10.0.2.2.
    defaultValue: kDebugMode ? 'http://localhost:8080' : '',
  );

  /// Tenant served by this app build (sent as X-Tenant-ID).
  static const String tenantId = String.fromEnvironment(
    'SHOP_TENANT_ID',
    defaultValue: '1',
  );

  /// Phone number for the "order by phone" button (tel: link).
  static const String shopPhone = String.fromEnvironment(
    'SHOP_PHONE',
    defaultValue: '',
  );

  /// Telegram link for the "order via Telegram" button.
  static const String shopTelegram = String.fromEnvironment(
    'SHOP_TELEGRAM',
    defaultValue: '',
  );

  /// Resolves image URLs returned by the API. The backend stores relative
  /// paths like /uploads/products/1/main.jpg.
  static String resolveImageUrl(String? url) {
    if (url == null || url.isEmpty) return '';
    if (url.startsWith('http://') || url.startsWith('https://')) return url;
    return '$apiBaseUrl$url';
  }
}
