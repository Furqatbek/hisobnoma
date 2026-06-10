import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../models/order.dart';
import '../util/format.dart';
import 'order_status_screen.dart';

class OrderSuccessScreen extends StatelessWidget {
  final CatalogApi api;
  final PublicOrder order;

  const OrderSuccessScreen({super.key, required this.api, required this.order});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(automaticallyImplyLeading: false),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Icon(Icons.check_circle, size: 80, color: Colors.green),
            const SizedBox(height: 16),
            Text(S.orderAccepted,
                style: theme.textTheme.headlineSmall,
                textAlign: TextAlign.center),
            const SizedBox(height: 24),
            Text(S.yourOrderNumber,
                style: theme.textTheme.bodyMedium?.copyWith(color: Colors.grey),
                textAlign: TextAlign.center),
            const SizedBox(height: 4),
            InkWell(
              onTap: () {
                Clipboard.setData(ClipboardData(text: order.orderNumber));
                ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(order.orderNumber)));
              },
              child: Text(
                order.orderNumber,
                style: theme.textTheme.headlineMedium
                    ?.copyWith(color: theme.colorScheme.primary),
                textAlign: TextAlign.center,
              ),
            ),
            const SizedBox(height: 8),
            Text(formatUzs(order.totalAmount),
                style: theme.textTheme.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            Text(S.orderSuccessHint,
                style: theme.textTheme.bodyMedium?.copyWith(color: Colors.grey),
                textAlign: TextAlign.center),
            const SizedBox(height: 32),
            FilledButton(
              onPressed: () =>
                  Navigator.of(context).popUntil((route) => route.isFirst),
              child: const Text(S.backToCatalog),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => OrderStatusScreen(
                      api: api, initialOrderNumber: order.orderNumber))),
              child: const Text(S.checkOrderStatus),
            ),
          ],
        ),
      ),
    );
  }
}
