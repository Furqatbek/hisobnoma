import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/api/catalog_api.dart';
import 'package:hisobnoma_shop/l10n/strings.dart';
import 'package:hisobnoma_shop/models/auth_store.dart';
import 'package:hisobnoma_shop/models/cart.dart';
import 'package:hisobnoma_shop/models/public_category.dart';
import 'package:hisobnoma_shop/screens/catalog_screen.dart';
import 'package:hisobnoma_shop/widgets/auth_scope.dart';
import 'package:hisobnoma_shop/widgets/cart_scope.dart';
import 'package:hisobnoma_shop/widgets/product_card.dart';

import 'fake_catalog_api.dart';

Future<void> pumpCatalog(WidgetTester tester, CatalogApi api,
    {CartStore? cart}) async {
  await tester.pumpWidget(AuthScope(
    auth: AuthStore(),
    child: CartScope(
      cart: cart ?? CartStore(),
      child: MaterialApp(home: CatalogScreen(api: api)),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('renders product grid with prices and stock badges',
      (tester) async {
    final api = FakeCatalogApi(products: [
      product(1, 'Cola', price: 12000),
      product(2, 'Juice', price: 8000, inStock: false),
    ]);

    await pumpCatalog(tester, api);

    expect(find.byType(ProductCard), findsNWidgets(2));
    expect(find.text('Cola'), findsOneWidget);
    expect(find.text('12 000 сўм'), findsOneWidget);
    expect(find.text(S.inStock), findsOneWidget);
    expect(find.text(S.outOfStock), findsOneWidget);
  });

  testWidgets('shows empty state when catalog has no items', (tester) async {
    await pumpCatalog(tester, FakeCatalogApi());

    expect(find.text(S.emptyCatalog), findsOneWidget);
    expect(find.byType(ProductCard), findsNothing);
  });

  testWidgets('shows error state with retry that reloads', (tester) async {
    final api = FakeCatalogApi(failProducts: true);
    await pumpCatalog(tester, api);

    expect(find.text(S.loadError), findsOneWidget);

    // Recover and retry
    api.failProducts = false;
    api.products = [product(1, 'Cola')];
    await tester.tap(find.text(S.retry));
    await tester.pumpAndSettle();

    expect(find.text('Cola'), findsOneWidget);
  });

  testWidgets('search filters the product list', (tester) async {
    final api = FakeCatalogApi(products: [
      product(1, 'Cola'),
      product(2, 'Juice'),
    ]);
    await pumpCatalog(tester, api);

    await tester.enterText(find.byType(TextField), 'cola');
    // Wait out the 400ms debounce.
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();

    expect(find.text('Cola'), findsOneWidget);
    expect(find.text('Juice'), findsNothing);
  });

  testWidgets('category chips filter the product list', (tester) async {
    final api = FakeCatalogApi(
      products: [
        product(1, 'Cola', categoryId: 10),
        product(2, 'Chips', categoryId: 20),
      ],
      categories: const [
        PublicCategory(id: 10, name: 'Drinks'),
        PublicCategory(id: 20, name: 'Snacks'),
      ],
    );
    await pumpCatalog(tester, api);

    expect(find.byType(ProductCard), findsNWidgets(2));

    await tester.tap(find.text('Snacks'));
    await tester.pumpAndSettle();

    expect(find.text('Chips'), findsOneWidget);
    expect(find.text('Cola'), findsNothing);
  });

  testWidgets('shows no-results message for fruitless search', (tester) async {
    final api = FakeCatalogApi(products: [product(1, 'Cola')]);
    await pumpCatalog(tester, api);

    await tester.enterText(find.byType(TextField), 'zzz');
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pumpAndSettle();

    expect(find.text(S.noResults), findsOneWidget);
  });
}
