import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
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

/// Read-only client of the public catalog API. Abstract so screens and
/// widget tests can substitute a fake.
abstract class CatalogApi {
  Future<PageResult<PublicProduct>> getProducts({
    String? search,
    int? categoryId,
    int page = 0,
    int size = 20,
  });

  Future<PublicProduct> getProduct(int id);

  Future<List<PublicCategory>> getCategories();
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
