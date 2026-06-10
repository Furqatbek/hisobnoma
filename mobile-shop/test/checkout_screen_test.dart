import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/l10n/strings.dart';
import 'package:hisobnoma_shop/models/auth_store.dart';
import 'package:hisobnoma_shop/models/cart.dart';
import 'package:hisobnoma_shop/models/delivery.dart';
import 'package:hisobnoma_shop/screens/checkout_screen.dart';
import 'package:hisobnoma_shop/screens/order_success_screen.dart';
import 'package:hisobnoma_shop/widgets/auth_scope.dart';
import 'package:hisobnoma_shop/widgets/cart_scope.dart';

import 'fake_catalog_api.dart';

Future<void> pumpCheckout(WidgetTester tester, FakeCatalogApi api,
    CartStore cart) async {
  await tester.pumpWidget(AuthScope(
    auth: AuthStore(),
    child: CartScope(
      cart: cart,
      child: MaterialApp(home: CheckoutScreen(api: api)),
    ),
  ));
  await tester.pumpAndSettle();
}

CartStore cartWith(List<(int, String, double, double)> entries) {
  final cart = CartStore();
  for (final (id, name, price, qty) in entries) {
    cart.add(product(id, name, price: price), quantity: qty);
  }
  return cart;
}

void main() {
  testWidgets('empty name and short phone block submission', (tester) async {
    final api = FakeCatalogApi();
    await pumpCheckout(tester, api, cartWith([(1, 'Cola', 12000, 1)]));

    await tester.enterText(
        find.widgetWithText(TextFormField, S.phoneNumber), '+998');
    await tester.tap(find.text(S.submitOrder));
    await tester.pumpAndSettle();

    expect(find.text(S.fieldRequired), findsOneWidget); // name
    expect(find.text(S.invalidPhone), findsOneWidget); // phone too short
    expect(api.lastOrderRequest, isNull);
  });

  testWidgets('valid form submits payload built from the cart and clears it',
      (tester) async {
    final api = FakeCatalogApi();
    final cart = cartWith([(1, 'Cola', 12000, 2), (5, 'Juice', 8000, 1)]);
    await pumpCheckout(tester, api, cart);

    await tester.enterText(
        find.widgetWithText(TextFormField, S.customerName), 'Ali Valiyev');
    await tester.enterText(
        find.widgetWithText(TextFormField, S.phoneNumber), '+998901234567');
    await tester.enterText(
        find.widgetWithText(TextFormField, S.noteOptional), 'Тез керак');
    await tester.tap(find.text(S.submitOrder));
    await tester.pumpAndSettle();

    final request = api.lastOrderRequest;
    expect(request, isNotNull);
    expect(request!.customerName, 'Ali Valiyev');
    expect(request.phone, '+998901234567');
    expect(request.note, 'Тез керак');
    expect(request.lines, hasLength(2));
    expect(request.lines.first.catalogItemId, 1);
    expect(request.lines.first.quantity, 2);

    // Cart cleared and success screen shown with the order number
    expect(cart.isEmpty, true);
    expect(find.byType(OrderSuccessScreen), findsOneWidget);
    expect(find.text('WO-000042'), findsOneWidget);
  });

  testWidgets('rate-limited checkout shows the try-later message',
      (tester) async {
    final api = FakeCatalogApi(failOrder: true, orderStatusCode: 429);
    final cart = cartWith([(1, 'Cola', 12000, 1)]);
    await pumpCheckout(tester, api, cart);

    await tester.enterText(
        find.widgetWithText(TextFormField, S.customerName), 'Ali');
    await tester.enterText(
        find.widgetWithText(TextFormField, S.phoneNumber), '+998901234567');
    await tester.tap(find.text(S.submitOrder));
    await tester.pumpAndSettle();

    expect(find.text(S.tooManyAttempts), findsOneWidget);
    expect(cart.isEmpty, false); // cart kept so the user can retry
  });

  testWidgets('region select loads villages for the chosen region',
      (tester) async {
    final api = FakeCatalogApi(
      regions: const [DeliveryRegion(id: 1, name: 'Тошкент')],
      villagesByRegion: const {
        1: [DeliveryVillage(id: 11, name: 'Чилонзор', regionId: 1)],
      },
    );
    await pumpCheckout(tester, api, cartWith([(1, 'Cola', 12000, 1)]));

    await tester.tap(find.text(S.region));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Тошкент').last);
    await tester.pumpAndSettle();

    expect(find.text(S.village), findsOneWidget);

    await tester.tap(find.text(S.village));
    await tester.pumpAndSettle();
    expect(find.text('Чилонзор'), findsWidgets);
  });
}
