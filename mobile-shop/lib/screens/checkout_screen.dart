import 'package:flutter/material.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../models/delivery.dart';
import '../models/order.dart';
import '../util/format.dart';
import '../widgets/cart_scope.dart';
import 'order_success_screen.dart';

class CheckoutScreen extends StatefulWidget {
  final CatalogApi api;

  const CheckoutScreen({super.key, required this.api});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController(text: '+998');
  final _noteController = TextEditingController();

  List<DeliveryRegion> _regions = [];
  List<DeliveryVillage> _villages = [];
  DeliveryRegion? _selectedRegion;
  DeliveryVillage? _selectedVillage;

  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _loadRegions();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _loadRegions() async {
    try {
      final regions = await widget.api.getRegions();
      if (mounted) setState(() => _regions = regions);
    } catch (_) {
      // Delivery selects are optional; the form still works without them.
    }
  }

  Future<void> _onRegionChanged(DeliveryRegion? region) async {
    setState(() {
      _selectedRegion = region;
      _selectedVillage = null;
      _villages = [];
    });
    if (region == null) return;
    try {
      final villages = await widget.api.getVillages(region.id);
      if (mounted && _selectedRegion?.id == region.id) {
        setState(() => _villages = villages);
      }
    } catch (_) {
      // Keep the village list empty on failure.
    }
  }

  String? _validateName(String? value) =>
      value == null || value.trim().isEmpty ? S.fieldRequired : null;

  String? _validatePhone(String? value) {
    if (value == null || value.trim().isEmpty) return S.fieldRequired;
    final digits = value.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length < 9 || digits.length > 15) return S.invalidPhone;
    return null;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || _submitting) return;

    final cart = CartScope.of(context);
    final request = OrderRequest(
      customerName: _nameController.text.trim(),
      phone: _phoneController.text.trim(),
      regionId: _selectedRegion?.id,
      villageId: _selectedVillage?.id,
      note: _noteController.text.trim(),
      lines: cart.items
          .map((item) => OrderRequestLine(
              catalogItemId: item.catalogItemId, quantity: item.quantity))
          .toList(),
    );

    setState(() => _submitting = true);
    try {
      final order = await widget.api.submitOrder(request);
      cart.clear();
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(
            builder: (_) => OrderSuccessScreen(api: widget.api, order: order)),
        (route) => route.isFirst,
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(e.statusCode == 429 ? S.tooManyAttempts : S.orderFailed)));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text(S.orderFailed)));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cart = CartScope.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text(S.checkoutTitle)),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextFormField(
              controller: _nameController,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: S.customerName,
                border: OutlineInputBorder(),
              ),
              validator: _validateName,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _phoneController,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: S.phoneNumber,
                border: OutlineInputBorder(),
              ),
              validator: _validatePhone,
            ),
            const SizedBox(height: 12),
            if (_regions.isNotEmpty) ...[
              DropdownButtonFormField<DeliveryRegion>(
                initialValue: _selectedRegion,
                decoration: const InputDecoration(
                  labelText: S.region,
                  border: OutlineInputBorder(),
                ),
                items: [
                  for (final region in _regions)
                    DropdownMenuItem(value: region, child: Text(region.name)),
                ],
                onChanged: _onRegionChanged,
              ),
              const SizedBox(height: 12),
              if (_villages.isNotEmpty) ...[
                DropdownButtonFormField<DeliveryVillage>(
                  initialValue: _selectedVillage,
                  decoration: const InputDecoration(
                    labelText: S.village,
                    border: OutlineInputBorder(),
                  ),
                  items: [
                    for (final village in _villages)
                      DropdownMenuItem(value: village, child: Text(village.name)),
                  ],
                  onChanged: (v) => setState(() => _selectedVillage = v),
                ),
                const SizedBox(height: 12),
              ],
            ],
            TextFormField(
              controller: _noteController,
              maxLines: 2,
              maxLength: 500,
              decoration: const InputDecoration(
                labelText: S.noteOptional,
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
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
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _submitting ? null : _submit,
                child: Text(_submitting ? S.submitting : S.submitOrder),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
