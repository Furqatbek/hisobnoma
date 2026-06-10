import 'package:flutter/material.dart';

import 'api/catalog_api.dart';
import 'l10n/strings.dart';
import 'models/cart.dart';
import 'screens/catalog_screen.dart';
import 'widgets/cart_scope.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final cart = await CartStore.load();
  runApp(ShopApp(api: HttpCatalogApi(), cart: cart));
}

class ShopApp extends StatelessWidget {
  final CatalogApi api;
  final CartStore cart;

  const ShopApp({super.key, required this.api, required this.cart});

  @override
  Widget build(BuildContext context) {
    return CartScope(
      cart: cart,
      child: MaterialApp(
        title: S.appTitle,
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
          useMaterial3: true,
        ),
        home: CatalogScreen(api: api),
      ),
    );
  }
}
