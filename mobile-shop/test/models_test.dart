import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/models/page_result.dart';
import 'package:hisobnoma_shop/models/public_category.dart';
import 'package:hisobnoma_shop/models/public_product.dart';

void main() {
  group('PublicProduct.fromJson', () {
    test('maps full payload from the catalog API', () {
      final product = PublicProduct.fromJson({
        'id': 1,
        'name': 'Cola Bottle',
        'shortDescription': '0.5L',
        'description': 'Cold drink',
        'price': 12000.5,
        'currency': 'UZS',
        'categoryId': 3,
        'categoryName': 'Drinks',
        'brandName': 'Coca-Cola',
        'unitName': 'Pieces',
        'inStock': true,
        'imageUrl': '/uploads/products/1/main.jpg',
        'images': ['/uploads/products/1/main.jpg', '/uploads/products/1/b.jpg'],
      });

      expect(product.id, 1);
      expect(product.name, 'Cola Bottle');
      expect(product.price, 12000.5);
      expect(product.currency, 'UZS');
      expect(product.categoryId, 3);
      expect(product.inStock, true);
      expect(product.images, hasLength(2));
    });

    test('tolerates missing optional fields', () {
      final product = PublicProduct.fromJson({
        'id': 2,
        'name': 'Juice',
        'price': 8000,
        'inStock': false,
      });

      expect(product.id, 2);
      expect(product.price, 8000.0);
      expect(product.currency, 'UZS');
      expect(product.inStock, false);
      expect(product.imageUrl, isNull);
      expect(product.images, isEmpty);
      expect(product.salePrice, isNull);
      expect(product.promotionLabel, isNull);
    });

    test('maps promotion sale price and label', () {
      final product = PublicProduct.fromJson({
        'id': 3,
        'name': 'Cola',
        'price': 12000,
        'salePrice': 10200.0,
        'promotionLabel': '-15%',
        'inStock': true,
      });

      expect(product.salePrice, 10200.0);
      expect(product.promotionLabel, '-15%');
    });
  });

  group('PublicCategory.fromJson', () {
    test('maps id and name', () {
      final category = PublicCategory.fromJson({'id': 5, 'name': 'Drinks'});
      expect(category.id, 5);
      expect(category.name, 'Drinks');
    });
  });

  group('PageResult.fromJson', () {
    test('maps content and page metadata', () {
      final page = PageResult.fromJson({
        'content': [
          {'id': 1, 'name': 'A', 'price': 100, 'inStock': true},
          {'id': 2, 'name': 'B', 'price': 200, 'inStock': false},
        ],
        'page': {'number': 1, 'totalPages': 3, 'last': false},
      }, PublicProduct.fromJson);

      expect(page.content, hasLength(2));
      expect(page.number, 1);
      expect(page.totalPages, 3);
      expect(page.last, false);
    });

    test('defaults to a single final page when metadata missing', () {
      final page = PageResult.fromJson(
        {'content': <Map<String, dynamic>>[]},
        PublicProduct.fromJson,
      );

      expect(page.content, isEmpty);
      expect(page.last, true);
    });
  });
}
