/// Primeira letra em maiúscula; resto mantém o que o utilizador escreveu.
/// Para várias frases separadas por `. ` aplica o mesmo à primeira letra de cada parte.
String shoppingListFormatSentenceCase(String input) {
  final t = input.trim();
  if (t.isEmpty) return input;
  final parts = t.split(RegExp(r'(?<=[.!?])\s+'));
  final out = <String>[];
  for (final p in parts) {
    final s = p.trimLeft();
    if (s.isEmpty) {
      out.add(p);
      continue;
    }
    out.add(s[0].toUpperCase() + s.substring(1));
  }
  return out.join(' ');
}

/// Sugestões para autocomplete (pt); não são L10n — nomes de produto comuns.
const List<String> kShoppingListLabelSeeds = [
  'Arroz',
  'Feijão',
  'Açúcar',
  'Sal',
  'Óleo',
  'Macarrão',
  'Leite',
  'Ovos',
  'Pão',
  'Manteiga',
  'Queijo',
  'Iogurte',
  'Frango',
  'Carne moída',
  'Peixe',
  'Tomate',
  'Cebola',
  'Alho',
  'Batata',
  'Cenoura',
  'Alface',
  'Banana',
  'Maçã',
  'Laranja',
  'Café',
  'Sabão em pó',
  'Papel higiénico',
  'Água',
  'Refrigerante',
  'Suco',
  'Detergente',
  'Esponja',
];
