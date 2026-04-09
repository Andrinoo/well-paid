import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/l10n/context_l10n.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/theme/well_paid_colors.dart';
import '../../../l10n/app_localizations.dart';
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
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        return AlertDialog(
          title: Text(l10n.famLeaveTitle),
          content: Text(l10n.famLeaveBody),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.famExitAction),
            ),
          ],
        );
      },
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
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        return AlertDialog(
          title: Text(l10n.famRemoveMemberTitle),
          content: Text(l10n.famRemoveMemberConfirm(m.email)),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.delete),
            ),
          ],
        );
      },
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
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        return AlertDialog(
          title: Text(l10n.famRenameTitle),
          content: TextField(
            controller: ctrl,
            decoration: InputDecoration(labelText: l10n.famNameField),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, ctrl.text.trim()),
              child: Text(l10n.save),
            ),
          ],
        );
      },
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
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        return AlertDialog(
          title: Text(l10n.famInviteTitle),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  l10n.famInviteValidUntil(
                    c.expiresAt.toLocal().toString().split('.').first,
                  ),
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
                    SnackBar(content: Text(l10n.tokenCopied)),
                  );
                }
              },
              child: Text(l10n.famCopyTokenButton),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.close),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final async = ref.watch(familyMeProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.familyTitle),
        actions: [
          IconButton(
            tooltip: l10n.expensesRefresh,
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
                  messageFromDio(e, l10n) ?? l10n.familyLoadError,
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
                      l10n.famNoFamilyIntro,
                      style: TextStyle(color: WellPaidColors.navy.withValues(alpha: 0.85)),
                    ),
                    const SizedBox(height: 20),
                    TextField(
                      controller: _createNameCtrl,
                      decoration: InputDecoration(
                        labelText: l10n.famCreateNameOptional,
                        border: const OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: _busy ? null : _createFamily,
                      child: Text(l10n.famCreate),
                    ),
                    const SizedBox(height: 32),
                    const Divider(),
                    const SizedBox(height: 16),
                    Text(
                      l10n.famJoinSectionTitle,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                            color: WellPaidColors.navy,
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _joinTokenCtrl,
                      decoration: InputDecoration(
                        labelText: l10n.famJoinTokenField,
                        border: const OutlineInputBorder(),
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
                      child: Text(l10n.famJoin),
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
                          tooltip: l10n.famEditNameTooltip,
                          onPressed: _busy ? null : () => _renameFamily(fam.name),
                          icon: const Icon(Icons.edit_outlined),
                        ),
                    ],
                  ),
                  Text(
                    l10n.famMemberCount(fam.members.length, 5),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: WellPaidColors.navy.withValues(alpha: 0.65),
                        ),
                  ),
                  const SizedBox(height: 16),
                  if (isOwner)
                    FilledButton.icon(
                      onPressed: _busy ? null : _showInviteDialog,
                      icon: const Icon(Icons.qr_code_2_outlined),
                      label: Text(l10n.famInviteQr),
                      style: FilledButton.styleFrom(
                        backgroundColor: WellPaidColors.gold,
                        foregroundColor: WellPaidColors.navy,
                      ),
                    ),
                  if (isOwner) const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: _busy ? null : _leave,
                    icon: const Icon(Icons.logout),
                    label: Text(l10n.famLeave),
                  ),
                  const SizedBox(height: 24),
                  Text(
                    l10n.famMembersSection,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: WellPaidColors.navy,
                          fontWeight: FontWeight.w700,
                        ),
                  ),
                  const SizedBox(height: 8),
                  ...fam.members.map((m) {
                    final roleLabel =
                        m.isOwner ? l10n.famRoleOwner : l10n.famRoleMember;
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
                          '${m.email} · $roleLabel${m.isSelf ? l10n.famYouSuffix : ''}',
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
