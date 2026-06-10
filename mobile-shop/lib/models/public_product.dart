/// Customer-facing catalog product as served by
/// GET /api/v1/web/catalog/products (see docs/API.md in the backend repo).
class PublicProduct {
  final int id;
  final String name;
  final String? shortDescription;
  final String? description;
  final double price;
  final String currency;
  final int? categoryId;
  final String? categoryName;
  final String? brandName;
  final String? unitName;
  final bool inStock;
  final String? imageUrl;
  final List<String> images;

  const PublicProduct({
    required this.id,
    required this.name,
    this.shortDescription,
    this.description,
    required this.price,
    required this.currency,
    this.categoryId,
    this.categoryName,
    this.brandName,
    this.unitName,
    required this.inStock,
    this.imageUrl,
    this.images = const [],
  });

  factory PublicProduct.fromJson(Map<String, dynamic> json) {
    return PublicProduct(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String? ?? '',
      shortDescription: json['shortDescription'] as String?,
      description: json['description'] as String?,
      price: (json['price'] as num?)?.toDouble() ?? 0,
      currency: json['currency'] as String? ?? 'UZS',
      categoryId: (json['categoryId'] as num?)?.toInt(),
      categoryName: json['categoryName'] as String?,
      brandName: json['brandName'] as String?,
      unitName: json['unitName'] as String?,
      inStock: json['inStock'] as bool? ?? false,
      imageUrl: json['imageUrl'] as String?,
      images: (json['images'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const [],
    );
  }
}
