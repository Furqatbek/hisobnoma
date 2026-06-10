/// Page of results as returned by the backend's PageResponse wrapper:
/// { "content": [...], "page": { "number": 0, "totalPages": 3, "last": false } }
class PageResult<T> {
  final List<T> content;
  final int number;
  final int totalPages;
  final bool last;

  const PageResult({
    required this.content,
    required this.number,
    required this.totalPages,
    required this.last,
  });

  factory PageResult.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromItem,
  ) {
    final page = json['page'] as Map<String, dynamic>? ?? const {};
    return PageResult(
      content: (json['content'] as List<dynamic>? ?? const [])
          .map((e) => fromItem(e as Map<String, dynamic>))
          .toList(),
      number: (page['number'] as num?)?.toInt() ?? 0,
      totalPages: (page['totalPages'] as num?)?.toInt() ?? 1,
      last: page['last'] as bool? ?? true,
    );
  }
}
