import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/l10n/strings.dart';
import 'package:hisobnoma_shop/models/auth_store.dart';
import 'package:hisobnoma_shop/models/order.dart';
import 'package:hisobnoma_shop/screens/login_screen.dart';
import 'package:hisobnoma_shop/screens/my_orders_screen.dart';
import 'package:hisobnoma_shop/widgets/auth_scope.dart';

import 'fake_catalog_api.dart';

Future<AuthStore> pumpLogin(WidgetTester tester, FakeCatalogApi api) async {
  final auth = AuthStore();
  await tester.pumpWidget(AuthScope(
    auth: auth,
    child: MaterialApp(home: LoginScreen(api: api)),
  ));
  await tester.pumpAndSettle();
  return auth;
}

void main() {
  testWidgets('phone step requests code and reveals code step', (tester) async {
    final api = FakeCatalogApi();
    await pumpLogin(tester, api);

    await tester.enterText(find.byType(TextField).first, '+998901234567');
    await tester.tap(find.text(S.sendCode));
    await tester.pumpAndSettle();

    expect(api.requestedOtpPhone, '+998901234567');
    expect(find.text(S.smsCode), findsOneWidget);
  });

  testWidgets('correct code logs in and opens my orders', (tester) async {
    final api = FakeCatalogApi(myOrders: const [
      PublicOrder(orderNumber: 'WO-000042', status: 'CONFIRMED',
          totalAmount: 12000, currency: 'UZS'),
    ]);
    final auth = await pumpLogin(tester, api);

    await tester.enterText(find.byType(TextField).first, '+998901234567');
    await tester.tap(find.text(S.sendCode));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextField, S.smsCode), '123456');
    await tester.enterText(
        find.widgetWithText(TextField, S.nameOptional), 'Ali');
    await tester.tap(find.text(S.confirmCode));
    await tester.pumpAndSettle();

    expect(auth.isLoggedIn, true);
    expect(auth.name, 'Ali');
    expect(find.byType(MyOrdersScreen), findsOneWidget);
    expect(find.text('WO-000042'), findsOneWidget);
  });

  testWidgets('wrong code shows error and stays on login', (tester) async {
    final api = FakeCatalogApi();
    final auth = await pumpLogin(tester, api);

    await tester.enterText(find.byType(TextField).first, '+998901234567');
    await tester.tap(find.text(S.sendCode));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextField, S.smsCode), '999999');
    await tester.tap(find.text(S.confirmCode));
    await tester.pumpAndSettle();

    expect(auth.isLoggedIn, false);
    expect(find.text(S.invalidCode), findsOneWidget);
  });
}
