class DeliveryRegion {
  final int id;
  final String name;
  final double deliveryFee;

  const DeliveryRegion(
      {required this.id, required this.name, this.deliveryFee = 0});

  factory DeliveryRegion.fromJson(Map<String, dynamic> json) {
    return DeliveryRegion(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String? ?? '',
      deliveryFee: (json['deliveryFee'] as num?)?.toDouble() ?? 0,
    );
  }
}

class DeliveryVillage {
  final int id;
  final String name;
  final int? regionId;

  const DeliveryVillage({required this.id, required this.name, this.regionId});

  factory DeliveryVillage.fromJson(Map<String, dynamic> json) {
    return DeliveryVillage(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String? ?? '',
      regionId: (json['regionId'] as num?)?.toInt(),
    );
  }
}
