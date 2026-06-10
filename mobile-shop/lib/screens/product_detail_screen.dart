import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/catalog_api.dart';
import '../config/app_config.dart';
import '../l10n/strings.dart';
import '../models/public_product.dart';
import '../util/format.dart';
import '../widgets/status_views.dart';

class ProductDetailScreen extends StatefulWidget {
  final CatalogApi api;
  final int productId;

  /// Product from the list, shown immediately while the detail loads.
  final PublicProduct? initialProduct;

  const ProductDetailScreen({
    super.key,
    required this.api,
    required this.productId,
    this.initialProduct,
  });

  @override
  State<ProductDetailScreen> createState() => _ProductDetailScreenState();
}

class _ProductDetailScreenState extends State<ProductDetailScreen> {
  PublicProduct? _product;
  bool _error = false;

  @override
  void initState() {
    super.initState();
    _product = widget.initialProduct;
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = false);
    try {
      final product = await widget.api.getProduct(widget.productId);
      if (mounted) setState(() => _product = product);
    } catch (_) {
      // Keep showing the list snapshot if we have one; otherwise show retry.
      if (mounted && _product == null) setState(() => _error = true);
    }
  }

  Future<void> _launch(Uri uri) async {
    try {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      // Nothing sensible to do on a phone without a dialer/Telegram.
    }
  }

  @override
  Widget build(BuildContext context) {
    final product = _product;
    return Scaffold(
      appBar: AppBar(title: Text(product?.name ?? '')),
      body: _error
          ? ErrorRetryView(onRetry: _load)
          : product == null
              ? const Center(child: CircularProgressIndicator())
              : _ProductDetailBody(product: product, onOrder: _launch),
    );
  }
}

class _ProductDetailBody extends StatelessWidget {
  final PublicProduct product;
  final void Function(Uri) onOrder;

  const _ProductDetailBody({required this.product, required this.onOrder});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final images = product.images.isNotEmpty
        ? product.images
        : [if (product.imageUrl != null) product.imageUrl!];

    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        if (images.isNotEmpty)
          SizedBox(
            height: 280,
            child: PageView(
              children: [
                for (final image in images)
                  Image.network(
                    AppConfig.resolveImageUrl(image),
                    fit: BoxFit.contain,
                    errorBuilder: (_, _, _) => const Center(
                      child: Icon(Icons.broken_image_outlined,
                          size: 64, color: Colors.grey),
                    ),
                  ),
              ],
            ),
          )
        else
          Container(
            height: 200,
            color: theme.colorScheme.surfaceContainerHighest,
            child: const Icon(Icons.image_outlined, size: 64, color: Colors.grey),
          ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(product.name, style: theme.textTheme.headlineSmall),
              if (product.shortDescription != null &&
                  product.shortDescription!.isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(product.shortDescription!,
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(color: Colors.grey.shade700)),
              ],
              const SizedBox(height: 12),
              Row(
                children: [
                  Text(
                    formatUzs(product.price),
                    style: theme.textTheme.headlineSmall
                        ?.copyWith(color: theme.colorScheme.primary),
                  ),
                  if (product.unitName != null) ...[
                    const SizedBox(width: 6),
                    Text('/ ${product.unitName}',
                        style: theme.textTheme.bodyMedium
                            ?.copyWith(color: Colors.grey)),
                  ],
                  const Spacer(),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: (product.inStock ? Colors.green : Colors.red)
                          .withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      product.inStock ? S.inStock : S.outOfStock,
                      style: TextStyle(
                        color: product.inStock
                            ? Colors.green.shade700
                            : Colors.red.shade700,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
              if (product.brandName != null || product.categoryName != null) ...[
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: [
                    if (product.categoryName != null)
                      Chip(label: Text(product.categoryName!)),
                    if (product.brandName != null)
                      Chip(label: Text(product.brandName!)),
                  ],
                ),
              ],
              if (product.description != null &&
                  product.description!.isNotEmpty) ...[
                const SizedBox(height: 16),
                Text(S.description, style: theme.textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(product.description!, style: theme.textTheme.bodyMedium),
              ],
              const SizedBox(height: 24),
              // Until in-app checkout ships (Phase 3), orders go via phone
              // or Telegram, configured at build time.
              if (AppConfig.shopPhone.isNotEmpty)
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () =>
                        onOrder(Uri.parse('tel:${AppConfig.shopPhone}')),
                    icon: const Icon(Icons.phone),
                    label: const Text(S.orderByPhone),
                  ),
                ),
              if (AppConfig.shopTelegram.isNotEmpty) ...[
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () =>
                        onOrder(Uri.parse(AppConfig.shopTelegram)),
                    icon: const Icon(Icons.send),
                    label: const Text(S.orderByTelegram),
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}
