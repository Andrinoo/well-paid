class CategoryOption {
  const CategoryOption({
    required this.id,
    required this.key,
    required this.name,
    required this.sortOrder,
  });

  final String id;
  final String key;
  final String name;
  final int sortOrder;

  factory CategoryOption.fromJson(Map<String, dynamic> json) {
    return CategoryOption(
      id: json['id'] as String,
      key: json['key'] as String,
      name: json['name'] as String,
      sortOrder: (json['sort_order'] as num).toInt(),
    );
  }
}
