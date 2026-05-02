import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../application/shopping_lists_providers.dart';

/// Rota dedicada [NavRoutes.shopping_list_new]: cria lista e abre detalhe.
class ShoppingListNewRoutePage extends ConsumerStatefulWidget {
  const ShoppingListNewRoutePage({super.key});

  @override
  ConsumerState<ShoppingListNewRoutePage> createState() =>
      _ShoppingListNewRoutePageState();
}

class _ShoppingListNewRoutePageState
    extends ConsumerState<ShoppingListNewRoutePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _run());
  }

  Future<void> _run() async {
    final l10n = context.l10n;
    try {
      final created =
          await ref.read(shoppingListsRepositoryProvider).createList();
      ref.invalidate(shoppingListsProvider);
      if (!mounted) return;
      context.go('/shopping-lists/${created.id}');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(messageFromDio(e, l10n) ?? '$e'),
        ),
      );
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 16),
            Text(l10n.pcShoppingListNewRouteTitle),
          ],
        ),
      ),
    );
  }
}
