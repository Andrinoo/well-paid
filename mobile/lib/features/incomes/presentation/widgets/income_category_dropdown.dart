import 'package:flutter/material.dart';

import '../../domain/income_category_option.dart';

class IncomeCategoryDropdown extends StatelessWidget {
  const IncomeCategoryDropdown({
    super.key,
    required this.categories,
    required this.value,
    required this.onChanged,
  });

  final List<IncomeCategoryOption> categories;
  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<String>(
      decoration: const InputDecoration(
        labelText: 'Tipo de provento',
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
      validator: (v) => v == null ? 'Obrigatório' : null,
    );
  }
}
