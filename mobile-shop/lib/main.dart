import 'package:flutter/material.dart';

import 'api/catalog_api.dart';
import 'l10n/strings.dart';
import 'screens/catalog_screen.dart';

void main() {
  runApp(ShopApp(api: HttpCatalogApi()));
}

class ShopApp extends StatelessWidget {
  final CatalogApi api;

  const ShopApp({super.key, required this.api});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: S.appTitle,
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      home: CatalogScreen(api: api),
    );
  }
}
