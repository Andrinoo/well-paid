import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../application/family_providers.dart';
import '../domain/family_models.dart';

/// Família §6 — criar, convidar (QR + link), entrar com token, gerir membros.
class FamilyPage extends ConsumerStatefulWidget {
  const FamilyPage({super.key, this.initialInviteToken});

  final String? initialInviteToken;

  @override
  ConsumerState<FamilyPage> createState() => _FamilyPageState();
}

class _FamilyPageState extends ConsumerState<FamilyPage> {
  final _createNameCtrl = TextEditingController();
  final _joinTokenCtrl = TextEditingController();
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    final t = widget.initialInviteToken;
    if (t != null && t.isNotEmpty) {
      _joinTokenCtrl.text = t;
    }
  }

  @override
  void dispose() {
    _createNameCtrl.dispose();
    _joinTokenCtrl.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function() fn) async {
    setState(() => _busy = true);
    try {
      await fn();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _createFamily() async {
    await _run(() async {
      final repo = ref.read(familyRepositoryProvider);
      final name = _createNameCtrl.text.trim();
      await repo.createFamily(name: name.isEmpty ? null : name);
      if (mounted) ref.invalidate(familyMeProvider);
    });
  }

  Future<void> _joinFamily() async {
    await _run(() async {
      final repo = ref.read(familyRepositoryProvider);
      await repo.join(_joinTokenCtrl.text);
      if (mounted) {
        ref.invalidate(familyMeProvider);
        _joinTokenCtrl.clear();
      }
    });
  }

  Future<void> _leave() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Sair da família'),
        content: const Text(
          'Se fores o único membro, a família será eliminada. Se fores titular, a titularidade passa para outro membro.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Sair')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    await _run(() async {
      await ref.read(familyRepositoryProvider).leave();
      if (mounted) ref.invalidate(familyMeProvider);
    });
  }

  Future<void> _removeMember(FamilyMemberItem m) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Remover membro'),
        content: Text('Remover ${m.email}?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Remover')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    await _run(() async {
      await ref.read(familyRepositoryProvider).removeMember(m.userId);
      if (mounted) ref.invalidate(familyMeProvider);
    });
  }

  Future<void> _renameFamily(String current) async {
    final ctrl = TextEditingController(text: current);
    final name = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nome da família'),
        content: TextField(
          controller: ctrl,
          decoration: const InputDecoration(labelText: 'Nome'),
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, ctrl.text.trim()),
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    if (name == null || name.isEmpty || !mounted) return;
    await _run(() async {
      await ref.read(familyRepositoryProvider).updateName(name);
      if (mounted) ref.invalidate(familyMeProvider);
    });
  }

  Future<void> _showInviteDialog() async {
    FamilyInviteCreated? created;
    await _run(() async {
      created = await ref.read(familyRepositoryProvider).createInvite();
    });
    if (!mounted || created == null) return;
    final c = created!;
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Convite'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Válido até ${c.expiresAt.toLocal().toString().split('.').first}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: QrImageView(
                  data: c.inviteUrl,
                  version: QrVersions.auto,
                  size: 200,
                  backgroundColor: Colors.white,
                ),
              ),
              const SizedBox(height: 12),
              SelectableText(
                c.token,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: c.token));
              if (ctx.mounted) {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  const SnackBar(content: Text('Token copiado')),
                );
              }
            },
            child: const Text('Copiar token'),
          ),
          FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('Fechar')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(familyMeProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: const Text('Família'),
        actions: [
          IconButton(
            tooltip: 'Atualizar',
            onPressed: _busy ? null : () => ref.invalidate(familyMeProvider),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Stack(
        children: [
          async.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  messageFromDio(e) ?? 'Erro ao carregar.',
                  textAlign: TextAlign.center,
                ),
              ),
            ),
            data: (me) {
              final fam = me.family;
              if (fam == null) {
                return ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    Text(
                      'Cria uma família ou entra com um convite (token ou QR).',
                      style: TextStyle(color: WellPaidColors.navy.withValues(alpha: 0.85)),
                    ),
                    const SizedBox(height: 20),
                    TextField(
                      controller: _createNameCtrl,
                      decoration: const InputDecoration(
                        labelText: 'Nome da família (opcional)',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: _busy ? null : _createFamily,
                      child: const Text('Criar família'),
                    ),
                    const SizedBox(height: 32),
                    const Divider(),
                    const SizedBox(height: 16),
                    Text(
                      'Entrar com convite',
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                            color: WellPaidColors.navy,
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _joinTokenCtrl,
                      decoration: const InputDecoration(
                        labelText: 'Cole o token do convite',
                        border: OutlineInputBorder(),
                      ),
                      maxLines: 3,
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: _busy ? null : _joinFamily,
                      style: FilledButton.styleFrom(
                        backgroundColor: WellPaidColors.navy,
                        foregroundColor: WellPaidColors.creamMuted,
                      ),
                      child: const Text('Entrar na família'),
                    ),
                  ],
                );
              }

              FamilyMemberItem? self;
              for (final m in fam.members) {
                if (m.isSelf) {
                  self = m;
                  break;
                }
              }
              final isOwner = self?.isOwner ?? false;

              return ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          fam.name,
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                color: WellPaidColors.navy,
                                fontWeight: FontWeight.w800,
                              ),
                        ),
                      ),
                      if (isOwner)
                        IconButton(
                          tooltip: 'Editar nome',
                          onPressed: _busy ? null : () => _renameFamily(fam.name),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                    ],
                  ),
                  Text(
                    '${fam.members.length} / 5 membros',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.65),
                        ),
                  ),
                  const SizedBox(height: 16),
                  if (isOwner)
                    FilledButton.icon(
                      onPressed: _busy ? null : _showInviteDialog,
                      icon: const Icon(Icons.qr_code_2_outlined),
                      label: const Text('Gerar convite (QR)'),
                      style: FilledButton.styleFrom(
                        backgroundColor: WellPaidColors.gold,
                        foregroundColor: WellPaidColors.navy,
                      ),
                    ),
                  if (isOwner) const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _busy ? null : _leave,
                    icon: const Icon(Icons.logout),
                    label: const Text('Sair da família'),
                  ),
                  const SizedBox(height: 24),
                  Text(
                    'Membros',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: WellPaidColors.navy,
                          fontWeight: FontWeight.w700,
                        ),
                  ),
                  const SizedBox(height: 8),
                  ...fam.members.map((m) {
                    return Card(
                      elevation: 0,
                      color: WellPaidColors.creamMuted.withValues(alpha: 0.9),
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        title: Text(
                          m.fullName?.isNotEmpty == true ? m.fullName! : m.email,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        subtitle: Text(
                          '${m.email} · ${m.isOwner ? 'Titular' : 'Membro'}${m.isSelf ? ' (tu)' : ''}',
                        ),
                        trailing: isOwner && !m.isSelf
                            ? IconButton(
                                icon: const Icon(Icons.person_remove_outlined),
                                onPressed: _busy ? null : () => _removeMember(m),
                              )
                            : null,
                      ),
                    );
                  }),
                ],
              );
            },
          ),
          if (_busy)
            const ModalBarrier(dismissible: false, color: Color(0x33000000)),
          if (_busy) const Center(child: CircularProgressIndicator()),
        ],
      ),
    );
  }
}
