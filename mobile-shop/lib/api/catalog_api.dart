import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import '../models/delivery.dart';
import '../models/order.dart';
import '../models/page_result.dart';
import '../models/public_category.dart';
import '../models/public_product.dart';

/// Thrown for non-2xx responses and transport failures.
class ApiException implements Exception {
  final String message;
  final int? statusCode;

  ApiException(this.message, {this.statusCode});

  @override
  String toString() => 'ApiException($statusCode): $message';
}

/// Client of the public shop API (catalog browsing + ordering).
/// Abstract so screens and widget tests can substitute a fake.
abstract class CatalogApi {
  Future<PageResult<PublicProduct>> getProducts({
    String? search,
    int? categoryId,
    int page = 0,
    int size = 20,
  });

  Future<PublicProduct> getProduct(int id);

  Future<List<PublicCategory>> getCategories();

  Future<List<DeliveryRegion>> getRegions();

  Future<List<DeliveryVillage>> getVillages(int regionId);

  Future<PublicOrder> submitOrder(OrderRequest request);

  Future<PublicOrder> getOrderStatus(String orderNumber, String phone);

  Future<void> requestOtp(String phone);

  /// Returns the auth session: {token, phone, name}.
  Future<AuthSession> verifyOtp(String phone, String code, {String? name});

  Future<PageResult<PublicOrder>> getMyOrders(String token, {int page = 0, int size = 20});
}

class AuthSession {
  final String token;
  final String phone;
  final String? name;

  const AuthSession({required this.token, required this.phone, this.name});

  factory AuthSession.fromJson(Map<String, dynamic> json) => AuthSession(
        token: json['token'] as String? ?? '',
        phone: json['phone'] as String? ?? '',
        name: json['name'] as String?,
      );
}

class HttpCatalogApi implements CatalogApi {
  final http.Client _client;
  final String _baseUrl;
  final String _tenantId;

  HttpCatalogApi({
    http.Client? client,
    String? baseUrl,
    String? tenantId,
  })  : _client = client ?? http.Client(),
        _baseUrl = baseUrl ?? AppConfig.apiBaseUrl,
        _tenantId = tenantId ?? AppConfig.tenantId;

  Map<String, String> get _headers => {
        'Accept': 'application/json',
        'X-Tenant-ID': _tenantId,
      };

  @override
  Future<PageResult<PublicProduct>> getProducts({
    String? search,
    int? categoryId,
    int page = 0,
    int size = 20,
  }) async {
    final params = <String, String>{
      'page': '$page',
      'size': '$size',
      if (search != null && search.trim().isNotEmpty) 'search': search.trim(),
      if (categoryId != null) 'categoryId': '$categoryId',
    };
    final json = await _get('/api/v1/web/catalog/products', params);
    return PageResult.fromJson(json, PublicProduct.fromJson);
  }

  @override
  Future<PublicProduct> getProduct(int id) async {
    final json = await _get('/api/v1/web/catalog/products/$id', const {});
    return PublicProduct.fromJson(json['data'] as Map<String, dynamic>);
  }

  @override
  Future<List<PublicCategory>> getCategories() async {
    final json = await _get('/api/v1/web/catalog/categories', const {});
    return (json['data'] as List<dynamic>? ?? const [])
        .map((e) => PublicCategory.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<DeliveryRegion>> getRegions() async {
    final json = await _get('/api/v1/web/delivery/regions', const {});
    return (json['data'] as List<dynamic>? ?? const [])
        .map((e) => DeliveryRegion.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<DeliveryVillage>> getVillages(int regionId) async {
    final json = await _get('/api/v1/web/delivery/villages', {'regionId': '$regionId'});
    return (json['data'] as List<dynamic>? ?? const [])
        .map((e) => DeliveryVillage.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<PublicOrder> submitOrder(OrderRequest request) async {
    final json = await _post('/api/v1/web/orders', request.toJson());
    return PublicOrder.fromJson(json['data'] as Map<String, dynamic>);
  }

  @override
  Future<PublicOrder> getOrderStatus(String orderNumber, String phone) async {
    final json = await _get('/api/v1/web/orders/$orderNumber', {'phone': phone});
    return PublicOrder.fromJson(json['data'] as Map<String, dynamic>);
  }

  @override
  Future<void> requestOtp(String phone) async {
    await _post('/api/v1/web/auth/request-otp', {'phone': phone});
  }

  @override
  Future<AuthSession> verifyOtp(String phone, String code, {String? name}) async {
    final json = await _post('/api/v1/web/auth/verify', {
      'phone': phone,
      'code': code,
      if (name != null && name.isNotEmpty) 'name': name,
    });
    return AuthSession.fromJson(json['data'] as Map<String, dynamic>);
  }

  @override
  Future<PageResult<PublicOrder>> getMyOrders(String token,
      {int page = 0, int size = 20}) async {
    final uri = Uri.parse('$_baseUrl/api/v1/web/me/orders')
        .replace(queryParameters: {'page': '$page', 'size': '$size'});
    late http.Response response;
    try {
      response = await _client
          .get(uri, headers: {..._headers, 'Authorization': 'Bearer $token'})
          .timeout(const Duration(seconds: 20));
    } on Exception catch (e) {
      throw ApiException('Network error: $e');
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('Request failed', statusCode: response.statusCode);
    }
    final json =
        jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    return PageResult.fromJson(json, PublicOrder.fromJson);
  }

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) async {
    final uri = Uri.parse('$_baseUrl$path');
    late http.Response response;
    try {
      response = await _client
          .post(uri,
              headers: {..._headers, 'Content-Type': 'application/json'},
              body: jsonEncode(body))
          .timeout(const Duration(seconds: 20));
    } on Exception catch (e) {
      throw ApiException('Network error: $e');
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('Request failed', statusCode: response.statusCode);
    }
    return jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> _get(
      String path, Map<String, String> params) async {
    final uri = Uri.parse('$_baseUrl$path')
        .replace(queryParameters: params.isEmpty ? null : params);
    late http.Response response;
    try {
      response = await _client
          .get(uri, headers: _headers)
          .timeout(const Duration(seconds: 20));
    } on Exception catch (e) {
      throw ApiException('Network error: $e');
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('Request failed', statusCode: response.statusCode);
    }
    return jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
  }
}
