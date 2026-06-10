import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/api/catalog_api.dart';
import 'package:hisobnoma_shop/models/order.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  HttpCatalogApi apiWith(MockClient client) =>
      HttpCatalogApi(client: client, baseUrl: 'http://test', tenantId: '7');

  group('HttpCatalogApi', () {
    test('getProducts sends tenant header and query params', () async {
      late http.Request captured;
      final client = MockClient((request) async {
        captured = request;
        return http.Response(
          jsonEncode({
            'content': [
              {'id': 1, 'name': 'Cola', 'price': 12000, 'inStock': true},
            ],
            'page': {'number': 0, 'totalPages': 1, 'last': true},
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final page = await apiWith(client)
          .getProducts(search: ' cola ', categoryId: 3, page: 2, size: 10);

      expect(captured.headers['X-Tenant-ID'], '7');
      expect(captured.url.path, '/api/v1/web/catalog/products');
      expect(captured.url.queryParameters['search'], 'cola');
      expect(captured.url.queryParameters['categoryId'], '3');
      expect(captured.url.queryParameters['page'], '2');
      expect(captured.url.queryParameters['size'], '10');
      expect(page.content.single.name, 'Cola');
    });

    test('getProduct unwraps ApiResponse data envelope', () async {
      final client = MockClient((request) async {
        expect(request.url.path, '/api/v1/web/catalog/products/5');
        return http.Response(
          jsonEncode({
            'success': true,
            'data': {'id': 5, 'name': 'Juice', 'price': 8000, 'inStock': false},
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final product = await apiWith(client).getProduct(5);

      expect(product.id, 5);
      expect(product.inStock, false);
    });

    test('getCategories maps data list', () async {
      final client = MockClient((request) async {
        return http.Response(
          jsonEncode({
            'success': true,
            'data': [
              {'id': 1, 'name': 'Drinks'},
              {'id': 2, 'name': 'Snacks'},
            ],
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final categories = await apiWith(client).getCategories();

      expect(categories, hasLength(2));
      expect(categories.first.name, 'Drinks');
    });

    test('non-2xx response throws ApiException with status code', () async {
      final client = MockClient((request) async => http.Response('nope', 404));

      expect(
        () => apiWith(client).getProduct(99),
        throwsA(isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 404)),
      );
    });

    test('submitOrder posts checkout payload with tenant header', () async {
      late http.Request captured;
      final client = MockClient((request) async {
        captured = request;
        return http.Response(
          jsonEncode({
            'success': true,
            'data': {
              'orderNumber': 'WO-000001',
              'status': 'NEW',
              'totalAmount': 24000,
              'currency': 'UZS',
            },
          }),
          201,
          headers: {'content-type': 'application/json'},
        );
      });

      final order = await apiWith(client).submitOrder(OrderRequest(
        customerName: 'Ali',
        phone: '+998901234567',
        lines: const [OrderRequestLine(catalogItemId: 1, quantity: 2)],
      ));

      expect(captured.method, 'POST');
      expect(captured.url.path, '/api/v1/web/orders');
      expect(captured.headers['X-Tenant-ID'], '7');
      final body = jsonDecode(captured.body) as Map<String, dynamic>;
      expect(body['customerName'], 'Ali');
      expect((body['lines'] as List).single['catalogItemId'], 1);
      expect(order.orderNumber, 'WO-000001');
    });

    test('getOrderStatus passes phone as query param', () async {
      late http.Request captured;
      final client = MockClient((request) async {
        captured = request;
        return http.Response(
          jsonEncode({
            'success': true,
            'data': {
              'orderNumber': 'WO-000001',
              'status': 'DELIVERING',
              'totalAmount': 24000,
              'currency': 'UZS',
            },
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final order =
          await apiWith(client).getOrderStatus('WO-000001', '+998901234567');

      expect(captured.url.path, '/api/v1/web/orders/WO-000001');
      expect(captured.url.queryParameters['phone'], '+998901234567');
      expect(order.status, 'DELIVERING');
    });

    test('decodes UTF-8 (Cyrillic) payloads correctly', () async {
      final client = MockClient((request) async {
        return http.Response.bytes(
          utf8.encode(jsonEncode({
            'content': [
              {'id': 1, 'name': 'Сут 1Л', 'price': 15000, 'inStock': true},
            ],
            'page': {'number': 0, 'totalPages': 1, 'last': true},
          })),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final page = await apiWith(client).getProducts();

      expect(page.content.single.name, 'Сут 1Л');
    });
  });
}
