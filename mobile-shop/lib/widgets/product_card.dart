import 'package:flutter/material.dart';

import '../config/app_config.dart';
import '../l10n/strings.dart';
import '../models/public_product.dart';
import '../util/format.dart';
import 'cart_scope.dart';

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
                    if (product.salePrice != null) ...[
                      Text(
                        formatUzs(product.price),
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: Colors.grey,
                          decoration: TextDecoration.lineThrough,
                        ),
                      ),
                      Row(
                        children: [
                          Flexible(
                            child: Text(
                              formatUzs(product.salePrice),
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.titleSmall?.copyWith(
                                  color: theme.colorScheme.primary,
                                  fontWeight: FontWeight.bold),
                            ),
                          ),
                          if (product.promotionLabel != null) ...[
                            const SizedBox(width: 4),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 5, vertical: 1),
                              decoration: BoxDecoration(
                                color: Colors.red.withValues(alpha: 0.1),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                product.promotionLabel!,
                                style: TextStyle(
                                    fontSize: 11,
                                    color: Colors.red.shade700,
                                    fontWeight: FontWeight.w700),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ] else
                      Text(
                        formatUzs(product.price),
                        style: theme.textTheme.titleSmall
                            ?.copyWith(color: theme.colorScheme.primary),
                      ),
                    const SizedBox(height: 4),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        _StockBadge(inStock: product.inStock),
                        if (product.inStock)
                          InkWell(
                            customBorder: const CircleBorder(),
                            onTap: () {
                              CartScope.of(context).add(product);
                              ScaffoldMessenger.of(context)
                                ..hideCurrentSnackBar()
                                ..showSnackBar(const SnackBar(
                                  content: Text(S.addedToCart),
                                  duration: Duration(seconds: 1),
                                ));
                            },
                            child: Padding(
                              padding: const EdgeInsets.all(2),
                              child: Icon(Icons.add_shopping_cart,
                                  size: 20, color: theme.colorScheme.primary),
                            ),
                          ),
                      ],
                    ),
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
