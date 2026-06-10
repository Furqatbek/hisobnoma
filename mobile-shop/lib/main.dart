import 'package:flutter/material.dart';

import 'api/catalog_api.dart';
import 'l10n/strings.dart';
import 'models/auth_store.dart';
import 'models/cart.dart';
import 'screens/catalog_screen.dart';
import 'widgets/auth_scope.dart';
import 'widgets/cart_scope.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final cart = await CartStore.load();
  final auth = await AuthStore.load();
  runApp(ShopApp(api: HttpCatalogApi(), cart: cart, auth: auth));
}

class ShopApp extends StatelessWidget {
  final CatalogApi api;
  final CartStore cart;
  final AuthStore auth;

  const ShopApp({super.key, required this.api, required this.cart, required this.auth});

  @override
  Widget build(BuildContext context) {
    return AuthScope(
      auth: auth,
      child: CartScope(
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
      ),
    );
  }
}
