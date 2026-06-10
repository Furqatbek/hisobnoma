class PublicCategory {
  final int id;
  final String name;

  const PublicCategory({required this.id, required this.name});

  factory PublicCategory.fromJson(Map<String, dynamic> json) {
    return PublicCategory(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String? ?? '',
    );
  }
}
