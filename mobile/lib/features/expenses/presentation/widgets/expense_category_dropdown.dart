import 'package:flutter/material.dart';

import '../../../../core/l10n/context_l10n.dart';
import '../../domain/category_option.dart';

class ExpenseCategoryDropdown extends StatelessWidget {
  const ExpenseCategoryDropdown({
    super.key,
    required this.categories,
    required this.value,
    required this.onChanged,
  });

  final List<CategoryOption> categories;
  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return DropdownButtonFormField<String>(
      decoration: InputDecoration(
        labelText: l10n.categoryLabel,
      ),
      // ignore: deprecated_member_use
      value: value,
      items: categories
          .map(
            (c) => DropdownMenuItem(
              value: c.id,
              child: Text(c.name),
            ),
          )
          .toList(),
      onChanged: onChanged,
      validator: (v) => v == null ? l10n.requiredField : null,
    );
  }
}
