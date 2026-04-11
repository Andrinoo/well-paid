// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Portuguese (`pt`).
class AppLocalizationsPt extends AppLocalizations {
  AppLocalizationsPt([String locale = 'pt']) : super(locale);

  @override
  String get appTitle => 'Well Paid';

  @override
  String get settingsTitle => 'Configurações';

  @override
  String get settingsLanguageTitle => 'Idioma da interface';

  @override
  String get settingsLanguageSubtitle =>
      'Textos do aplicativo em português (Brasil) ou inglês (EUA).';

  @override
  String get langPortugueseBrazil => 'Português (Brasil)';

  @override
  String get langEnglishUS => 'English (US)';

  @override
  String get settingsLanguageUpdated => 'Idioma atualizado.';

  @override
  String get save => 'Salvar';

  @override
  String get cancel => 'Cancelar';

  @override
  String get ok => 'OK';

  @override
  String get copy => 'Copiar';

  @override
  String get confirm => 'Confirmar';

  @override
  String get delete => 'Eliminar';

  @override
  String get close => 'Fechar';

  @override
  String get errorGeneric => 'Erro.';

  @override
  String get dioRequestTimeout =>
      'Tempo esgotado. Confirma que o backend está a correr e que o URL da API está certo.';

  @override
  String get dioConnectionFailed =>
      'Sem ligação ao servidor. No emulador Android use 10.0.2.2; no Windows ou iOS use 127.0.0.1 (--dart-define=API_BASE_URL=…).';

  @override
  String get dioNetworkFallback => 'Erro de rede';

  @override
  String get tryAgain => 'Tentar novamente';

  @override
  String get requiredField => 'Obrigatório';

  @override
  String get valueInvalid => 'Valor inválido.';

  @override
  String get homeDashboardTitle => 'Dashboard';

  @override
  String get tooltipSettings => 'Configurações';

  @override
  String get tooltipSecurity => 'Segurança';

  @override
  String get tooltipFamily => 'Família';

  @override
  String get tooltipRefreshDashboard => 'Atualizar dashboard';

  @override
  String get tooltipLogout => 'Sair';

  @override
  String get navHome => 'Início';

  @override
  String get navExpenses => 'Despesas';

  @override
  String get navIncomes => 'Proventos';

  @override
  String get navGoals => 'Metas';

  @override
  String get navReserve => 'Reserva';

  @override
  String get navQuickPanelToggleHint =>
      'Puxar ou tocar para atalhos A pagar e Listas de compras';

  @override
  String get menuMoreTooltip => 'Mais opções';

  @override
  String get logoutConfirmTitle => 'Terminar sessão?';

  @override
  String get logoutConfirmBody =>
      'Terá de voltar a iniciar sessão para aceder à conta.';

  @override
  String get homeQuickExpenses => 'Despesas';

  @override
  String get homeQuickIncomes => 'Proventos';

  @override
  String get homeQuickGoals => 'Metas';

  @override
  String get homeQuickReserve => 'Reserva';

  @override
  String get homeDashboardError => 'Erro ao carregar o dashboard.';

  @override
  String get authLoginTitle => 'Entrar na conta';

  @override
  String get authEmail => 'E-mail';

  @override
  String get authPassword => 'Senha';

  @override
  String get authShowPassword => 'Mostrar senha';

  @override
  String get authHidePassword => 'Ocultar senha';

  @override
  String get authPasswordRequired => 'Informe a senha';

  @override
  String get authForgotPassword => 'Esqueceu a senha?';

  @override
  String get authEnter => 'Entrar';

  @override
  String get authLoginError => 'Erro ao entrar';

  @override
  String get authNoAccountYet => 'Ainda sem conta?';

  @override
  String get authCreateAccount => 'Criar conta';

  @override
  String get authCopyright =>
      'Copyright © 2026 Andrino Cabral. All rights reserved.';

  @override
  String get authRegisterTitle => 'Criar conta';

  @override
  String get authRegisterSubtitle => 'Começa em poucos passos.';

  @override
  String get authRegisterError => 'Erro ao registar';

  @override
  String get authPasswordPolicyHint =>
      'Mín. 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial.';

  @override
  String get authPasswordRulesError => 'Senha não cumpre as regras';

  @override
  String get authNameOptional => 'Nome (opcional)';

  @override
  String get authPhoneOptional => 'Telemóvel (opcional)';

  @override
  String get authConfirmPassword => 'Confirmar senha';

  @override
  String get authConfirmPasswordRequired => 'Confirme a senha';

  @override
  String get authPasswordMismatch => 'As senhas não coincidem';

  @override
  String get authRegisterButton => 'Registar';

  @override
  String get authVerifyEmailTitle => 'Confirmar e-mail';

  @override
  String get authVerifyEmailSubtitle =>
      'Introduza o código de 6 dígitos que enviámos para o seu e-mail.';

  @override
  String get authVerifyEmailFromLink => 'A confirmar o seu e-mail…';

  @override
  String get authVerificationCodeLabel => 'Código';

  @override
  String get authVerificationCodeHint => '000000';

  @override
  String get authVerificationCodeError => 'Introduza os 6 dígitos';

  @override
  String get authVerifyEmailButton => 'Confirmar e entrar';

  @override
  String get authResendVerification => 'Reenviar código';

  @override
  String get authVerifyEmailSuccess => 'E-mail confirmado. Bem-vindo!';

  @override
  String get authVerifyEmailError => 'Não foi possível confirmar';

  @override
  String get authResendVerificationError => 'Não foi possível reenviar';

  @override
  String get authVerifyEmailAction => 'Confirmar e-mail';

  @override
  String authDevVerificationHint(String token, String code) {
    return 'Token (link):\n$token\n\nCódigo:\n$code';
  }

  @override
  String get authAlreadyHaveAccount => 'Já tenho conta — entrar';

  @override
  String get authEmailRequired => 'Informe o e-mail';

  @override
  String get authEmailInvalid => 'E-mail inválido';

  @override
  String get authForgotTitle => 'Recuperar senha';

  @override
  String get authForgotSubtitle =>
      'Indica o teu e-mail para receberes instruções.';

  @override
  String get authForgotBody =>
      'Enviaremos um código para redefinires a senha na app. Se não vês o e-mail, verifica o spam.';

  @override
  String get authForgotSend => 'Enviar instruções';

  @override
  String get authForgotError => 'Erro ao enviar pedido';

  @override
  String get authBackToLogin => 'Voltar ao login';

  @override
  String get authDevModeTitle => 'Modo desenvolvimento';

  @override
  String authDevTokenHint(String token) {
    return 'O backend devolveu o token para testes. Salve-o e use no ecrã seguinte.\n\n$token';
  }

  @override
  String get tokenCopied => 'Token copiado';

  @override
  String get dashMonthSummary => 'Resumo do mês';

  @override
  String get dashIncome => 'Receitas';

  @override
  String get dashExpenses => 'Despesas';

  @override
  String get dashBalance => 'Saldo';

  @override
  String get dashByCategory => 'Despesas por categoria';

  @override
  String get dashToPay => 'A pagar';

  @override
  String get toPayScreenSubtitle =>
      'Todas as despesas pendentes por ordem cronológica de vencimento. Parcelas aparecem em linhas separadas. As cores indicam a proximidade do vencimento.';

  @override
  String get toPayViewAllExpenses => 'Todas as despesas';

  @override
  String toPayDueOn(String date) {
    return 'Vence $date';
  }

  @override
  String toPayCompetenceOn(String date) {
    return 'Competência $date (sem data de vencimento)';
  }

  @override
  String get toPayProjectedHint =>
      'Prevista — a linha efetiva surge alguns dias antes do vencimento.';

  @override
  String get toPayOfflineCacheBanner =>
      'A mostrar dados guardados — a lista pode estar incompleta até voltar a ter rede.';

  @override
  String toPayPendingTotal(String amount) {
    return 'Total pendente: $amount';
  }

  @override
  String get toPayFilterAll => 'Todas';

  @override
  String get toPayFilterOverdue => 'Atrasadas';

  @override
  String get toPayFilterThisWeek => 'Esta semana';

  @override
  String get toPaySectionOverdue => 'Atrasadas';

  @override
  String get toPaySectionThisWeek => 'Vencem esta semana';

  @override
  String get toPaySectionLater => 'Mais tarde';

  @override
  String get toPayFilterEmpty => 'Nada corresponde a este filtro.';

  @override
  String get toPayLegendTitle => 'Cores do vencimento';

  @override
  String get toPayLegendOverdue => 'Já passou a data de vencimento';

  @override
  String get toPayLegendDueToday => 'Vence hoje';

  @override
  String get toPayLegendDueSoon => 'Vence em 1–3 dias';

  @override
  String get toPayLegendUpcoming => 'Vence em 4–10 dias';

  @override
  String get toPayLegendSafe => 'Vence daqui a 11 dias ou mais';

  @override
  String get toPayQuickPaySwitchTooltip =>
      'Ligue para confirmar o pagamento; a lista atualiza após sucesso. Se falhar, o interruptor volta a desligar.';

  @override
  String expenseListNextInstallmentLine(String date) {
    return 'Próx.: $date';
  }

  @override
  String get expenseTagPar => 'PAR';

  @override
  String get expenseTagParA11y => 'Despesa parcelada';

  @override
  String get expenseTagRec => 'REC';

  @override
  String get expenseTagRecA11y => 'Despesa recorrente';

  @override
  String get expenseNextDueSectionTitle => 'Próxima ocorrência';

  @override
  String expenseNextDueCompetenceLine(String date) {
    return 'Competência: $date';
  }

  @override
  String expenseNextDueDateLine(String date) {
    return 'Vencimento: $date';
  }

  @override
  String get expenseInstallmentNextSectionTitle => 'Próxima parcela';

  @override
  String get expenseInstallmentLastSectionTitle => 'Última parcela';

  @override
  String get expensePayEarlyTitle => 'Pagar antecipadamente?';

  @override
  String get expensePayEarlyBody =>
      'Esta despesa é de um mês posterior ao atual. Tem a certeza de que quer marcá-la como paga agora?';

  @override
  String get dashNothingPending => 'Nada pendente.';

  @override
  String get dashPendingTotal => 'Total pendente';

  @override
  String get dashSeeAll => 'Ver todas';

  @override
  String get dashUpcomingDue => 'Próximos vencimentos';

  @override
  String get dashNoUpcomingInNextMonth =>
      'Sem contas com vencimento no próximo mês.';

  @override
  String get dashSeeMore => 'Ver mais';

  @override
  String get dashGoals => 'Metas';

  @override
  String get dashNoActiveGoals => 'Sem metas ativas no momento.';

  @override
  String get dashSeeGoals => 'Ver metas';

  @override
  String get dashEmergencyReserve => 'Reserva de emergência';

  @override
  String get dashEmergencyReserveBalance => 'Saldo acumulado';

  @override
  String get dashEmergencyReserveMonthly => 'Meta mensal';

  @override
  String get dashEmergencyReserveTimesTarget => 'x da meta';

  @override
  String dashEmergencyReserveAnnualProgress(int percent) {
    return '$percent% do objetivo anual';
  }

  @override
  String get dashEmergencyReserveMomentum => 'Siga assim';

  @override
  String get dashEmergencyReserveAnnualDone => 'Objetivo anual atingido';

  @override
  String get dashEmergencyReserveStageFirst => 'Primeiro passo';

  @override
  String get dashEmergencyReserveStageStart => 'Bom ritmo';

  @override
  String get dashEmergencyReserveStageMid => 'Metade do caminho';

  @override
  String get dashEmergencyReserveStageStrong => 'Reta final';

  @override
  String get dashEmergencyReserveStageDone => 'Objetivo anual atingido';

  @override
  String get dashEmergencyReserveFootnote =>
      'Cada mês civil acrescenta a meta ao saldo (registo interno; não substitui contas bancárias).';

  @override
  String get dashEmergencyReserveConfigure => 'Configurar';

  @override
  String get emergencyReserveTitle => 'Reserva de emergência';

  @override
  String get emergencyReserveIntro =>
      'Define quanto queres poupar por mês. O saldo aumenta automaticamente em cada mês civil, a partir do mês em que activares a meta.';

  @override
  String get emergencyReserveMonthlyLabel => 'Valor a poupar por mês (R\$)';

  @override
  String get emergencyReserveQuickPickTitle => 'Atalhos rápidos';

  @override
  String get emergencyReserveSave => 'Salvar meta';

  @override
  String get emergencyReserveSavedSnackbar => 'Meta de reserva actualizada.';

  @override
  String get emergencyReserveError => 'Não foi possível guardar.';

  @override
  String get emergencyReserveAccrualListTitle => 'Adesões mensais';

  @override
  String get emergencyReserveAccrualListEmpty =>
      'Ainda não existem adesões mensais.';

  @override
  String get emergencyReserveAccrualListCredit => 'Crédito aplicado';

  @override
  String get emergencyReserveAccrualEdit => 'Editar crédito';

  @override
  String get emergencyReserveAccrualDelete => 'Remover crédito';

  @override
  String emergencyReserveAccrualDeleteTitle(String month) {
    return 'Remover crédito de $month?';
  }

  @override
  String get emergencyReserveAccrualDeleteBody =>
      'O saldo será ajustado. Este mês deixa de receber crédito automático (podes voltar a acrescentar manualmente).';

  @override
  String get emergencyReserveAccrualDeleteConfirm => 'Remover';

  @override
  String emergencyReserveAccrualEditTitle(String month) {
    return 'Crédito de $month';
  }

  @override
  String get emergencyReserveAccrualUpdatedSnackbar => 'Crédito actualizado.';

  @override
  String get emergencyReserveAccrualRemovedSnackbar => 'Crédito removido.';

  @override
  String get emergencyReserveResetAction => 'Limpar reserva e histórico';

  @override
  String get emergencyReserveResetTitle => 'Limpar tudo?';

  @override
  String get emergencyReserveResetBody =>
      'Remove a meta, o saldo e todos os créditos mensais. Esta acção não se pode desfazer.';

  @override
  String get emergencyReserveResetConfirm => 'Limpar';

  @override
  String get emergencyReserveResetSuccess => 'Reserva reposta.';

  @override
  String get settingsEmergencyReserve => 'Reserva de emergência';

  @override
  String get dashMarkPaidTooltip => 'Marcar como paga';

  @override
  String get dashDueShort => 'Venc.';

  @override
  String get dashDueVerb => 'Vence';

  @override
  String get dashFamilySuffix => ' · Família';

  @override
  String get dashGoalFamilySuffix => ' (família)';

  @override
  String dashPendingItemA11y(String description, String amount, String due) {
    return '$description, $amount, vencimento $due';
  }

  @override
  String periodSummaryA11y(String label) {
    return 'Período do resumo, $label';
  }

  @override
  String get periodPrevMonth => 'Mês anterior';

  @override
  String get periodNextMonth => 'Próximo mês';

  @override
  String get chartTotalExpenses => 'Total despesas';

  @override
  String get chartNoExpensesThisMonth => 'Sem despesas\nneste mês.';

  @override
  String get chartNoExpensesRegistered => 'Sem despesas registadas\nneste mês.';

  @override
  String chartSemanticsWithData(String total) {
    return 'Gráfico de despesas por categoria, total $total';
  }

  @override
  String get chartSemanticsNoData =>
      'Gráfico de despesas por categoria, sem dados no mês';

  @override
  String get chartCategoriesHint =>
      'As categorias aparecem quando existirem despesas neste mês.';

  @override
  String get chartDonutTapHint => 'Toque numa fatia para ver a categoria.';

  @override
  String get chartCategoryOther => 'Outros';

  @override
  String get goalsTitle => 'Metas';

  @override
  String get goalsRefresh => 'Atualizar';

  @override
  String get goalsLoadError => 'Erro ao carregar metas.';

  @override
  String get goalsEmpty => 'Sem metas ativas ainda.';

  @override
  String get goalsAddTooltip => 'Nova meta';

  @override
  String get newGoalTitle => 'Nova meta';

  @override
  String get goalFormTitleLabel => 'Nome da meta';

  @override
  String get goalFormTargetLabel => 'Objetivo (valor a atingir)';

  @override
  String get goalFormIntro =>
      'Define um valor-alvo em reais. Podes registar o progresso mais tarde.';

  @override
  String get goalFormSave => 'Criar meta';

  @override
  String get goalFormCreatedSnackbar => 'Meta criada.';

  @override
  String get goalSaveError => 'Não foi possível guardar a meta.';

  @override
  String get goalFormInitialLabel => 'Já tenho (opcional)';

  @override
  String get goalFormInitialHint =>
      'Quanto já poupaste para esta meta ao criá-la.';

  @override
  String get goalDetailTitle => 'Meta';

  @override
  String get goalRemaining => 'Falta';

  @override
  String get goalCompleted => 'Meta atingida';

  @override
  String get goalContribute => 'Adicionar valor';

  @override
  String get goalContributeTitle => 'Contribuição';

  @override
  String get goalContributeAmountLabel => 'Valor (R\$)';

  @override
  String get goalContributeNoteLabel => 'Nota (opcional)';

  @override
  String get goalContributeSaved => 'Contribuição registada.';

  @override
  String get goalContributeError => 'Não foi possível registar.';

  @override
  String get goalContributionsTitle => 'Histórico';

  @override
  String get goalContributionsEmpty => 'Ainda não há contribuições registadas.';

  @override
  String get goalContributionsOwnerOnly =>
      'O histórico de contribuições só é visível para quem criou a meta.';

  @override
  String get goalArchive => 'Arquivar';

  @override
  String get goalArchiveTitle => 'Arquivar meta?';

  @override
  String get goalArchiveBody =>
      'A meta deixa de contar como ativa. Podes reativá-la editando o estado.';

  @override
  String get goalArchivedSnackbar => 'Meta arquivada.';

  @override
  String get goalDelete => 'Eliminar';

  @override
  String get goalDeleteTitle => 'Eliminar meta?';

  @override
  String get goalDeleteBody =>
      'Só é possível eliminar quando o saldo da meta é zero.';

  @override
  String get goalDeletedSnackbar => 'Meta eliminada.';

  @override
  String get goalInactiveBadge => 'Arquivada';

  @override
  String get goalReactivate => 'Reativar';

  @override
  String get goalReactivatedSnackbar => 'Meta reativada.';

  @override
  String get expensePayConfirmTitle => 'Marcar como paga?';

  @override
  String get expensePayConflict =>
      'Esta despesa já não pode ser quitada (por exemplo, já está paga).';

  @override
  String expensePayInstallmentLine(int current, int total) {
    return 'Parcela $current de $total';
  }

  @override
  String get expensesTitle => 'Despesas';

  @override
  String get expensesRefresh => 'Atualizar';

  @override
  String get expensesNew => 'Nova';

  @override
  String get expensesNewLong => 'Nova despesa';

  @override
  String get expensesRefreshList => 'Atualizar lista';

  @override
  String get expensesFilterAll => 'Todas';

  @override
  String get expensesFilterPending => 'Pendentes';

  @override
  String get expensesFilterPaid => 'Pagas';

  @override
  String get expensesLoadError => 'Erro ao carregar.';

  @override
  String get expensesEmpty => 'Nenhuma despesa neste filtro.';

  @override
  String get expensePay => 'Pagar';

  @override
  String get expenseMarkedPaid => 'Despesa marcada como paga.';

  @override
  String get expensePayError => 'Erro ao pagar.';

  @override
  String expenseTileFamilyCategory(String category) {
    return '$category · Família';
  }

  @override
  String expenseTileDateLine(String date, String status) {
    return 'Data $date · $status';
  }

  @override
  String get expenseStatusPending => 'Pendente';

  @override
  String get expenseStatusPaid => 'Paga';

  @override
  String expenseInstallmentChip(int n, int total) {
    return 'Parcela $n/$total';
  }

  @override
  String get expenseRecurringMonthly => 'Recorrente · mensal';

  @override
  String get expenseRecurringWeekly => 'Recorrente · semanal';

  @override
  String get expenseRecurringYearly => 'Recorrente · anual';

  @override
  String expenseSharedWith(String name) {
    return 'Partilhada · $name';
  }

  @override
  String get expenseShared => 'Partilhada';

  @override
  String get expenseTitle => 'Despesa';

  @override
  String get expenseLoadError => 'Erro ao carregar.';

  @override
  String get expenseCompetence => 'Data (competência)';

  @override
  String get expenseDue => 'Vencimento';

  @override
  String get expenseStatusLabel => 'Estado';

  @override
  String get expenseInstallmentsRow => 'Parcelas';

  @override
  String get expenseRecurrence => 'Recorrência';

  @override
  String get expenseShare => 'Partilha';

  @override
  String expenseShareWith(String name) {
    return 'Com $name';
  }

  @override
  String get expenseShareFamily => 'Família';

  @override
  String get expenseMarkPaid => 'Marcar como paga';

  @override
  String get expenseEdit => 'Editar';

  @override
  String get expenseDelete => 'Eliminar';

  @override
  String get expenseReadOnlyBanner =>
      'Despesa de outro membro da família — só podes ver.';

  @override
  String get expenseDeleteError => 'Erro ao eliminar.';

  @override
  String get expDelInstallmentTitle => 'Eliminar plano parcelado';

  @override
  String get expDelInstallmentPaidBody =>
      'Este plano tem parcelas já pagas. Queres apagar também o histórico dessas parcelas, ou só as parcelas futuras pendentes?';

  @override
  String get expDelInstallmentMaybePaidBody =>
      'Se já existirem parcelas pagas, podes manter o histórico e apagar só as futuras pendentes, ou eliminar o plano inteiro.';

  @override
  String get expDelFutureOnly => 'Só futuras pendentes';

  @override
  String get expDelAllIncludingPaid => 'Tudo (inclui pagas)';

  @override
  String get expDelInstallmentSimpleTitle => 'Eliminar plano parcelado?';

  @override
  String expDelInstallmentSimpleBody(int total) {
    return 'Todas as $total parcelas serão eliminadas.';
  }

  @override
  String get expDelRecurringTitle => 'Eliminar recorrência';

  @override
  String get expDelRecurringBody =>
      'Podes cancelar só as ocorrências futuras pendentes (após hoje), ou encerrar a série: remove todas as pendentes e desliga as já pagas do histórico da recorrência (mantêm-se como despesas normais).';

  @override
  String get expDelCloseSeries => 'Encerrar série';

  @override
  String get expDelRecurringOccurrenceTitle => 'Eliminar despesa recorrente';

  @override
  String get expDelRecurringOccurrenceBody =>
      'Eliminar só esta competência ou aplicar à série inteira?';

  @override
  String get expDelThisOnly => 'Só esta';

  @override
  String get expDelWholeSeries => 'Toda a série';

  @override
  String get expDelRemoveFromRecurrenceTitle => 'Remover da recorrência';

  @override
  String get expDelRemoveOccurrenceTitle => 'Eliminar ocorrência?';

  @override
  String get expDelPaidUnlinkBody =>
      'Esta despesa já foi paga: não será apagada do histórico, apenas deixa de fazer parte da série recorrente.';

  @override
  String get expDelPendingDeleteBody =>
      'Esta linha pendente será eliminada; as restantes da série mantêm-se.';

  @override
  String get expDelSeriesScopeTitle => 'Âmbito da série';

  @override
  String get expDelSeriesScopeBody =>
      'Cancelar só ocorrências futuras pendentes (após hoje), ou encerrar toda a série (pendentes + desligar pagas do histórico da recorrência)?';

  @override
  String get expDelSingleTitle => 'Eliminar despesa?';

  @override
  String get expDelSingleBody => 'Esta ação não pode ser anulada.';

  @override
  String get expDelSuccessInstallmentFuture => 'Parcelas futuras canceladas.';

  @override
  String get expDelSuccessInstallmentAll => 'Plano eliminado.';

  @override
  String get expDelSuccessOccurrence => 'Ocorrência eliminada.';

  @override
  String get expDelSuccessOccurrenceUnlink => 'Removida da recorrência.';

  @override
  String get expDelSuccessRecurringFuture => 'Futuras pendentes canceladas.';

  @override
  String get expDelSuccessRecurringClose => 'Recorrência encerrada.';

  @override
  String get expDelSuccessSingle => 'Despesa eliminada.';

  @override
  String get newExpenseTitle => 'Nova despesa';

  @override
  String get editExpenseTitle => 'Editar despesa';

  @override
  String get expenseSaveError => 'Erro ao salvar.';

  @override
  String get expenseChangesSaved => 'Alterações salvas.';

  @override
  String get expenseEditOwnerOnly =>
      'Só o titular desta despesa a pode editar.';

  @override
  String get incomesTitle => 'Proventos';

  @override
  String get newIncomeTitle => 'Novo provento';

  @override
  String get editIncomeTitle => 'Editar provento';

  @override
  String get incomeEditOwnerOnly =>
      'Só o titular deste provento o pode editar.';

  @override
  String get incomeSaveError => 'Erro ao salvar.';

  @override
  String get incomeSaved => 'Salvo.';

  @override
  String get incomeDetailTitle => 'Provento';

  @override
  String get incomesAddLong => 'Adicionar provento';

  @override
  String get incomesRefresh => 'Atualizar';

  @override
  String get incomesRefreshList => 'Atualizar lista';

  @override
  String get incomesLoadError => 'Erro ao carregar.';

  @override
  String get incomesEmpty => 'Nenhum provento neste mês.';

  @override
  String get incomesListHint =>
      'Valores em reais com centavos exactos; competência pelo dia do provento.';

  @override
  String incomeTileDateLine(String date) {
    return 'Data $date';
  }

  @override
  String get familyTitle => 'Família';

  @override
  String get securityTitle => 'Segurança';

  @override
  String get unlockTitle => 'Desbloquear';

  @override
  String get resetPasswordTitle => 'Redefinir senha';

  @override
  String get categoryLabel => 'Categoria';

  @override
  String get noneDash => '—';

  @override
  String get secAppTitle => 'Segurança da app';

  @override
  String get secSetPinTitle => 'Definir PIN da app';

  @override
  String get secNewPinTitle => 'Novo PIN';

  @override
  String get secPinField => 'PIN (4–6 dígitos)';

  @override
  String get secRepeatPinField => 'Repetir PIN';

  @override
  String get secPinInvalidOrMismatch => 'PINs inválidos ou não coincidem.';

  @override
  String get secPinSavedSnackbar => 'PIN da app salvo.';

  @override
  String get secDisablePinTitle => 'Desativar PIN';

  @override
  String get secCurrentPinField => 'PIN actual';

  @override
  String get secWrongPin => 'PIN incorreto.';

  @override
  String get secPinDisabledSnackbar => 'Bloqueio por PIN desativado.';

  @override
  String get secLockWithPin => 'Bloquear com PIN';

  @override
  String get secLockPinOnSub => 'Ao minimizar, a app pede o PIN ao voltar.';

  @override
  String get secLockPinOffSub =>
      'Desligado — só a sessão online (login) protege.';

  @override
  String get secBiometricTitle => 'Oferecer biometria no desbloqueio';

  @override
  String get secBiometricOnSub =>
      'Impressão digital ou rosto, se o telemóvel permitir.';

  @override
  String get secBiometricOffSub =>
      'Este dispositivo não expõe biometria à app.';

  @override
  String get secChangePin => 'Alterar PIN';

  @override
  String get unlockIntro => 'Introduz o PIN da app para continuar.';

  @override
  String get unlockPinLabel => 'PIN';

  @override
  String get unlockUseBiometric => 'Usar biometria';

  @override
  String get authResetSubtitle => 'Cola o código que recebeste por e-mail.';

  @override
  String get authResetTokenLabel => 'Código de recuperação';

  @override
  String get authResetTokenHint => 'Cole o token completo';

  @override
  String get authResetTokenError => 'Cole o código recebido por e-mail';

  @override
  String get authNewPassword => 'Nova senha';

  @override
  String get authConfirmNewPassword => 'Confirmar nova senha';

  @override
  String get authSaveNewPassword => 'Salvar nova senha';

  @override
  String get authResetSuccess => 'Senha atualizada. Pode entrar.';

  @override
  String get authResetError => 'Erro ao redefinir';

  @override
  String get authRequestNewCode => 'Pedir novo código';

  @override
  String get shareFamilyTitle => 'Partilhar na família';

  @override
  String get shareFamilySubOn =>
      'Visível para o agregado; podes indicar com quem dividir.';

  @override
  String get shareFamilySubOff =>
      'Junta-te a uma família (convite) para usar partilha.';

  @override
  String get shareSplitWith => 'Dividir com';

  @override
  String get shareWholeFamily => 'Toda a família';

  @override
  String get expFormDescription => 'Descrição';

  @override
  String get expFormAmountInstallment => 'Valor da parcela (R\$)';

  @override
  String get expFormAmount => 'Valor (R\$)';

  @override
  String get expFormKindSingle => 'Única';

  @override
  String get expFormKindInstallments => 'Parcelas';

  @override
  String get expFormKindRecurring => 'Recorrente';

  @override
  String get expFormInstallmentsLabel => 'Parcelas (competência mensal)';

  @override
  String get expFormInstallmentsHint => '1 a 24';

  @override
  String get expFormInstallmentRangeError => 'Use de 1 a 24';

  @override
  String get expFormInstallmentInvalid => 'Número inválido';

  @override
  String get expFormRecurringFrequency => 'Frequência';

  @override
  String get expFormRecurringChoose => 'Escolher…';

  @override
  String get expFormRecurringHelp =>
      'Despesas fixas (água, luz, internet, assinaturas): o sistema gera linhas pendentes por mês ao consultares a lista. Alterar o valor na linha principal actualiza só ocorrências futuras pendentes; o histórico mantém os valores antigos.';

  @override
  String get expFormMarkPaid => 'Já está paga';

  @override
  String get expFormHasDueDate => 'Tem data de vencimento';

  @override
  String get expFormHasDueDateSub => 'Contas a pagar / alertas no dashboard';

  @override
  String get expFormExpenseDate => 'Data da despesa (competência)';

  @override
  String get expFormDueDate => 'Data de vencimento';

  @override
  String get expFormChooseDate => 'Escolher…';

  @override
  String get expFormCategoriesLoadError => 'Erro ao carregar categorias.';

  @override
  String get expFormPickCategory => 'Escolhe uma categoria.';

  @override
  String get expFormCreateError => 'Erro ao criar.';

  @override
  String expFormPlanCreated(int count) {
    return 'Plano: $count parcelas criadas.';
  }

  @override
  String expFormPlanCreatedRef(int count, String ref) {
    return 'Plano: $count parcelas (ref. $ref).';
  }

  @override
  String get expFormCreated => 'Despesa criada.';

  @override
  String get expFormPickDue => 'Escolhe a data de vencimento.';

  @override
  String get expFormRecurringFreqRequired =>
      'Escolhe a frequência da recorrência.';

  @override
  String get expEditDescription => 'Descrição';

  @override
  String get expEditAmount => 'Valor (R\$)';

  @override
  String get expEditExpenseDate => 'Data da despesa';

  @override
  String get expEditDueOptional => 'Vencimento (opcional)';

  @override
  String get expEditState => 'Estado';

  @override
  String get expEditRecurrenceMeta => 'Recorrência (metadado)';

  @override
  String get expEditRecurrenceNone => 'Nenhuma';

  @override
  String expEditInstallmentBanner(int n, int total) {
    return 'Parcela $n de $total. Alterações aplicam-se só a esta linha.';
  }

  @override
  String get expEditRecurringAnchorBanner =>
      'Recorrência: ao alterar o valor, só as ocorrências geradas futuras e pendentes (data após hoje) actualizam; meses já passados mantêm o valor histórico.';

  @override
  String get incFormDescription => 'Descrição';

  @override
  String get incFormAmount => 'Valor (R\$)';

  @override
  String get incFormIncomeDate => 'Data do provento';

  @override
  String get incFormNotes => 'Notas (opcional)';

  @override
  String get incDetailLoadError => 'Erro.';

  @override
  String get familyLoadError => 'Erro.';

  @override
  String get familySaveError => 'Erro ao salvar.';

  @override
  String get saveChanges => 'Salvar alterações';

  @override
  String get expFormAmountHint => 'ex. 12,50';

  @override
  String expFormInstallmentNeedAmountLine(int count) {
    return 'Informe o valor de cada parcela. Total = parcela × $count.';
  }

  @override
  String expFormInstallmentPlanTotalLine(
    String total,
    int count,
    String installment,
  ) {
    return 'Total do plano: $total ($count × $installment).';
  }

  @override
  String get expFormFreqMonthly => 'Mensal';

  @override
  String get expFormFreqWeekly => 'Semanal';

  @override
  String get expFormFreqYearly => 'Anual';

  @override
  String get incFormIntro =>
      'Regista entradas reais (salário, extras, etc.). O saldo do mês no dashboard usa a soma dos proventos deste período.';

  @override
  String get incFormDescHint => 'ex. Salário abril, Honorários cliente X';

  @override
  String get incFormAmountHint => 'ex. 3.500,00';

  @override
  String get incFormIncomeDateCompetence => 'Data do provento (competência)';

  @override
  String get incFormPickCategory => 'Escolhe o tipo de provento.';

  @override
  String get incFormCreatedSnackbar => 'Provento registado.';

  @override
  String get incFormSaveButton => 'Salvar provento';

  @override
  String get incFormCategoriesLoadError => 'Erro ao carregar tipos.';

  @override
  String get incFormNotesHint =>
      'Origem, referência, NIF… — só para o teu agregado';

  @override
  String get incFormCategoryLabel => 'Tipo de provento';

  @override
  String get incomeDeleteTitle => 'Eliminar provento?';

  @override
  String get incomeDeletedSnackbar => 'Eliminado.';

  @override
  String get incomeReadOnlyBanner =>
      'Provento de outro membro da família — só podes ver.';

  @override
  String get incomeDetailTypeLabel => 'Tipo';

  @override
  String get incomeDetailDateCompetenceLabel => 'Data (competência)';

  @override
  String get incomeDetailNotesLabel => 'Notas';

  @override
  String get incomeDeleteError => 'Erro ao eliminar.';

  @override
  String get unlockPinTooShort => 'PIN com pelo menos 4 dígitos';

  @override
  String get unlockBioReason => 'Desbloquear o Well Paid';

  @override
  String get unlockBioUnavailable => 'Biometria indisponível';

  @override
  String get famLeaveTitle => 'Sair da família';

  @override
  String get famLeaveBody =>
      'Se fores o único membro, a família será eliminada. Se fores titular, a titularidade passa para outro membro.';

  @override
  String get famExitAction => 'Sair';

  @override
  String get famRemoveMemberTitle => 'Remover membro';

  @override
  String famRemoveMemberConfirm(String email) {
    return 'Remover $email?';
  }

  @override
  String get famRenameTitle => 'Nome da família';

  @override
  String get famNameField => 'Nome';

  @override
  String get famInviteTitle => 'Convite';

  @override
  String famInviteValidUntil(String date) {
    return 'Válido até $date';
  }

  @override
  String get famCopyTokenButton => 'Copiar token';

  @override
  String get famNoFamilyIntro =>
      'Cria uma família ou entra com um convite (token ou QR).';

  @override
  String get famCreateNameOptional => 'Nome da família (opcional)';

  @override
  String get famJoinSectionTitle => 'Entrar com convite';

  @override
  String get famJoinTokenField => 'Cole o token do convite';

  @override
  String get famCreate => 'Criar família';

  @override
  String get famJoin => 'Entrar na família';

  @override
  String get famInviteQr => 'Gerar convite (QR)';

  @override
  String get famLeave => 'Sair da família';

  @override
  String get famMembersSection => 'Membros';

  @override
  String famMemberCount(int current, int max) {
    return '$current / $max membros';
  }

  @override
  String get famRoleOwner => 'Titular';

  @override
  String get famRoleMember => 'Membro';

  @override
  String get famYouSuffix => ' (tu)';

  @override
  String get famEditNameTooltip => 'Editar nome';

  @override
  String get dashCashflowTitle => 'Histórico mensal';

  @override
  String get dashCashflowChartOptions => 'Período e previsão';

  @override
  String get dashCashflowDynamicMode => 'Modo dinâmico';

  @override
  String get dashCashflowStartMonth => 'Início';

  @override
  String get dashCashflowEndMonth => 'Fim';

  @override
  String get dashCashflowForecastMonths => 'Meses de previsão';

  @override
  String get dashCashflowApply => 'Aplicar';

  @override
  String get dashCashflowLegendIncome => 'Proventos';

  @override
  String get dashCashflowLegendExpensePaid => 'Despesas pagas';

  @override
  String get dashCashflowLegendExpenseForecast => 'Despesas previstas';

  @override
  String get dashCashflowEmpty => 'Sem dados para este período.';

  @override
  String get dashCashflowError => 'Erro ao carregar o histórico mensal.';

  @override
  String dashCashflowFooterForecastTotal(String amount) {
    return 'Total previsto: $amount';
  }

  @override
  String dashCashflowFooterBalance(String amount) {
    return 'Saldo no período: $amount';
  }

  @override
  String get dashCashflowSemantics =>
      'Gráfico de histórico mensal de proventos e despesas';

  @override
  String dashCashflowLoadedPoints(int count) {
    return '$count meses no período';
  }

  @override
  String get dashCashflowRangeInvalid =>
      'O mês inicial não pode ser depois do mês final.';

  @override
  String get dashCashflowA11yPickStartMonth =>
      'Escolher mês inicial do intervalo';

  @override
  String get dashCashflowA11yPickEndMonth => 'Escolher mês final do intervalo';

  @override
  String get dashCashflowA11yForecastDropdown =>
      'Número de meses de previsão após o intervalo';

  @override
  String get dashCashflowA11yApply => 'Aplicar filtros ao histórico mensal';

  @override
  String dashCashflowA11ySeriesToggle(String name) {
    return '$name. Toque para mostrar ou ocultar no gráfico.';
  }

  @override
  String dashCashflowA11ySummary(String forecast, String balance) {
    return 'Totais do período. Previsto: $forecast. Saldo: $balance.';
  }

  @override
  String get shoppingListsTitle => 'Listas de compras';

  @override
  String get shoppingListsMenuLabel => 'Listas de compras';

  @override
  String get shoppingListsActiveSection => 'Em planeamento';

  @override
  String get shoppingListsHistorySection => 'Histórico';

  @override
  String get shoppingListsEmpty =>
      'Ainda não tens listas. Cria uma para planear itens e preencher valores na loja.';

  @override
  String get shoppingListsNewList => 'Nova lista';

  @override
  String get shoppingListUntitled => 'Lista sem título';

  @override
  String shoppingListItemsCount(int count) {
    return '$count itens';
  }

  @override
  String shoppingListSubtotal(String amount) {
    return 'Subtotal: $amount';
  }

  @override
  String get shoppingListNoPriceYet => 'Sem valor (preencher na loja)';

  @override
  String get shoppingListAddItem => 'Adicionar item';

  @override
  String get shoppingListItemLabelHint => 'Descrição do item';

  @override
  String get shoppingListItemAmountOptional => 'Valor (opcional)';

  @override
  String get shoppingListDeleteItem => 'Remover item';

  @override
  String get shoppingListConfirmDeleteDraftTitle => 'Apagar lista?';

  @override
  String get shoppingListConfirmDeleteDraftBody =>
      'Esta lista em rascunho será eliminada. Esta ação não pode ser desfeita.';

  @override
  String get shoppingListComplete => 'Fechar compra';

  @override
  String get shoppingListCompleteTitle => 'Lançar despesa';

  @override
  String get shoppingListTotalOverrideHint => 'Total manual (opcional)';

  @override
  String get shoppingListDescriptionOptional =>
      'Descrição da despesa (opcional)';

  @override
  String get shoppingListMarkPaid => 'Já paguei';

  @override
  String get shoppingListExpenseDate => 'Data da despesa';

  @override
  String get shoppingListViewExpense => 'Ver despesa';

  @override
  String shoppingListCompletedOn(String date) {
    return 'Concluída em $date';
  }

  @override
  String get shoppingListEditMetaTitle => 'Título e loja';

  @override
  String get shoppingListTitleOptional => 'Título (opcional)';

  @override
  String get shoppingListStoreOptional => 'Loja (opcional)';

  @override
  String get shoppingListErrorLoad => 'Não foi possível carregar as listas.';

  @override
  String get shoppingListReadOnlyDraft =>
      'Só o autor pode editar esta lista em rascunho.';

  @override
  String get shoppingListNoItems =>
      'Sem itens. Adiciona produtos para planear a compra.';

  @override
  String get shoppingListFooterEstimatedTotal => 'Total estimado';

  @override
  String get shoppingListFooterEstimatedNote =>
      'Soma só dos itens com valor preenchido.';

  @override
  String get shoppingListInlineAmountHint => 'Valor';

  @override
  String get shoppingListEditLabelTitle => 'Nome do item';

  @override
  String get shoppingListConfirmRemoveItemTitle => 'Remover item?';

  @override
  String get shoppingListConfirmRemoveItemBody => 'Este produto sai da lista.';

  @override
  String get shoppingListEditItemTitle => 'Editar item';

  @override
  String get shoppingListCompleteSuccess =>
      'Despesa registada. A lista foi movida para o histórico.';
}
