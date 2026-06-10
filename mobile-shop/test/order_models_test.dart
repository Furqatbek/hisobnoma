import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/models/delivery.dart';
import 'package:hisobnoma_shop/models/order.dart';

void main() {
  group('OrderRequest.toJson', () {
    test('maps all fields to the checkout payload', () {
      final request = OrderRequest(
        customerName: 'Ali',
        phone: '+998901234567',
        regionId: 1,
        villageId: 11,
        note: 'Тез керак',
        lines: const [
          OrderRequestLine(catalogItemId: 100, quantity: 2.5),
        ],
      );

      final json = request.toJson();

      expect(json['customerName'], 'Ali');
      expect(json['phone'], '+998901234567');
      expect(json['regionId'], 1);
      expect(json['villageId'], 11);
      expect(json['note'], 'Тез керак');
      expect(json['lines'], [
        {'catalogItemId': 100, 'quantity': 2.5},
      ]);
    });

    test('omits optional fields when absent', () {
      final request = OrderRequest(
        customerName: 'Ali',
        phone: '+998901234567',
        note: '',
        lines: const [OrderRequestLine(catalogItemId: 1, quantity: 1)],
      );

      final json = request.toJson();

      expect(json.containsKey('regionId'), false);
      expect(json.containsKey('villageId'), false);
      expect(json.containsKey('note'), false);
    });
  });

  group('PublicOrder.fromJson', () {
    test('maps order with lines', () {
      final order = PublicOrder.fromJson({
        'orderNumber': 'WO-000007',
        'status': 'CONFIRMED',
        'totalAmount': 36000.5,
        'currency': 'UZS',
        'lines': [
          {
            'productName': 'Cola',
            'quantity': 3,
            'unitPrice': 12000,
            'lineTotal': 36000,
          },
        ],
      });

      expect(order.orderNumber, 'WO-000007');
      expect(order.status, 'CONFIRMED');
      expect(order.totalAmount, 36000.5);
      expect(order.lines.single.productName, 'Cola');
      expect(order.lines.single.quantity, 3);
    });
  });

  group('Delivery models', () {
    test('region and village map id/name/regionId', () {
      final region = DeliveryRegion.fromJson({'id': 1, 'name': 'Тошкент'});
      final village = DeliveryVillage.fromJson(
          {'id': 11, 'name': 'Чилонзор', 'regionId': 1});

      expect(region.id, 1);
      expect(region.name, 'Тошкент');
      expect(region.deliveryFee, 0); // default when backend omits it
      expect(village.regionId, 1);
    });

    test('region maps delivery fee', () {
      final region = DeliveryRegion.fromJson(
          {'id': 1, 'name': 'Тошкент', 'deliveryFee': 5000.0});

      expect(region.deliveryFee, 5000.0);
    });
  });
}
