import 'package:flutter/material.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../models/order.dart';
import '../util/format.dart';

class OrderStatusScreen extends StatefulWidget {
  final CatalogApi api;
  final String? initialOrderNumber;

  const OrderStatusScreen({super.key, required this.api, this.initialOrderNumber});

  @override
  State<OrderStatusScreen> createState() => _OrderStatusScreenState();
}

class _OrderStatusScreenState extends State<OrderStatusScreen> {
  late final TextEditingController _numberController;
  final _phoneController = TextEditingController(text: '+998');

  PublicOrder? _order;
  bool _loading = false;
  bool _notFound = false;

  @override
  void initState() {
    super.initState();
    _numberController =
        TextEditingController(text: widget.initialOrderNumber ?? '');
  }

  @override
  void dispose() {
    _numberController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _lookup() async {
    final number = _numberController.text.trim();
    final phone = _phoneController.text.trim();
    if (number.isEmpty || phone.isEmpty || _loading) return;

    setState(() {
      _loading = true;
      _notFound = false;
      _order = null;
    });
    try {
      final order = await widget.api.getOrderStatus(number, phone);
      if (mounted) setState(() => _order = order);
    } catch (_) {
      if (mounted) setState(() => _notFound = true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'NEW':
        return Colors.orange;
      case 'CONFIRMED':
        return Colors.blue;
      case 'DELIVERING':
        return Colors.amber.shade800;
      case 'COMPLETED':
        return Colors.green;
      case 'CANCELLED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final order = _order;

    return Scaffold(
      appBar: AppBar(title: const Text(S.orderStatusTitle)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _numberController,
            decoration: const InputDecoration(
              labelText: S.orderNumberField,
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _phoneController,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(
              labelText: S.phoneNumber,
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _loading ? null : _lookup,
            icon: const Icon(Icons.search),
            label: Text(_loading ? S.loadingMore : S.find),
          ),
          const SizedBox(height: 24),
          if (_notFound)
            Text(S.orderNotFound,
                style: const TextStyle(color: Colors.red),
                textAlign: TextAlign.center),
          if (order != null) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(order.orderNumber,
                            style: theme.textTheme.titleMedium),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: _statusColor(order.status)
                                .withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            S.statusName(order.status),
                            style: TextStyle(
                              color: _statusColor(order.status),
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 24),
                    for (final line in order.lines)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 6),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                  '${line.productName} × ${line.quantity == line.quantity.truncateToDouble() ? line.quantity.toInt() : line.quantity}'),
                            ),
                            Text(formatUzs(line.lineTotal)),
                          ],
                        ),
                      ),
                    const Divider(height: 24),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(S.total,
                            style: TextStyle(fontWeight: FontWeight.w600)),
                        Text(formatUzs(order.totalAmount),
                            style: const TextStyle(fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
