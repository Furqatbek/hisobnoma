import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/models/cart.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'fake_catalog_api.dart';

void main() {
  group('CartStore math', () {
    test('add inserts new item and increments existing', () {
      final cart = CartStore();

      cart.add(product(1, 'Cola', price: 12000));
      cart.add(product(1, 'Cola', price: 12000));
      cart.add(product(2, 'Juice', price: 8000), quantity: 3);

      expect(cart.distinctCount, 2);
      expect(cart.quantityOf(1), 2);
      expect(cart.quantityOf(2), 3);
      expect(cart.total, 2 * 12000 + 3 * 8000);
    });

    test('setQuantity updates total and removes at zero', () {
      final cart = CartStore();
      cart.add(product(1, 'Cola', price: 12000));

      cart.setQuantity(1, 5);
      expect(cart.total, 60000);

      cart.setQuantity(1, 0);
      expect(cart.isEmpty, true);
    });

    test('remove and clear empty the cart', () {
      final cart = CartStore();
      cart.add(product(1, 'Cola'));
      cart.add(product(2, 'Juice'));

      cart.remove(1);
      expect(cart.distinctCount, 1);

      cart.clear();
      expect(cart.isEmpty, true);
      expect(cart.total, 0);
    });

    test('notifies listeners on every change', () {
      final cart = CartStore();
      var notifications = 0;
      cart.addListener(() => notifications++);

      cart.add(product(1, 'Cola'));
      cart.setQuantity(1, 2);
      cart.remove(1);

      expect(notifications, 3);
    });
  });

  group('CartStore persistence', () {
    test('cart survives a reload round-trip', () async {
      SharedPreferences.setMockInitialValues({});

      final cart = await CartStore.load();
      cart.add(product(1, 'Cola', price: 12000), quantity: 2);
      cart.add(product(2, 'Juice', price: 8000));
      // _persist is synchronous fire-and-forget; allow it to complete.
      await Future<void>.delayed(Duration.zero);

      final reloaded = await CartStore.load();
      expect(reloaded.distinctCount, 2);
      expect(reloaded.quantityOf(1), 2);
      expect(reloaded.total, 2 * 12000 + 8000);
      expect(reloaded.items.firstWhere((i) => i.catalogItemId == 1).name, 'Cola');
    });

    test('corrupted storage starts with an empty cart', () async {
      SharedPreferences.setMockInitialValues({'cart_v1': 'not json'});

      final cart = await CartStore.load();

      expect(cart.isEmpty, true);
    });
  });
}
