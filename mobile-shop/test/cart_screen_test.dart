import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/l10n/strings.dart';
import 'package:hisobnoma_shop/models/cart.dart';
import 'package:hisobnoma_shop/models/order.dart';
import 'package:hisobnoma_shop/screens/cart_screen.dart';
import 'package:hisobnoma_shop/widgets/cart_scope.dart';

import 'fake_catalog_api.dart';

Future<void> pumpCart(WidgetTester tester, FakeCatalogApi api,
    CartStore cart) async {
  await tester.pumpWidget(CartScope(
    cart: cart,
    child: MaterialApp(home: CartScreen(api: api)),
  ));
  // Let the 400ms price-preview debounce fire and the fake API resolve.
  await tester.pump(const Duration(milliseconds: 450));
  await tester.pump();
}

CartStore cartWith(List<(int, String, double, double)> entries) {
  final cart = CartStore();
  for (final (id, name, price, qty) in entries) {
    cart.add(product(id, name, price: price), quantity: qty);
  }
  return cart;
}

void main() {
  testWidgets('shows server discount row and discounted total', (tester) async {
    final api = FakeCatalogApi(products: [product(1, 'Cola', price: 12000)])
      ..cartPrice = const CartPrice(
        subtotal: 36000,
        discountTotal: 3600,
        total: 32400,
        appliedPromotions: ['Ёзги акция'],
      );

    await pumpCart(tester, api, cartWith([(1, 'Cola', 12000, 3)]));

    expect(api.priceCartCalls, 1);
    expect(api.lastPricedLines!.single.catalogItemId, 1);
    expect(find.textContaining(S.discount), findsOneWidget);
    expect(find.textContaining('Ёзги акция'), findsOneWidget);
    expect(find.text('−3 600 сўм'), findsOneWidget);
    expect(find.text('32 400 сўм'), findsOneWidget);
  });

  testWidgets('no discount keeps the plain local total', (tester) async {
    final api = FakeCatalogApi(products: [product(1, 'Cola', price: 12000)]);

    await pumpCart(tester, api, cartWith([(1, 'Cola', 12000, 3)]));

    expect(find.textContaining(S.discount), findsNothing);
    expect(find.text('36 000 сўм'), findsOneWidget);
  });

  testWidgets('preview failure falls back to local total', (tester) async {
    final api = FakeCatalogApi(products: [product(1, 'Cola', price: 12000)]);
    api.failPriceCart = true;

    await pumpCart(tester, api, cartWith([(1, 'Cola', 12000, 3)]));

    expect(find.textContaining(S.discount), findsNothing);
    expect(find.text('36 000 сўм'), findsOneWidget);
  });
}
