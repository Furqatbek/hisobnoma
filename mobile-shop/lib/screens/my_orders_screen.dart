import 'package:flutter/material.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../models/order.dart';
import '../util/format.dart';
import '../widgets/auth_scope.dart';
import '../widgets/status_views.dart';

class MyOrdersScreen extends StatefulWidget {
  final CatalogApi api;

  const MyOrdersScreen({super.key, required this.api});

  @override
  State<MyOrdersScreen> createState() => _MyOrdersScreenState();
}

class _MyOrdersScreenState extends State<MyOrdersScreen> {
  List<PublicOrder> _orders = [];
  bool _loading = true;
  bool _error = false;

  @override
  void initState() {
    super.initState();
    // AuthScope is an inherited widget — defer until the first frame.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _load();
    });
  }

  Future<void> _load() async {
    final auth = AuthScope.of(context);
    if (!auth.isLoggedIn) return;
    setState(() {
      _loading = true;
      _error = false;
    });
    try {
      final page = await widget.api.getMyOrders(auth.token!);
      if (mounted) {
        setState(() {
          _orders = page.content;
          _loading = false;
        });
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.statusCode == 401) {
        // Session expired — drop it and send the user back.
        auth.logout();
        Navigator.of(context).pop();
        return;
      }
      setState(() {
        _loading = false;
        _error = true;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = true;
        });
      }
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

  void _logout() {
    AuthScope.of(context).logout();
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final auth = AuthScope.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text(S.myOrders),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: S.logout,
            onPressed: _logout,
          ),
        ],
      ),
      body: Column(
        children: [
          if (auth.isLoggedIn)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: Row(
                children: [
                  const Icon(Icons.person_outline, color: Colors.grey),
                  const SizedBox(width: 8),
                  Text(
                    [if (auth.name != null && auth.name!.isNotEmpty) auth.name, '+${auth.phone}']
                        .whereType<String>()
                        .join(' · '),
                    style: const TextStyle(color: Colors.grey),
                  ),
                ],
              ),
            ),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error) {
      return ErrorRetryView(onRetry: _load);
    }
    if (_orders.isEmpty) {
      return const EmptyView(message: S.noOrdersYet);
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(12),
        physics: const AlwaysScrollableScrollPhysics(),
        itemCount: _orders.length,
        separatorBuilder: (_, _) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final order = _orders[index];
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(order.orderNumber,
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: _statusColor(order.status).withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          S.statusName(order.status),
                          style: TextStyle(
                            fontSize: 12,
                            color: _statusColor(order.status),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  for (final line in order.lines)
                    Text(
                      '${line.productName} × ${line.quantity == line.quantity.truncateToDouble() ? line.quantity.toInt() : line.quantity}',
                      style: const TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                  const SizedBox(height: 6),
                  Text(formatUzs(order.totalAmount),
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
