import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'public_product.dart';

class CartItem {
  final int catalogItemId;
  final String name;
  final double price;
  final String? imageUrl;
  final String? unitName;
  double quantity;

  CartItem({
    required this.catalogItemId,
    required this.name,
    required this.price,
    this.imageUrl,
    this.unitName,
    this.quantity = 1,
  });

  double get lineTotal => price * quantity;

  Map<String, dynamic> toJson() => {
        'catalogItemId': catalogItemId,
        'name': name,
        'price': price,
        'imageUrl': imageUrl,
        'unitName': unitName,
        'quantity': quantity,
      };

  factory CartItem.fromJson(Map<String, dynamic> json) => CartItem(
        catalogItemId: (json['catalogItemId'] as num).toInt(),
        name: json['name'] as String? ?? '',
        price: (json['price'] as num?)?.toDouble() ?? 0,
        imageUrl: json['imageUrl'] as String?,
        unitName: json['unitName'] as String?,
        quantity: (json['quantity'] as num?)?.toDouble() ?? 1,
      );
}

/// Shopping cart persisted to SharedPreferences so it survives app restarts.
class CartStore extends ChangeNotifier {
  static const _storageKey = 'cart_v1';

  final Map<int, CartItem> _items = {};
  final SharedPreferences? _prefs;

  CartStore({SharedPreferences? prefs}) : _prefs = prefs; // ignore: prefer_initializing_formals

  /// Loads the persisted cart.
  static Future<CartStore> load() async {
    final prefs = await SharedPreferences.getInstance();
    final store = CartStore(prefs: prefs);
    final raw = prefs.getString(_storageKey);
    if (raw != null && raw.isNotEmpty) {
      try {
        for (final entry in jsonDecode(raw) as List<dynamic>) {
          final item = CartItem.fromJson(entry as Map<String, dynamic>);
          store._items[item.catalogItemId] = item;
        }
      } catch (_) {
        // Corrupted cart data — start fresh.
      }
    }
    return store;
  }

  List<CartItem> get items => List.unmodifiable(_items.values);

  bool get isEmpty => _items.isEmpty;

  int get distinctCount => _items.length;

  double get total =>
      _items.values.fold(0, (sum, item) => sum + item.lineTotal);

  bool contains(int catalogItemId) => _items.containsKey(catalogItemId);

  double quantityOf(int catalogItemId) =>
      _items[catalogItemId]?.quantity ?? 0;

  /// Adds a product (or increments its quantity when already in the cart).
  void add(PublicProduct product, {double quantity = 1}) {
    final existing = _items[product.id];
    if (existing != null) {
      existing.quantity += quantity;
    } else {
      _items[product.id] = CartItem(
        catalogItemId: product.id,
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrl,
        unitName: product.unitName,
        quantity: quantity,
      );
    }
    _changed();
  }

  /// Sets an exact quantity; zero or less removes the item.
  void setQuantity(int catalogItemId, double quantity) {
    if (quantity <= 0) {
      _items.remove(catalogItemId);
    } else {
      _items[catalogItemId]?.quantity = quantity;
    }
    _changed();
  }

  void remove(int catalogItemId) {
    _items.remove(catalogItemId);
    _changed();
  }

  void clear() {
    _items.clear();
    _changed();
  }

  void _changed() {
    notifyListeners();
    _persist();
  }

  void _persist() {
    _prefs?.setString(
        _storageKey, jsonEncode(_items.values.map((i) => i.toJson()).toList()));
  }
}
