import 'package:flutter/material.dart';

import '../config/app_config.dart';
import '../l10n/strings.dart';
import '../models/public_product.dart';
import '../util/format.dart';

class ProductCard extends StatelessWidget {
  final PublicProduct product;
  final VoidCallback onTap;

  const ProductCard({super.key, required this.product, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final imageUrl = AppConfig.resolveImageUrl(product.imageUrl);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AspectRatio(
              aspectRatio: 1.2,
              child: imageUrl.isEmpty
                  ? Container(
                      color: theme.colorScheme.surfaceContainerHighest,
                      child: const Icon(Icons.image_outlined,
                          size: 40, color: Colors.grey),
                    )
                  : Image.network(
                      imageUrl,
                      fit: BoxFit.cover,
                      errorBuilder: (_, _, _) => Container(
                        color: theme.colorScheme.surfaceContainerHighest,
                        child: const Icon(Icons.broken_image_outlined,
                            size: 40, color: Colors.grey),
                      ),
                    ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      product.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(fontWeight: FontWeight.w600),
                    ),
                    const Spacer(),
                    Text(
                      formatUzs(product.price),
                      style: theme.textTheme.titleSmall
                          ?.copyWith(color: theme.colorScheme.primary),
                    ),
                    const SizedBox(height: 4),
                    _StockBadge(inStock: product.inStock),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StockBadge extends StatelessWidget {
  final bool inStock;

  const _StockBadge({required this.inStock});

  @override
  Widget build(BuildContext context) {
    final color = inStock ? Colors.green : Colors.red;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Text(
        inStock ? S.inStock : S.outOfStock,
        style: TextStyle(
            fontSize: 11, color: color.shade700, fontWeight: FontWeight.w600),
      ),
    );
  }
}
