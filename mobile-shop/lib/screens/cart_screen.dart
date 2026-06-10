import 'package:flutter/material.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../util/format.dart';
import '../widgets/cart_scope.dart';
import 'checkout_screen.dart';

class CartScreen extends StatelessWidget {
  final CatalogApi api;

  const CartScreen({super.key, required this.api});

  @override
  Widget build(BuildContext context) {
    final cart = CartScope.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text(S.cartTitle)),
      body: cart.isEmpty
          ? const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.shopping_cart_outlined, size: 56, color: Colors.grey),
                  SizedBox(height: 12),
                  Text(S.emptyCart, style: TextStyle(fontSize: 16)),
                ],
              ),
            )
          : ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: cart.items.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final item = cart.items[index];
                return ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(item.name, maxLines: 2, overflow: TextOverflow.ellipsis),
                  subtitle: Text(formatUzs(item.price)),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.remove_circle_outline),
                        onPressed: () =>
                            cart.setQuantity(item.catalogItemId, item.quantity - 1),
                      ),
                      Text(
                        item.quantity == item.quantity.truncateToDouble()
                            ? item.quantity.toInt().toString()
                            : item.quantity.toString(),
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                      ),
                      IconButton(
                        icon: const Icon(Icons.add_circle_outline),
                        onPressed: () =>
                            cart.setQuantity(item.catalogItemId, item.quantity + 1),
                      ),
                      IconButton(
                        icon: const Icon(Icons.delete_outline, color: Colors.red),
                        tooltip: S.removeItem,
                        onPressed: () => cart.remove(item.catalogItemId),
                      ),
                    ],
                  ),
                );
              },
            ),
      bottomNavigationBar: cart.isEmpty
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(S.total,
                            style: TextStyle(fontSize: 16, color: Colors.grey)),
                        Text(formatUzs(cart.total),
                            style: const TextStyle(
                                fontSize: 18, fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton.icon(
                        onPressed: () => Navigator.of(context).push(
                          MaterialPageRoute(
                              builder: (_) => CheckoutScreen(api: api)),
                        ),
                        icon: const Icon(Icons.arrow_forward),
                        label: const Text(S.checkout),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
