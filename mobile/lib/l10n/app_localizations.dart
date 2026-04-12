import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_pt.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('pt'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In pt, this message translates to:
  /// **'Well Paid'**
  String get appTitle;

  /// No description provided for @settingsTitle.
  ///
  /// In pt, this message translates to:
  /// **'Configurações'**
  String get settingsTitle;

  /// No description provided for @settingsLanguageTitle.
  ///
  /// In pt, this message translates to:
  /// **'Idioma da interface'**
  String get settingsLanguageTitle;

  /// No description provided for @settingsLanguageSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Textos do aplicativo em português (Brasil) ou inglês (EUA).'**
  String get settingsLanguageSubtitle;

  /// No description provided for @langPortugueseBrazil.
  ///
  /// In pt, this message translates to:
  /// **'Português (Brasil)'**
  String get langPortugueseBrazil;

  /// No description provided for @langEnglishUS.
  ///
  /// In pt, this message translates to:
  /// **'English (US)'**
  String get langEnglishUS;

  /// No description provided for @settingsLanguageUpdated.
  ///
  /// In pt, this message translates to:
  /// **'Idioma atualizado.'**
  String get settingsLanguageUpdated;

  /// No description provided for @save.
  ///
  /// In pt, this message translates to:
  /// **'Salvar'**
  String get save;

  /// No description provided for @cancel.
  ///
  /// In pt, this message translates to:
  /// **'Cancelar'**
  String get cancel;

  /// No description provided for @ok.
  ///
  /// In pt, this message translates to:
  /// **'OK'**
  String get ok;

  /// No description provided for @copy.
  ///
  /// In pt, this message translates to:
  /// **'Copiar'**
  String get copy;

  /// No description provided for @confirm.
  ///
  /// In pt, this message translates to:
  /// **'Confirmar'**
  String get confirm;

  /// No description provided for @delete.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar'**
  String get delete;

  /// No description provided for @close.
  ///
  /// In pt, this message translates to:
  /// **'Fechar'**
  String get close;

  /// No description provided for @errorGeneric.
  ///
  /// In pt, this message translates to:
  /// **'Erro.'**
  String get errorGeneric;

  /// No description provided for @dioRequestTimeout.
  ///
  /// In pt, this message translates to:
  /// **'Tempo esgotado. Confirma que o backend está a correr e que o URL da API está certo.'**
  String get dioRequestTimeout;

  /// No description provided for @dioConnectionFailed.
  ///
  /// In pt, this message translates to:
  /// **'Sem ligação ao servidor. No emulador Android use 10.0.2.2; no Windows ou iOS use 127.0.0.1 (--dart-define=API_BASE_URL=…).'**
  String get dioConnectionFailed;

  /// No description provided for @dioNetworkFallback.
  ///
  /// In pt, this message translates to:
  /// **'Erro de rede'**
  String get dioNetworkFallback;

  /// No description provided for @tryAgain.
  ///
  /// In pt, this message translates to:
  /// **'Tentar novamente'**
  String get tryAgain;

  /// No description provided for @requiredField.
  ///
  /// In pt, this message translates to:
  /// **'Obrigatório'**
  String get requiredField;

  /// No description provided for @valueInvalid.
  ///
  /// In pt, this message translates to:
  /// **'Valor inválido.'**
  String get valueInvalid;

  /// No description provided for @homeDashboardTitle.
  ///
  /// In pt, this message translates to:
  /// **'Dashboard'**
  String get homeDashboardTitle;

  /// No description provided for @tooltipSettings.
  ///
  /// In pt, this message translates to:
  /// **'Configurações'**
  String get tooltipSettings;

  /// No description provided for @tooltipSecurity.
  ///
  /// In pt, this message translates to:
  /// **'Segurança'**
  String get tooltipSecurity;

  /// No description provided for @tooltipFamily.
  ///
  /// In pt, this message translates to:
  /// **'Família'**
  String get tooltipFamily;

  /// No description provided for @tooltipRefreshDashboard.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar dashboard'**
  String get tooltipRefreshDashboard;

  /// No description provided for @tooltipLogout.
  ///
  /// In pt, this message translates to:
  /// **'Sair'**
  String get tooltipLogout;

  /// No description provided for @navHome.
  ///
  /// In pt, this message translates to:
  /// **'Início'**
  String get navHome;

  /// No description provided for @navExpenses.
  ///
  /// In pt, this message translates to:
  /// **'Despesas'**
  String get navExpenses;

  /// No description provided for @navIncomes.
  ///
  /// In pt, this message translates to:
  /// **'Proventos'**
  String get navIncomes;

  /// No description provided for @navGoals.
  ///
  /// In pt, this message translates to:
  /// **'Metas'**
  String get navGoals;

  /// No description provided for @navReserve.
  ///
  /// In pt, this message translates to:
  /// **'Reserva'**
  String get navReserve;

  /// No description provided for @navQuickPanelToggleHint.
  ///
  /// In pt, this message translates to:
  /// **'Puxar ou tocar para atalhos A pagar e Listas de compras'**
  String get navQuickPanelToggleHint;

  /// No description provided for @menuMoreTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Mais opções'**
  String get menuMoreTooltip;

  /// No description provided for @logoutConfirmTitle.
  ///
  /// In pt, this message translates to:
  /// **'Terminar sessão?'**
  String get logoutConfirmTitle;

  /// No description provided for @logoutConfirmBody.
  ///
  /// In pt, this message translates to:
  /// **'Terá de voltar a iniciar sessão para aceder à conta.'**
  String get logoutConfirmBody;

  /// No description provided for @homeQuickExpenses.
  ///
  /// In pt, this message translates to:
  /// **'Despesas'**
  String get homeQuickExpenses;

  /// No description provided for @homeQuickIncomes.
  ///
  /// In pt, this message translates to:
  /// **'Proventos'**
  String get homeQuickIncomes;

  /// No description provided for @homeQuickGoals.
  ///
  /// In pt, this message translates to:
  /// **'Metas'**
  String get homeQuickGoals;

  /// No description provided for @homeQuickReserve.
  ///
  /// In pt, this message translates to:
  /// **'Reserva'**
  String get homeQuickReserve;

  /// No description provided for @homeDashboardError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar o dashboard.'**
  String get homeDashboardError;

  /// No description provided for @authLoginTitle.
  ///
  /// In pt, this message translates to:
  /// **'Entrar na conta'**
  String get authLoginTitle;

  /// No description provided for @authEmail.
  ///
  /// In pt, this message translates to:
  /// **'E-mail'**
  String get authEmail;

  /// No description provided for @authPassword.
  ///
  /// In pt, this message translates to:
  /// **'Senha'**
  String get authPassword;

  /// No description provided for @authShowPassword.
  ///
  /// In pt, this message translates to:
  /// **'Mostrar senha'**
  String get authShowPassword;

  /// No description provided for @authHidePassword.
  ///
  /// In pt, this message translates to:
  /// **'Ocultar senha'**
  String get authHidePassword;

  /// No description provided for @authPasswordRequired.
  ///
  /// In pt, this message translates to:
  /// **'Informe a senha'**
  String get authPasswordRequired;

  /// No description provided for @authRememberCredentials.
  ///
  /// In pt, this message translates to:
  /// **'Lembrar e-mail e senha neste aparelho'**
  String get authRememberCredentials;

  /// No description provided for @authForgotPassword.
  ///
  /// In pt, this message translates to:
  /// **'Esqueceu a senha?'**
  String get authForgotPassword;

  /// No description provided for @authEnter.
  ///
  /// In pt, this message translates to:
  /// **'Entrar'**
  String get authEnter;

  /// No description provided for @authLoginError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao entrar'**
  String get authLoginError;

  /// No description provided for @authNoAccountYet.
  ///
  /// In pt, this message translates to:
  /// **'Ainda sem conta?'**
  String get authNoAccountYet;

  /// No description provided for @authCreateAccount.
  ///
  /// In pt, this message translates to:
  /// **'Criar conta'**
  String get authCreateAccount;

  /// No description provided for @authCopyright.
  ///
  /// In pt, this message translates to:
  /// **'Copyright © 2026 Andrino Cabral. All rights reserved.'**
  String get authCopyright;

  /// No description provided for @authRegisterTitle.
  ///
  /// In pt, this message translates to:
  /// **'Criar conta'**
  String get authRegisterTitle;

  /// No description provided for @authRegisterSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Começa em poucos passos.'**
  String get authRegisterSubtitle;

  /// No description provided for @authRegisterError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao registar'**
  String get authRegisterError;

  /// No description provided for @authPasswordPolicyHint.
  ///
  /// In pt, this message translates to:
  /// **'Mín. 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial.'**
  String get authPasswordPolicyHint;

  /// No description provided for @authPasswordRulesError.
  ///
  /// In pt, this message translates to:
  /// **'Senha não cumpre as regras'**
  String get authPasswordRulesError;

  /// No description provided for @authNameOptional.
  ///
  /// In pt, this message translates to:
  /// **'Nome (opcional)'**
  String get authNameOptional;

  /// No description provided for @authPhoneOptional.
  ///
  /// In pt, this message translates to:
  /// **'Telemóvel (opcional)'**
  String get authPhoneOptional;

  /// No description provided for @authConfirmPassword.
  ///
  /// In pt, this message translates to:
  /// **'Confirmar senha'**
  String get authConfirmPassword;

  /// No description provided for @authConfirmPasswordRequired.
  ///
  /// In pt, this message translates to:
  /// **'Confirme a senha'**
  String get authConfirmPasswordRequired;

  /// No description provided for @authPasswordMismatch.
  ///
  /// In pt, this message translates to:
  /// **'As senhas não coincidem'**
  String get authPasswordMismatch;

  /// No description provided for @authRegisterButton.
  ///
  /// In pt, this message translates to:
  /// **'Registar'**
  String get authRegisterButton;

  /// No description provided for @authVerifyEmailTitle.
  ///
  /// In pt, this message translates to:
  /// **'Confirmar e-mail'**
  String get authVerifyEmailTitle;

  /// No description provided for @authVerifyEmailSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Introduza o código de 6 dígitos que enviámos para o seu e-mail.'**
  String get authVerifyEmailSubtitle;

  /// No description provided for @authVerifyEmailFromLink.
  ///
  /// In pt, this message translates to:
  /// **'A confirmar o seu e-mail…'**
  String get authVerifyEmailFromLink;

  /// No description provided for @authVerificationCodeLabel.
  ///
  /// In pt, this message translates to:
  /// **'Código'**
  String get authVerificationCodeLabel;

  /// No description provided for @authVerificationCodeHint.
  ///
  /// In pt, this message translates to:
  /// **'000000'**
  String get authVerificationCodeHint;

  /// No description provided for @authVerificationCodeError.
  ///
  /// In pt, this message translates to:
  /// **'Introduza os 6 dígitos'**
  String get authVerificationCodeError;

  /// No description provided for @authVerifyEmailButton.
  ///
  /// In pt, this message translates to:
  /// **'Confirmar e entrar'**
  String get authVerifyEmailButton;

  /// No description provided for @authResendVerification.
  ///
  /// In pt, this message translates to:
  /// **'Reenviar código'**
  String get authResendVerification;

  /// No description provided for @authVerifyEmailSuccess.
  ///
  /// In pt, this message translates to:
  /// **'E-mail confirmado. Bem-vindo!'**
  String get authVerifyEmailSuccess;

  /// No description provided for @authVerifyEmailError.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível confirmar'**
  String get authVerifyEmailError;

  /// No description provided for @authResendVerificationError.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível reenviar'**
  String get authResendVerificationError;

  /// No description provided for @authVerifyEmailAction.
  ///
  /// In pt, this message translates to:
  /// **'Confirmar e-mail'**
  String get authVerifyEmailAction;

  /// No description provided for @authDevVerificationHint.
  ///
  /// In pt, this message translates to:
  /// **'Token (link):\n{token}\n\nCódigo:\n{code}'**
  String authDevVerificationHint(String token, String code);

  /// No description provided for @authAlreadyHaveAccount.
  ///
  /// In pt, this message translates to:
  /// **'Já tenho conta — entrar'**
  String get authAlreadyHaveAccount;

  /// No description provided for @authEmailRequired.
  ///
  /// In pt, this message translates to:
  /// **'Informe o e-mail'**
  String get authEmailRequired;

  /// No description provided for @authEmailInvalid.
  ///
  /// In pt, this message translates to:
  /// **'E-mail inválido'**
  String get authEmailInvalid;

  /// No description provided for @authForgotTitle.
  ///
  /// In pt, this message translates to:
  /// **'Recuperar senha'**
  String get authForgotTitle;

  /// No description provided for @authForgotSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Indica o teu e-mail para receberes instruções.'**
  String get authForgotSubtitle;

  /// No description provided for @authForgotBody.
  ///
  /// In pt, this message translates to:
  /// **'Enviaremos um código para redefinires a senha na app. Se não vês o e-mail, verifica o spam.'**
  String get authForgotBody;

  /// No description provided for @authForgotSend.
  ///
  /// In pt, this message translates to:
  /// **'Enviar instruções'**
  String get authForgotSend;

  /// No description provided for @authForgotError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao enviar pedido'**
  String get authForgotError;

  /// No description provided for @authBackToLogin.
  ///
  /// In pt, this message translates to:
  /// **'Voltar ao login'**
  String get authBackToLogin;

  /// No description provided for @authDevModeTitle.
  ///
  /// In pt, this message translates to:
  /// **'Modo desenvolvimento'**
  String get authDevModeTitle;

  /// No description provided for @authDevTokenHint.
  ///
  /// In pt, this message translates to:
  /// **'O backend devolveu o token para testes. Salve-o e use no ecrã seguinte.\n\n{token}'**
  String authDevTokenHint(String token);

  /// No description provided for @tokenCopied.
  ///
  /// In pt, this message translates to:
  /// **'Token copiado'**
  String get tokenCopied;

  /// No description provided for @dashMonthSummary.
  ///
  /// In pt, this message translates to:
  /// **'Resumo do mês'**
  String get dashMonthSummary;

  /// No description provided for @dashIncome.
  ///
  /// In pt, this message translates to:
  /// **'Receitas'**
  String get dashIncome;

  /// No description provided for @dashExpenses.
  ///
  /// In pt, this message translates to:
  /// **'Despesas'**
  String get dashExpenses;

  /// No description provided for @dashBalance.
  ///
  /// In pt, this message translates to:
  /// **'Saldo'**
  String get dashBalance;

  /// No description provided for @dashByCategory.
  ///
  /// In pt, this message translates to:
  /// **'Despesas por categoria'**
  String get dashByCategory;

  /// No description provided for @dashHomeChartTabCategory.
  ///
  /// In pt, this message translates to:
  /// **'Categorias'**
  String get dashHomeChartTabCategory;

  /// No description provided for @dashHomeChartTabCashflow.
  ///
  /// In pt, this message translates to:
  /// **'Fluxo'**
  String get dashHomeChartTabCashflow;

  /// No description provided for @dashPendingThisMonthTitle.
  ///
  /// In pt, this message translates to:
  /// **'Contas a pagar'**
  String get dashPendingThisMonthTitle;

  /// No description provided for @dashPendingThisMonthSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Toque para rever e quitar'**
  String get dashPendingThisMonthSubtitle;

  /// No description provided for @chartViewCategoryExpenses.
  ///
  /// In pt, this message translates to:
  /// **'Ver despesas desta categoria'**
  String get chartViewCategoryExpenses;

  /// No description provided for @chartRegisterExpenseCta.
  ///
  /// In pt, this message translates to:
  /// **'Registar despesa'**
  String get chartRegisterExpenseCta;

  /// No description provided for @expenseListFilteredByCategory.
  ///
  /// In pt, this message translates to:
  /// **'Filtrado por categoria'**
  String get expenseListFilteredByCategory;

  /// No description provided for @expenseListClearCategoryFilter.
  ///
  /// In pt, this message translates to:
  /// **'Limpar'**
  String get expenseListClearCategoryFilter;

  /// No description provided for @dashToPay.
  ///
  /// In pt, this message translates to:
  /// **'A pagar'**
  String get dashToPay;

  /// No description provided for @toPayScreenSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Todas as despesas pendentes por ordem cronológica de vencimento. Parcelas aparecem em linhas separadas. As cores indicam a proximidade do vencimento.'**
  String get toPayScreenSubtitle;

  /// No description provided for @toPayViewAllExpenses.
  ///
  /// In pt, this message translates to:
  /// **'Todas as despesas'**
  String get toPayViewAllExpenses;

  /// No description provided for @toPayDueOn.
  ///
  /// In pt, this message translates to:
  /// **'Vence {date}'**
  String toPayDueOn(String date);

  /// No description provided for @toPayCompetenceOn.
  ///
  /// In pt, this message translates to:
  /// **'Competência {date} (sem data de vencimento)'**
  String toPayCompetenceOn(String date);

  /// No description provided for @toPayProjectedHint.
  ///
  /// In pt, this message translates to:
  /// **'Prevista — a linha efetiva surge alguns dias antes do vencimento.'**
  String get toPayProjectedHint;

  /// No description provided for @toPayOfflineCacheBanner.
  ///
  /// In pt, this message translates to:
  /// **'A mostrar dados guardados — a lista pode estar incompleta até voltar a ter rede.'**
  String get toPayOfflineCacheBanner;

  /// No description provided for @toPayPendingTotal.
  ///
  /// In pt, this message translates to:
  /// **'Total pendente: {amount}'**
  String toPayPendingTotal(String amount);

  /// No description provided for @toPayFilterAll.
  ///
  /// In pt, this message translates to:
  /// **'Todas'**
  String get toPayFilterAll;

  /// No description provided for @toPayFilterOverdue.
  ///
  /// In pt, this message translates to:
  /// **'Atrasadas'**
  String get toPayFilterOverdue;

  /// No description provided for @toPayFilterThisWeek.
  ///
  /// In pt, this message translates to:
  /// **'Esta semana'**
  String get toPayFilterThisWeek;

  /// No description provided for @toPaySectionOverdue.
  ///
  /// In pt, this message translates to:
  /// **'Atrasadas'**
  String get toPaySectionOverdue;

  /// No description provided for @toPaySectionThisWeek.
  ///
  /// In pt, this message translates to:
  /// **'Vencem esta semana'**
  String get toPaySectionThisWeek;

  /// No description provided for @toPaySectionLater.
  ///
  /// In pt, this message translates to:
  /// **'Mais tarde'**
  String get toPaySectionLater;

  /// No description provided for @toPayFilterEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Nada corresponde a este filtro.'**
  String get toPayFilterEmpty;

  /// No description provided for @toPayLegendTitle.
  ///
  /// In pt, this message translates to:
  /// **'Cores do vencimento'**
  String get toPayLegendTitle;

  /// No description provided for @toPayLegendOverdue.
  ///
  /// In pt, this message translates to:
  /// **'Já passou a data de vencimento'**
  String get toPayLegendOverdue;

  /// No description provided for @toPayLegendDueToday.
  ///
  /// In pt, this message translates to:
  /// **'Vence hoje'**
  String get toPayLegendDueToday;

  /// No description provided for @toPayLegendDueSoon.
  ///
  /// In pt, this message translates to:
  /// **'Vence em 1–3 dias'**
  String get toPayLegendDueSoon;

  /// No description provided for @toPayLegendUpcoming.
  ///
  /// In pt, this message translates to:
  /// **'Vence em 4–10 dias'**
  String get toPayLegendUpcoming;

  /// No description provided for @toPayLegendSafe.
  ///
  /// In pt, this message translates to:
  /// **'Vence daqui a 11 dias ou mais'**
  String get toPayLegendSafe;

  /// No description provided for @toPayQuickPaySwitchTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Ligue para confirmar o pagamento; a lista atualiza após sucesso. Se falhar, o interruptor volta a desligar.'**
  String get toPayQuickPaySwitchTooltip;

  /// No description provided for @expenseListNextInstallmentLine.
  ///
  /// In pt, this message translates to:
  /// **'Próx.: {date}'**
  String expenseListNextInstallmentLine(String date);

  /// No description provided for @expenseTagPar.
  ///
  /// In pt, this message translates to:
  /// **'PAR'**
  String get expenseTagPar;

  /// No description provided for @expenseTagParA11y.
  ///
  /// In pt, this message translates to:
  /// **'Despesa parcelada'**
  String get expenseTagParA11y;

  /// No description provided for @expenseTagRec.
  ///
  /// In pt, this message translates to:
  /// **'REC'**
  String get expenseTagRec;

  /// No description provided for @expenseTagRecA11y.
  ///
  /// In pt, this message translates to:
  /// **'Despesa recorrente'**
  String get expenseTagRecA11y;

  /// No description provided for @expenseNextDueSectionTitle.
  ///
  /// In pt, this message translates to:
  /// **'Próxima ocorrência'**
  String get expenseNextDueSectionTitle;

  /// No description provided for @expenseNextDueCompetenceLine.
  ///
  /// In pt, this message translates to:
  /// **'Competência: {date}'**
  String expenseNextDueCompetenceLine(String date);

  /// No description provided for @expenseNextDueDateLine.
  ///
  /// In pt, this message translates to:
  /// **'Vencimento: {date}'**
  String expenseNextDueDateLine(String date);

  /// No description provided for @expenseInstallmentNextSectionTitle.
  ///
  /// In pt, this message translates to:
  /// **'Próxima parcela'**
  String get expenseInstallmentNextSectionTitle;

  /// No description provided for @expenseInstallmentLastSectionTitle.
  ///
  /// In pt, this message translates to:
  /// **'Última parcela'**
  String get expenseInstallmentLastSectionTitle;

  /// No description provided for @expensePayEarlyTitle.
  ///
  /// In pt, this message translates to:
  /// **'Pagar antecipadamente?'**
  String get expensePayEarlyTitle;

  /// No description provided for @expensePayEarlyBody.
  ///
  /// In pt, this message translates to:
  /// **'Esta despesa é de um mês posterior ao atual. Tem a certeza de que quer marcá-la como paga agora?'**
  String get expensePayEarlyBody;

  /// No description provided for @dashNothingPending.
  ///
  /// In pt, this message translates to:
  /// **'Nada pendente.'**
  String get dashNothingPending;

  /// No description provided for @dashPendingTotal.
  ///
  /// In pt, this message translates to:
  /// **'Total pendente'**
  String get dashPendingTotal;

  /// No description provided for @dashSeeAll.
  ///
  /// In pt, this message translates to:
  /// **'Ver todas'**
  String get dashSeeAll;

  /// No description provided for @dashUpcomingDue.
  ///
  /// In pt, this message translates to:
  /// **'Próximos vencimentos'**
  String get dashUpcomingDue;

  /// No description provided for @dashNoUpcomingInNextMonth.
  ///
  /// In pt, this message translates to:
  /// **'Sem contas com vencimento no próximo mês.'**
  String get dashNoUpcomingInNextMonth;

  /// No description provided for @dashSeeMore.
  ///
  /// In pt, this message translates to:
  /// **'Ver mais'**
  String get dashSeeMore;

  /// No description provided for @dashGoals.
  ///
  /// In pt, this message translates to:
  /// **'Metas'**
  String get dashGoals;

  /// No description provided for @dashNoActiveGoals.
  ///
  /// In pt, this message translates to:
  /// **'Sem metas ativas no momento.'**
  String get dashNoActiveGoals;

  /// No description provided for @dashSeeGoals.
  ///
  /// In pt, this message translates to:
  /// **'Ver metas'**
  String get dashSeeGoals;

  /// No description provided for @dashEmergencyReserve.
  ///
  /// In pt, this message translates to:
  /// **'Reserva de emergência'**
  String get dashEmergencyReserve;

  /// No description provided for @dashEmergencyReserveBalance.
  ///
  /// In pt, this message translates to:
  /// **'Saldo acumulado'**
  String get dashEmergencyReserveBalance;

  /// No description provided for @dashEmergencyReserveMonthly.
  ///
  /// In pt, this message translates to:
  /// **'Meta mensal'**
  String get dashEmergencyReserveMonthly;

  /// No description provided for @dashEmergencyReserveTimesTarget.
  ///
  /// In pt, this message translates to:
  /// **'x da meta'**
  String get dashEmergencyReserveTimesTarget;

  /// No description provided for @dashEmergencyReserveAnnualProgress.
  ///
  /// In pt, this message translates to:
  /// **'{percent}% do objetivo anual'**
  String dashEmergencyReserveAnnualProgress(int percent);

  /// No description provided for @dashEmergencyReserveMomentum.
  ///
  /// In pt, this message translates to:
  /// **'Siga assim'**
  String get dashEmergencyReserveMomentum;

  /// No description provided for @dashEmergencyReserveAnnualDone.
  ///
  /// In pt, this message translates to:
  /// **'Objetivo anual atingido'**
  String get dashEmergencyReserveAnnualDone;

  /// No description provided for @dashEmergencyReserveStageFirst.
  ///
  /// In pt, this message translates to:
  /// **'Primeiro passo'**
  String get dashEmergencyReserveStageFirst;

  /// No description provided for @dashEmergencyReserveStageStart.
  ///
  /// In pt, this message translates to:
  /// **'Bom ritmo'**
  String get dashEmergencyReserveStageStart;

  /// No description provided for @dashEmergencyReserveStageMid.
  ///
  /// In pt, this message translates to:
  /// **'Metade do caminho'**
  String get dashEmergencyReserveStageMid;

  /// No description provided for @dashEmergencyReserveStageStrong.
  ///
  /// In pt, this message translates to:
  /// **'Reta final'**
  String get dashEmergencyReserveStageStrong;

  /// No description provided for @dashEmergencyReserveStageDone.
  ///
  /// In pt, this message translates to:
  /// **'Objetivo anual atingido'**
  String get dashEmergencyReserveStageDone;

  /// No description provided for @dashEmergencyReserveFootnote.
  ///
  /// In pt, this message translates to:
  /// **'Cada mês civil acrescenta a meta ao saldo (registo interno; não substitui contas bancárias).'**
  String get dashEmergencyReserveFootnote;

  /// No description provided for @dashEmergencyReserveConfigure.
  ///
  /// In pt, this message translates to:
  /// **'Configurar'**
  String get dashEmergencyReserveConfigure;

  /// No description provided for @emergencyReserveTitle.
  ///
  /// In pt, this message translates to:
  /// **'Reserva de emergência'**
  String get emergencyReserveTitle;

  /// No description provided for @emergencyReserveIntro.
  ///
  /// In pt, this message translates to:
  /// **'Define quanto queres poupar por mês. O saldo aumenta automaticamente em cada mês civil, a partir do mês em que activares a meta.'**
  String get emergencyReserveIntro;

  /// No description provided for @emergencyReserveMonthlyLabel.
  ///
  /// In pt, this message translates to:
  /// **'Valor a poupar por mês (R\$)'**
  String get emergencyReserveMonthlyLabel;

  /// No description provided for @emergencyReserveQuickPickTitle.
  ///
  /// In pt, this message translates to:
  /// **'Atalhos rápidos'**
  String get emergencyReserveQuickPickTitle;

  /// No description provided for @emergencyReserveSave.
  ///
  /// In pt, this message translates to:
  /// **'Salvar meta'**
  String get emergencyReserveSave;

  /// No description provided for @emergencyReserveSavedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Meta de reserva actualizada.'**
  String get emergencyReserveSavedSnackbar;

  /// No description provided for @emergencyReserveError.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível guardar.'**
  String get emergencyReserveError;

  /// No description provided for @emergencyReserveAccrualListTitle.
  ///
  /// In pt, this message translates to:
  /// **'Adesões mensais'**
  String get emergencyReserveAccrualListTitle;

  /// No description provided for @emergencyReserveAccrualListEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Ainda não existem adesões mensais.'**
  String get emergencyReserveAccrualListEmpty;

  /// No description provided for @emergencyReserveAccrualListCredit.
  ///
  /// In pt, this message translates to:
  /// **'Crédito aplicado'**
  String get emergencyReserveAccrualListCredit;

  /// No description provided for @emergencyReserveAccrualEdit.
  ///
  /// In pt, this message translates to:
  /// **'Editar crédito'**
  String get emergencyReserveAccrualEdit;

  /// No description provided for @emergencyReserveAccrualDelete.
  ///
  /// In pt, this message translates to:
  /// **'Remover crédito'**
  String get emergencyReserveAccrualDelete;

  /// No description provided for @emergencyReserveAccrualDeleteTitle.
  ///
  /// In pt, this message translates to:
  /// **'Remover crédito de {month}?'**
  String emergencyReserveAccrualDeleteTitle(String month);

  /// No description provided for @emergencyReserveAccrualDeleteBody.
  ///
  /// In pt, this message translates to:
  /// **'O saldo será ajustado. Este mês deixa de receber crédito automático (podes voltar a acrescentar manualmente).'**
  String get emergencyReserveAccrualDeleteBody;

  /// No description provided for @emergencyReserveAccrualDeleteConfirm.
  ///
  /// In pt, this message translates to:
  /// **'Remover'**
  String get emergencyReserveAccrualDeleteConfirm;

  /// No description provided for @emergencyReserveAccrualEditTitle.
  ///
  /// In pt, this message translates to:
  /// **'Crédito de {month}'**
  String emergencyReserveAccrualEditTitle(String month);

  /// No description provided for @emergencyReserveAccrualUpdatedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Crédito actualizado.'**
  String get emergencyReserveAccrualUpdatedSnackbar;

  /// No description provided for @emergencyReserveAccrualRemovedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Crédito removido.'**
  String get emergencyReserveAccrualRemovedSnackbar;

  /// No description provided for @emergencyReserveResetAction.
  ///
  /// In pt, this message translates to:
  /// **'Limpar reserva e histórico'**
  String get emergencyReserveResetAction;

  /// No description provided for @emergencyReserveResetTitle.
  ///
  /// In pt, this message translates to:
  /// **'Limpar tudo?'**
  String get emergencyReserveResetTitle;

  /// No description provided for @emergencyReserveResetBody.
  ///
  /// In pt, this message translates to:
  /// **'Remove a meta, o saldo e todos os créditos mensais. Esta acção não se pode desfazer.'**
  String get emergencyReserveResetBody;

  /// No description provided for @emergencyReserveResetConfirm.
  ///
  /// In pt, this message translates to:
  /// **'Limpar'**
  String get emergencyReserveResetConfirm;

  /// No description provided for @emergencyReserveResetSuccess.
  ///
  /// In pt, this message translates to:
  /// **'Reserva reposta.'**
  String get emergencyReserveResetSuccess;

  /// No description provided for @reserveMilestoneBannerQuarter.
  ///
  /// In pt, this message translates to:
  /// **'Já alcançaste um quarto do objetivo anual da reserva — bom ritmo.'**
  String get reserveMilestoneBannerQuarter;

  /// No description provided for @reserveMilestoneBannerHalf.
  ///
  /// In pt, this message translates to:
  /// **'Metade do caminho em relação ao teu ano de poupança.'**
  String get reserveMilestoneBannerHalf;

  /// No description provided for @reserveMilestoneBannerAlmost.
  ///
  /// In pt, this message translates to:
  /// **'Quase lá: falta pouco para fechares o objetivo anual da reserva.'**
  String get reserveMilestoneBannerAlmost;

  /// No description provided for @reserveMilestoneBannerComplete.
  ///
  /// In pt, this message translates to:
  /// **'Objetivo anual da reserva atingido. Disciplina em destaque.'**
  String get reserveMilestoneBannerComplete;

  /// No description provided for @settingsEmergencyReserve.
  ///
  /// In pt, this message translates to:
  /// **'Reserva de emergência'**
  String get settingsEmergencyReserve;

  /// No description provided for @settingsNotificationsSection.
  ///
  /// In pt, this message translates to:
  /// **'Notificações'**
  String get settingsNotificationsSection;

  /// No description provided for @settingsGoalStallReminderTitle.
  ///
  /// In pt, this message translates to:
  /// **'Lembrete de metas paradas'**
  String get settingsGoalStallReminderTitle;

  /// No description provided for @settingsGoalStallReminderSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Uma notificação local se não actualizares uma meta há cerca de 3 semanas (usa a data da última actualização).'**
  String get settingsGoalStallReminderSubtitle;

  /// No description provided for @settingsGoalStallPermissionDenied.
  ///
  /// In pt, this message translates to:
  /// **'Activa as notificações nas definições do sistema para receberes o lembrete.'**
  String get settingsGoalStallPermissionDenied;

  /// No description provided for @goalStallNotifTitle.
  ///
  /// In pt, this message translates to:
  /// **'Metas sem movimento'**
  String get goalStallNotifTitle;

  /// No description provided for @goalStallNotifBodySingle.
  ///
  /// In pt, this message translates to:
  /// **'«{name}» — já passou algum tempo sem actualização. Queres rever?'**
  String goalStallNotifBodySingle(String name);

  /// No description provided for @goalStallNotifBodyTwo.
  ///
  /// In pt, this message translates to:
  /// **'«{a}» e «{b}» — metas há tempo sem actualização.'**
  String goalStallNotifBodyTwo(String a, String b);

  /// No description provided for @goalStallNotifBodyMany.
  ///
  /// In pt, this message translates to:
  /// **'«{first}» e mais {count} — não te esqueças das metas.'**
  String goalStallNotifBodyMany(String first, int count);

  /// No description provided for @dashMarkPaidTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Marcar como paga'**
  String get dashMarkPaidTooltip;

  /// No description provided for @dashDueShort.
  ///
  /// In pt, this message translates to:
  /// **'Venc.'**
  String get dashDueShort;

  /// No description provided for @dashDueVerb.
  ///
  /// In pt, this message translates to:
  /// **'Vence'**
  String get dashDueVerb;

  /// No description provided for @dashFamilySuffix.
  ///
  /// In pt, this message translates to:
  /// **' · Família'**
  String get dashFamilySuffix;

  /// No description provided for @dashGoalFamilySuffix.
  ///
  /// In pt, this message translates to:
  /// **' (família)'**
  String get dashGoalFamilySuffix;

  /// No description provided for @dashPendingItemA11y.
  ///
  /// In pt, this message translates to:
  /// **'{description}, {amount}, vencimento {due}'**
  String dashPendingItemA11y(String description, String amount, String due);

  /// No description provided for @periodSummaryA11y.
  ///
  /// In pt, this message translates to:
  /// **'Período do resumo, {label}'**
  String periodSummaryA11y(String label);

  /// No description provided for @periodPrevMonth.
  ///
  /// In pt, this message translates to:
  /// **'Mês anterior'**
  String get periodPrevMonth;

  /// No description provided for @periodNextMonth.
  ///
  /// In pt, this message translates to:
  /// **'Próximo mês'**
  String get periodNextMonth;

  /// No description provided for @chartTotalExpenses.
  ///
  /// In pt, this message translates to:
  /// **'Total despesas'**
  String get chartTotalExpenses;

  /// No description provided for @chartNoExpensesThisMonth.
  ///
  /// In pt, this message translates to:
  /// **'Sem despesas\nneste mês.'**
  String get chartNoExpensesThisMonth;

  /// No description provided for @chartNoExpensesRegistered.
  ///
  /// In pt, this message translates to:
  /// **'Sem despesas registadas\nneste mês.'**
  String get chartNoExpensesRegistered;

  /// No description provided for @chartSemanticsWithData.
  ///
  /// In pt, this message translates to:
  /// **'Gráfico de despesas por categoria, total {total}'**
  String chartSemanticsWithData(String total);

  /// No description provided for @chartSemanticsNoData.
  ///
  /// In pt, this message translates to:
  /// **'Gráfico de despesas por categoria, sem dados no mês'**
  String get chartSemanticsNoData;

  /// No description provided for @chartCategoriesHint.
  ///
  /// In pt, this message translates to:
  /// **'As categorias aparecem quando existirem despesas neste mês.'**
  String get chartCategoriesHint;

  /// No description provided for @chartDonutTapHint.
  ///
  /// In pt, this message translates to:
  /// **'Fatia ou grelha — toque para destacar.'**
  String get chartDonutTapHint;

  /// No description provided for @chartDonutSelectedHeading.
  ///
  /// In pt, this message translates to:
  /// **'Categoria selecionada'**
  String get chartDonutSelectedHeading;

  /// No description provided for @chartDonutPctOfTotal.
  ///
  /// In pt, this message translates to:
  /// **'{pct}% do total'**
  String chartDonutPctOfTotal(int pct);

  /// No description provided for @chartCategoryOther.
  ///
  /// In pt, this message translates to:
  /// **'Outros'**
  String get chartCategoryOther;

  /// No description provided for @goalsTitle.
  ///
  /// In pt, this message translates to:
  /// **'Metas'**
  String get goalsTitle;

  /// No description provided for @goalsRefresh.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar'**
  String get goalsRefresh;

  /// No description provided for @goalsLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar metas.'**
  String get goalsLoadError;

  /// No description provided for @goalsEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Sem metas ativas ainda.'**
  String get goalsEmpty;

  /// No description provided for @goalsAddTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Nova meta'**
  String get goalsAddTooltip;

  /// No description provided for @goalsAggregateTitle.
  ///
  /// In pt, this message translates to:
  /// **'Resumo'**
  String get goalsAggregateTitle;

  /// No description provided for @goalsAggregateLine.
  ///
  /// In pt, this message translates to:
  /// **'{count} metas activas · {saved} de {target}'**
  String goalsAggregateLine(int count, String saved, String target);

  /// No description provided for @goalsScreenHint.
  ///
  /// In pt, this message translates to:
  /// **'Progresso visível e totais combinados reforçam o hábito de poupança.'**
  String get goalsScreenHint;

  /// No description provided for @goalMilestoneBannerQuarter.
  ///
  /// In pt, this message translates to:
  /// **'Já alcançaste um quarto do objectivo — bom ritmo.'**
  String get goalMilestoneBannerQuarter;

  /// No description provided for @goalMilestoneBannerHalf.
  ///
  /// In pt, this message translates to:
  /// **'Metade do caminho. Mantém o foco.'**
  String get goalMilestoneBannerHalf;

  /// No description provided for @goalMilestoneBannerAlmost.
  ///
  /// In pt, this message translates to:
  /// **'Quase lá: falta pouco para fechares a meta.'**
  String get goalMilestoneBannerAlmost;

  /// No description provided for @goalMilestoneBannerComplete.
  ///
  /// In pt, this message translates to:
  /// **'Objectivo atingido. Parabéns pela consistência.'**
  String get goalMilestoneBannerComplete;

  /// No description provided for @goalMilestoneChipQuarter.
  ///
  /// In pt, this message translates to:
  /// **'25%'**
  String get goalMilestoneChipQuarter;

  /// No description provided for @goalMilestoneChipHalf.
  ///
  /// In pt, this message translates to:
  /// **'50%'**
  String get goalMilestoneChipHalf;

  /// No description provided for @goalMilestoneChipAlmost.
  ///
  /// In pt, this message translates to:
  /// **'90%'**
  String get goalMilestoneChipAlmost;

  /// No description provided for @goalMilestoneChipComplete.
  ///
  /// In pt, this message translates to:
  /// **'OK'**
  String get goalMilestoneChipComplete;

  /// No description provided for @newGoalTitle.
  ///
  /// In pt, this message translates to:
  /// **'Nova meta'**
  String get newGoalTitle;

  /// No description provided for @goalFormTitleLabel.
  ///
  /// In pt, this message translates to:
  /// **'Nome da meta'**
  String get goalFormTitleLabel;

  /// No description provided for @goalFormTargetLabel.
  ///
  /// In pt, this message translates to:
  /// **'Objetivo (valor a atingir)'**
  String get goalFormTargetLabel;

  /// No description provided for @goalFormIntro.
  ///
  /// In pt, this message translates to:
  /// **'Define um valor-alvo em reais. Podes registar o progresso mais tarde.'**
  String get goalFormIntro;

  /// No description provided for @goalFormSave.
  ///
  /// In pt, this message translates to:
  /// **'Criar meta'**
  String get goalFormSave;

  /// No description provided for @goalFormCreatedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Meta criada.'**
  String get goalFormCreatedSnackbar;

  /// No description provided for @goalSaveError.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível guardar a meta.'**
  String get goalSaveError;

  /// No description provided for @goalFormInitialLabel.
  ///
  /// In pt, this message translates to:
  /// **'Já tenho (opcional)'**
  String get goalFormInitialLabel;

  /// No description provided for @goalFormInitialHint.
  ///
  /// In pt, this message translates to:
  /// **'Quanto já poupaste para esta meta ao criá-la.'**
  String get goalFormInitialHint;

  /// No description provided for @goalDetailTitle.
  ///
  /// In pt, this message translates to:
  /// **'Meta'**
  String get goalDetailTitle;

  /// No description provided for @goalRemaining.
  ///
  /// In pt, this message translates to:
  /// **'Falta'**
  String get goalRemaining;

  /// No description provided for @goalCompleted.
  ///
  /// In pt, this message translates to:
  /// **'Meta atingida'**
  String get goalCompleted;

  /// No description provided for @goalContribute.
  ///
  /// In pt, this message translates to:
  /// **'Adicionar valor'**
  String get goalContribute;

  /// No description provided for @goalContributeTitle.
  ///
  /// In pt, this message translates to:
  /// **'Contribuição'**
  String get goalContributeTitle;

  /// No description provided for @goalContributeAmountLabel.
  ///
  /// In pt, this message translates to:
  /// **'Valor (R\$)'**
  String get goalContributeAmountLabel;

  /// No description provided for @goalContributeNoteLabel.
  ///
  /// In pt, this message translates to:
  /// **'Nota (opcional)'**
  String get goalContributeNoteLabel;

  /// No description provided for @goalContributeSaved.
  ///
  /// In pt, this message translates to:
  /// **'Contribuição registada.'**
  String get goalContributeSaved;

  /// No description provided for @goalContributeError.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível registar.'**
  String get goalContributeError;

  /// No description provided for @goalContributionsTitle.
  ///
  /// In pt, this message translates to:
  /// **'Histórico'**
  String get goalContributionsTitle;

  /// No description provided for @goalContributionsEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Ainda não há contribuições registadas.'**
  String get goalContributionsEmpty;

  /// No description provided for @goalContributionsOwnerOnly.
  ///
  /// In pt, this message translates to:
  /// **'O histórico de contribuições só é visível para quem criou a meta.'**
  String get goalContributionsOwnerOnly;

  /// No description provided for @goalArchive.
  ///
  /// In pt, this message translates to:
  /// **'Arquivar'**
  String get goalArchive;

  /// No description provided for @goalArchiveTitle.
  ///
  /// In pt, this message translates to:
  /// **'Arquivar meta?'**
  String get goalArchiveTitle;

  /// No description provided for @goalArchiveBody.
  ///
  /// In pt, this message translates to:
  /// **'A meta deixa de contar como ativa. Podes reativá-la editando o estado.'**
  String get goalArchiveBody;

  /// No description provided for @goalArchivedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Meta arquivada.'**
  String get goalArchivedSnackbar;

  /// No description provided for @goalDelete.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar'**
  String get goalDelete;

  /// No description provided for @goalDeleteTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar meta?'**
  String get goalDeleteTitle;

  /// No description provided for @goalDeleteBody.
  ///
  /// In pt, this message translates to:
  /// **'Só é possível eliminar quando o saldo da meta é zero.'**
  String get goalDeleteBody;

  /// No description provided for @goalDeletedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Meta eliminada.'**
  String get goalDeletedSnackbar;

  /// No description provided for @goalInactiveBadge.
  ///
  /// In pt, this message translates to:
  /// **'Arquivada'**
  String get goalInactiveBadge;

  /// No description provided for @goalReactivate.
  ///
  /// In pt, this message translates to:
  /// **'Reativar'**
  String get goalReactivate;

  /// No description provided for @goalReactivatedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Meta reativada.'**
  String get goalReactivatedSnackbar;

  /// No description provided for @goalLinearPaceCardTitle.
  ///
  /// In pt, this message translates to:
  /// **'Ritmo e previsão'**
  String get goalLinearPaceCardTitle;

  /// No description provided for @goalLinearPaceAvgPerMonth.
  ///
  /// In pt, this message translates to:
  /// **'Ritmo médio até aqui: {amount}/mês'**
  String goalLinearPaceAvgPerMonth(String amount);

  /// No description provided for @goalLinearPaceEta.
  ///
  /// In pt, this message translates to:
  /// **'Se mantiveres este ritmo: objetivo ~até {monthYear}'**
  String goalLinearPaceEta(String monthYear);

  /// No description provided for @goalLinearPaceDisclaimer.
  ///
  /// In pt, this message translates to:
  /// **'Estimativa simples com base no que já poupaste e na data de criação da meta; o ritmo real pode variar.'**
  String get goalLinearPaceDisclaimer;

  /// No description provided for @goalLinearPaceListHint.
  ///
  /// In pt, this message translates to:
  /// **'~{amount}/mês · meta ~{monthYear}'**
  String goalLinearPaceListHint(String amount, String monthYear);

  /// No description provided for @goalLinearPaceInsufficientHistory.
  ///
  /// In pt, this message translates to:
  /// **'Ainda não há histórico suficiente para estimar o ritmo.'**
  String get goalLinearPaceInsufficientHistory;

  /// No description provided for @expensePayConfirmTitle.
  ///
  /// In pt, this message translates to:
  /// **'Marcar como paga?'**
  String get expensePayConfirmTitle;

  /// No description provided for @expensePayConflict.
  ///
  /// In pt, this message translates to:
  /// **'Esta despesa já não pode ser quitada (por exemplo, já está paga).'**
  String get expensePayConflict;

  /// No description provided for @expensePayInstallmentLine.
  ///
  /// In pt, this message translates to:
  /// **'Parcela {current} de {total}'**
  String expensePayInstallmentLine(int current, int total);

  /// No description provided for @expensesTitle.
  ///
  /// In pt, this message translates to:
  /// **'Despesas'**
  String get expensesTitle;

  /// No description provided for @expensesRefresh.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar'**
  String get expensesRefresh;

  /// No description provided for @expensesNew.
  ///
  /// In pt, this message translates to:
  /// **'Nova'**
  String get expensesNew;

  /// No description provided for @expensesNewLong.
  ///
  /// In pt, this message translates to:
  /// **'Nova despesa'**
  String get expensesNewLong;

  /// No description provided for @expensesRefreshList.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar lista'**
  String get expensesRefreshList;

  /// No description provided for @expensesFilterAll.
  ///
  /// In pt, this message translates to:
  /// **'Todas'**
  String get expensesFilterAll;

  /// No description provided for @expensesFilterPending.
  ///
  /// In pt, this message translates to:
  /// **'Pendentes'**
  String get expensesFilterPending;

  /// No description provided for @expensesFilterPaid.
  ///
  /// In pt, this message translates to:
  /// **'Pagas'**
  String get expensesFilterPaid;

  /// No description provided for @expensesLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar.'**
  String get expensesLoadError;

  /// No description provided for @expensesEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Nenhuma despesa neste filtro.'**
  String get expensesEmpty;

  /// No description provided for @expensePay.
  ///
  /// In pt, this message translates to:
  /// **'Pagar'**
  String get expensePay;

  /// No description provided for @expenseMarkedPaid.
  ///
  /// In pt, this message translates to:
  /// **'Despesa marcada como paga.'**
  String get expenseMarkedPaid;

  /// No description provided for @expensePayError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao pagar.'**
  String get expensePayError;

  /// No description provided for @expenseTileFamilyCategory.
  ///
  /// In pt, this message translates to:
  /// **'{category} · Família'**
  String expenseTileFamilyCategory(String category);

  /// No description provided for @expenseTileDateLine.
  ///
  /// In pt, this message translates to:
  /// **'Data {date} · {status}'**
  String expenseTileDateLine(String date, String status);

  /// No description provided for @expenseStatusPending.
  ///
  /// In pt, this message translates to:
  /// **'Pendente'**
  String get expenseStatusPending;

  /// No description provided for @expenseStatusPaid.
  ///
  /// In pt, this message translates to:
  /// **'Paga'**
  String get expenseStatusPaid;

  /// No description provided for @expenseInstallmentChip.
  ///
  /// In pt, this message translates to:
  /// **'Parcela {n}/{total}'**
  String expenseInstallmentChip(int n, int total);

  /// No description provided for @expenseRecurringMonthly.
  ///
  /// In pt, this message translates to:
  /// **'Recorrente · mensal'**
  String get expenseRecurringMonthly;

  /// No description provided for @expenseRecurringWeekly.
  ///
  /// In pt, this message translates to:
  /// **'Recorrente · semanal'**
  String get expenseRecurringWeekly;

  /// No description provided for @expenseRecurringYearly.
  ///
  /// In pt, this message translates to:
  /// **'Recorrente · anual'**
  String get expenseRecurringYearly;

  /// No description provided for @expenseSharedWith.
  ///
  /// In pt, this message translates to:
  /// **'Partilhada · {name}'**
  String expenseSharedWith(String name);

  /// No description provided for @expenseShared.
  ///
  /// In pt, this message translates to:
  /// **'Partilhada'**
  String get expenseShared;

  /// No description provided for @expenseTitle.
  ///
  /// In pt, this message translates to:
  /// **'Despesa'**
  String get expenseTitle;

  /// No description provided for @expenseLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar.'**
  String get expenseLoadError;

  /// No description provided for @expenseCompetence.
  ///
  /// In pt, this message translates to:
  /// **'Data (competência)'**
  String get expenseCompetence;

  /// No description provided for @expenseDue.
  ///
  /// In pt, this message translates to:
  /// **'Vencimento'**
  String get expenseDue;

  /// No description provided for @expenseStatusLabel.
  ///
  /// In pt, this message translates to:
  /// **'Estado'**
  String get expenseStatusLabel;

  /// No description provided for @expenseInstallmentsRow.
  ///
  /// In pt, this message translates to:
  /// **'Parcelas'**
  String get expenseInstallmentsRow;

  /// No description provided for @expenseRecurrence.
  ///
  /// In pt, this message translates to:
  /// **'Recorrência'**
  String get expenseRecurrence;

  /// No description provided for @expenseShare.
  ///
  /// In pt, this message translates to:
  /// **'Partilha'**
  String get expenseShare;

  /// No description provided for @expenseShareWith.
  ///
  /// In pt, this message translates to:
  /// **'Com {name}'**
  String expenseShareWith(String name);

  /// No description provided for @expenseShareFamily.
  ///
  /// In pt, this message translates to:
  /// **'Família'**
  String get expenseShareFamily;

  /// No description provided for @expenseMarkPaid.
  ///
  /// In pt, this message translates to:
  /// **'Marcar como paga'**
  String get expenseMarkPaid;

  /// No description provided for @expenseEdit.
  ///
  /// In pt, this message translates to:
  /// **'Editar'**
  String get expenseEdit;

  /// No description provided for @expenseDelete.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar'**
  String get expenseDelete;

  /// No description provided for @expenseReadOnlyBanner.
  ///
  /// In pt, this message translates to:
  /// **'Despesa de outro membro da família — só podes ver.'**
  String get expenseReadOnlyBanner;

  /// No description provided for @expenseDeleteError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao eliminar.'**
  String get expenseDeleteError;

  /// No description provided for @expDelInstallmentTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar plano parcelado'**
  String get expDelInstallmentTitle;

  /// No description provided for @expDelInstallmentPaidBody.
  ///
  /// In pt, this message translates to:
  /// **'Este plano tem parcelas já pagas. Queres apagar também o histórico dessas parcelas, ou só as parcelas futuras pendentes?'**
  String get expDelInstallmentPaidBody;

  /// No description provided for @expDelInstallmentMaybePaidBody.
  ///
  /// In pt, this message translates to:
  /// **'Se já existirem parcelas pagas, podes manter o histórico e apagar só as futuras pendentes, ou eliminar o plano inteiro.'**
  String get expDelInstallmentMaybePaidBody;

  /// No description provided for @expDelFutureOnly.
  ///
  /// In pt, this message translates to:
  /// **'Só futuras pendentes'**
  String get expDelFutureOnly;

  /// No description provided for @expDelAllIncludingPaid.
  ///
  /// In pt, this message translates to:
  /// **'Tudo (inclui pagas)'**
  String get expDelAllIncludingPaid;

  /// No description provided for @expDelInstallmentSimpleTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar plano parcelado?'**
  String get expDelInstallmentSimpleTitle;

  /// No description provided for @expDelInstallmentSimpleBody.
  ///
  /// In pt, this message translates to:
  /// **'Todas as {total} parcelas serão eliminadas.'**
  String expDelInstallmentSimpleBody(int total);

  /// No description provided for @expDelRecurringTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar recorrência'**
  String get expDelRecurringTitle;

  /// No description provided for @expDelRecurringBody.
  ///
  /// In pt, this message translates to:
  /// **'Podes cancelar só as ocorrências futuras pendentes (após hoje), ou encerrar a série: remove todas as pendentes e desliga as já pagas do histórico da recorrência (mantêm-se como despesas normais).'**
  String get expDelRecurringBody;

  /// No description provided for @expDelCloseSeries.
  ///
  /// In pt, this message translates to:
  /// **'Encerrar série'**
  String get expDelCloseSeries;

  /// No description provided for @expDelRecurringOccurrenceTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar despesa recorrente'**
  String get expDelRecurringOccurrenceTitle;

  /// No description provided for @expDelRecurringOccurrenceBody.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar só esta competência ou aplicar à série inteira?'**
  String get expDelRecurringOccurrenceBody;

  /// No description provided for @expDelThisOnly.
  ///
  /// In pt, this message translates to:
  /// **'Só esta'**
  String get expDelThisOnly;

  /// No description provided for @expDelWholeSeries.
  ///
  /// In pt, this message translates to:
  /// **'Toda a série'**
  String get expDelWholeSeries;

  /// No description provided for @expDelRemoveFromRecurrenceTitle.
  ///
  /// In pt, this message translates to:
  /// **'Remover da recorrência'**
  String get expDelRemoveFromRecurrenceTitle;

  /// No description provided for @expDelRemoveOccurrenceTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar ocorrência?'**
  String get expDelRemoveOccurrenceTitle;

  /// No description provided for @expDelPaidUnlinkBody.
  ///
  /// In pt, this message translates to:
  /// **'Esta despesa já foi paga: não será apagada do histórico, apenas deixa de fazer parte da série recorrente.'**
  String get expDelPaidUnlinkBody;

  /// No description provided for @expDelPendingDeleteBody.
  ///
  /// In pt, this message translates to:
  /// **'Esta linha pendente será eliminada; as restantes da série mantêm-se.'**
  String get expDelPendingDeleteBody;

  /// No description provided for @expDelSeriesScopeTitle.
  ///
  /// In pt, this message translates to:
  /// **'Âmbito da série'**
  String get expDelSeriesScopeTitle;

  /// No description provided for @expDelSeriesScopeBody.
  ///
  /// In pt, this message translates to:
  /// **'Cancelar só ocorrências futuras pendentes (após hoje), ou encerrar toda a série (pendentes + desligar pagas do histórico da recorrência)?'**
  String get expDelSeriesScopeBody;

  /// No description provided for @expDelSingleTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar despesa?'**
  String get expDelSingleTitle;

  /// No description provided for @expDelSingleBody.
  ///
  /// In pt, this message translates to:
  /// **'Esta ação não pode ser anulada.'**
  String get expDelSingleBody;

  /// No description provided for @expDelSuccessInstallmentFuture.
  ///
  /// In pt, this message translates to:
  /// **'Parcelas futuras canceladas.'**
  String get expDelSuccessInstallmentFuture;

  /// No description provided for @expDelSuccessInstallmentAll.
  ///
  /// In pt, this message translates to:
  /// **'Plano eliminado.'**
  String get expDelSuccessInstallmentAll;

  /// No description provided for @expDelSuccessOccurrence.
  ///
  /// In pt, this message translates to:
  /// **'Ocorrência eliminada.'**
  String get expDelSuccessOccurrence;

  /// No description provided for @expDelSuccessOccurrenceUnlink.
  ///
  /// In pt, this message translates to:
  /// **'Removida da recorrência.'**
  String get expDelSuccessOccurrenceUnlink;

  /// No description provided for @expDelSuccessRecurringFuture.
  ///
  /// In pt, this message translates to:
  /// **'Futuras pendentes canceladas.'**
  String get expDelSuccessRecurringFuture;

  /// No description provided for @expDelSuccessRecurringClose.
  ///
  /// In pt, this message translates to:
  /// **'Recorrência encerrada.'**
  String get expDelSuccessRecurringClose;

  /// No description provided for @expDelSuccessSingle.
  ///
  /// In pt, this message translates to:
  /// **'Despesa eliminada.'**
  String get expDelSuccessSingle;

  /// No description provided for @newExpenseTitle.
  ///
  /// In pt, this message translates to:
  /// **'Nova despesa'**
  String get newExpenseTitle;

  /// No description provided for @editExpenseTitle.
  ///
  /// In pt, this message translates to:
  /// **'Editar despesa'**
  String get editExpenseTitle;

  /// No description provided for @expenseSaveError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao salvar.'**
  String get expenseSaveError;

  /// No description provided for @expenseChangesSaved.
  ///
  /// In pt, this message translates to:
  /// **'Alterações salvas.'**
  String get expenseChangesSaved;

  /// No description provided for @expenseEditOwnerOnly.
  ///
  /// In pt, this message translates to:
  /// **'Só o titular desta despesa a pode editar.'**
  String get expenseEditOwnerOnly;

  /// No description provided for @incomesTitle.
  ///
  /// In pt, this message translates to:
  /// **'Proventos'**
  String get incomesTitle;

  /// No description provided for @newIncomeTitle.
  ///
  /// In pt, this message translates to:
  /// **'Novo provento'**
  String get newIncomeTitle;

  /// No description provided for @editIncomeTitle.
  ///
  /// In pt, this message translates to:
  /// **'Editar provento'**
  String get editIncomeTitle;

  /// No description provided for @incomeEditOwnerOnly.
  ///
  /// In pt, this message translates to:
  /// **'Só o titular deste provento o pode editar.'**
  String get incomeEditOwnerOnly;

  /// No description provided for @incomeSaveError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao salvar.'**
  String get incomeSaveError;

  /// No description provided for @incomeSaved.
  ///
  /// In pt, this message translates to:
  /// **'Salvo.'**
  String get incomeSaved;

  /// No description provided for @incomeDetailTitle.
  ///
  /// In pt, this message translates to:
  /// **'Provento'**
  String get incomeDetailTitle;

  /// No description provided for @incomesAddLong.
  ///
  /// In pt, this message translates to:
  /// **'Adicionar provento'**
  String get incomesAddLong;

  /// No description provided for @incomesRefresh.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar'**
  String get incomesRefresh;

  /// No description provided for @incomesRefreshList.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar lista'**
  String get incomesRefreshList;

  /// No description provided for @incomesLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar.'**
  String get incomesLoadError;

  /// No description provided for @incomesEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Nenhum provento neste mês.'**
  String get incomesEmpty;

  /// No description provided for @incomesListHint.
  ///
  /// In pt, this message translates to:
  /// **'Valores em reais com centavos exactos; competência pelo dia do provento.'**
  String get incomesListHint;

  /// No description provided for @incomeTileDateLine.
  ///
  /// In pt, this message translates to:
  /// **'Data {date}'**
  String incomeTileDateLine(String date);

  /// No description provided for @familyTitle.
  ///
  /// In pt, this message translates to:
  /// **'Família'**
  String get familyTitle;

  /// No description provided for @securityTitle.
  ///
  /// In pt, this message translates to:
  /// **'Segurança'**
  String get securityTitle;

  /// No description provided for @unlockTitle.
  ///
  /// In pt, this message translates to:
  /// **'Desbloquear'**
  String get unlockTitle;

  /// No description provided for @resetPasswordTitle.
  ///
  /// In pt, this message translates to:
  /// **'Redefinir senha'**
  String get resetPasswordTitle;

  /// No description provided for @categoryLabel.
  ///
  /// In pt, this message translates to:
  /// **'Categoria'**
  String get categoryLabel;

  /// No description provided for @noneDash.
  ///
  /// In pt, this message translates to:
  /// **'—'**
  String get noneDash;

  /// No description provided for @secAppTitle.
  ///
  /// In pt, this message translates to:
  /// **'Segurança da app'**
  String get secAppTitle;

  /// No description provided for @secSetPinTitle.
  ///
  /// In pt, this message translates to:
  /// **'Definir PIN da app'**
  String get secSetPinTitle;

  /// No description provided for @secNewPinTitle.
  ///
  /// In pt, this message translates to:
  /// **'Novo PIN'**
  String get secNewPinTitle;

  /// No description provided for @secPinField.
  ///
  /// In pt, this message translates to:
  /// **'PIN (4–6 dígitos)'**
  String get secPinField;

  /// No description provided for @secRepeatPinField.
  ///
  /// In pt, this message translates to:
  /// **'Repetir PIN'**
  String get secRepeatPinField;

  /// No description provided for @secPinInvalidOrMismatch.
  ///
  /// In pt, this message translates to:
  /// **'PINs inválidos ou não coincidem.'**
  String get secPinInvalidOrMismatch;

  /// No description provided for @secPinSavedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'PIN da app salvo.'**
  String get secPinSavedSnackbar;

  /// No description provided for @secDisablePinTitle.
  ///
  /// In pt, this message translates to:
  /// **'Desativar PIN'**
  String get secDisablePinTitle;

  /// No description provided for @secCurrentPinField.
  ///
  /// In pt, this message translates to:
  /// **'PIN actual'**
  String get secCurrentPinField;

  /// No description provided for @secWrongPin.
  ///
  /// In pt, this message translates to:
  /// **'PIN incorreto.'**
  String get secWrongPin;

  /// No description provided for @secPinDisabledSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Bloqueio por PIN desativado.'**
  String get secPinDisabledSnackbar;

  /// No description provided for @secLockWithPin.
  ///
  /// In pt, this message translates to:
  /// **'Bloquear com PIN'**
  String get secLockWithPin;

  /// No description provided for @secLockPinOnSub.
  ///
  /// In pt, this message translates to:
  /// **'Ao minimizar, a app pede o PIN ao voltar.'**
  String get secLockPinOnSub;

  /// No description provided for @secLockPinOffSub.
  ///
  /// In pt, this message translates to:
  /// **'Desligado — só a sessão online (login) protege.'**
  String get secLockPinOffSub;

  /// No description provided for @secBiometricTitle.
  ///
  /// In pt, this message translates to:
  /// **'Desbloquear com rosto ou impressão digital'**
  String get secBiometricTitle;

  /// No description provided for @secBiometricOnSub.
  ///
  /// In pt, this message translates to:
  /// **'Usa Face ID, reconhecimento facial ou impressão digital do telemóvel — o sistema escolhe o método em que estás registado.'**
  String get secBiometricOnSub;

  /// No description provided for @secBiometricOffSub.
  ///
  /// In pt, this message translates to:
  /// **'Este dispositivo não expõe biometria ou rosto à app.'**
  String get secBiometricOffSub;

  /// No description provided for @secChangePin.
  ///
  /// In pt, this message translates to:
  /// **'Alterar PIN'**
  String get secChangePin;

  /// No description provided for @unlockIntro.
  ///
  /// In pt, this message translates to:
  /// **'Introduz o PIN da app para continuar.'**
  String get unlockIntro;

  /// No description provided for @unlockPinLabel.
  ///
  /// In pt, this message translates to:
  /// **'PIN'**
  String get unlockPinLabel;

  /// No description provided for @unlockUseBiometric.
  ///
  /// In pt, this message translates to:
  /// **'Usar biometria'**
  String get unlockUseBiometric;

  /// No description provided for @unlockUseFaceRecognition.
  ///
  /// In pt, this message translates to:
  /// **'Usar reconhecimento facial'**
  String get unlockUseFaceRecognition;

  /// No description provided for @unlockUseFingerprint.
  ///
  /// In pt, this message translates to:
  /// **'Usar impressão digital'**
  String get unlockUseFingerprint;

  /// No description provided for @unlockUseBiometricMixed.
  ///
  /// In pt, this message translates to:
  /// **'Usar rosto ou impressão digital'**
  String get unlockUseBiometricMixed;

  /// No description provided for @authResetSubtitle.
  ///
  /// In pt, this message translates to:
  /// **'Cola o código que recebeste por e-mail.'**
  String get authResetSubtitle;

  /// No description provided for @authResetTokenLabel.
  ///
  /// In pt, this message translates to:
  /// **'Código de recuperação'**
  String get authResetTokenLabel;

  /// No description provided for @authResetTokenHint.
  ///
  /// In pt, this message translates to:
  /// **'Cole o token completo'**
  String get authResetTokenHint;

  /// No description provided for @authResetTokenError.
  ///
  /// In pt, this message translates to:
  /// **'Cole o código recebido por e-mail'**
  String get authResetTokenError;

  /// No description provided for @authNewPassword.
  ///
  /// In pt, this message translates to:
  /// **'Nova senha'**
  String get authNewPassword;

  /// No description provided for @authConfirmNewPassword.
  ///
  /// In pt, this message translates to:
  /// **'Confirmar nova senha'**
  String get authConfirmNewPassword;

  /// No description provided for @authSaveNewPassword.
  ///
  /// In pt, this message translates to:
  /// **'Salvar nova senha'**
  String get authSaveNewPassword;

  /// No description provided for @authResetSuccess.
  ///
  /// In pt, this message translates to:
  /// **'Senha atualizada. Pode entrar.'**
  String get authResetSuccess;

  /// No description provided for @authResetError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao redefinir'**
  String get authResetError;

  /// No description provided for @authRequestNewCode.
  ///
  /// In pt, this message translates to:
  /// **'Pedir novo código'**
  String get authRequestNewCode;

  /// No description provided for @shareFamilyTitle.
  ///
  /// In pt, this message translates to:
  /// **'Partilhar na família'**
  String get shareFamilyTitle;

  /// No description provided for @shareFamilySubOn.
  ///
  /// In pt, this message translates to:
  /// **'Visível para o agregado; podes indicar com quem dividir.'**
  String get shareFamilySubOn;

  /// No description provided for @shareFamilySubOff.
  ///
  /// In pt, this message translates to:
  /// **'Junta-te a uma família (convite) para usar partilha.'**
  String get shareFamilySubOff;

  /// No description provided for @shareSplitWith.
  ///
  /// In pt, this message translates to:
  /// **'Dividir com'**
  String get shareSplitWith;

  /// No description provided for @shareWholeFamily.
  ///
  /// In pt, this message translates to:
  /// **'Toda a família'**
  String get shareWholeFamily;

  /// No description provided for @expFormDescription.
  ///
  /// In pt, this message translates to:
  /// **'Descrição'**
  String get expFormDescription;

  /// No description provided for @expFormAmountInstallment.
  ///
  /// In pt, this message translates to:
  /// **'Valor da parcela (R\$)'**
  String get expFormAmountInstallment;

  /// No description provided for @expFormAmount.
  ///
  /// In pt, this message translates to:
  /// **'Valor (R\$)'**
  String get expFormAmount;

  /// No description provided for @expFormKindSingle.
  ///
  /// In pt, this message translates to:
  /// **'Única'**
  String get expFormKindSingle;

  /// No description provided for @expFormKindInstallments.
  ///
  /// In pt, this message translates to:
  /// **'Parcelas'**
  String get expFormKindInstallments;

  /// No description provided for @expFormKindRecurring.
  ///
  /// In pt, this message translates to:
  /// **'Recorrente'**
  String get expFormKindRecurring;

  /// No description provided for @expFormInstallmentsLabel.
  ///
  /// In pt, this message translates to:
  /// **'Parcelas (competência mensal)'**
  String get expFormInstallmentsLabel;

  /// No description provided for @expFormInstallmentsHint.
  ///
  /// In pt, this message translates to:
  /// **'1 a 24'**
  String get expFormInstallmentsHint;

  /// No description provided for @expFormInstallmentRangeError.
  ///
  /// In pt, this message translates to:
  /// **'Use de 1 a 24'**
  String get expFormInstallmentRangeError;

  /// No description provided for @expFormInstallmentInvalid.
  ///
  /// In pt, this message translates to:
  /// **'Número inválido'**
  String get expFormInstallmentInvalid;

  /// No description provided for @expFormRecurringFrequency.
  ///
  /// In pt, this message translates to:
  /// **'Frequência'**
  String get expFormRecurringFrequency;

  /// No description provided for @expFormRecurringChoose.
  ///
  /// In pt, this message translates to:
  /// **'Escolher…'**
  String get expFormRecurringChoose;

  /// No description provided for @expFormRecurringHelp.
  ///
  /// In pt, this message translates to:
  /// **'Despesas fixas (água, luz, internet, assinaturas): o sistema gera linhas pendentes por mês ao consultares a lista. Alterar o valor na linha principal actualiza só ocorrências futuras pendentes; o histórico mantém os valores antigos.'**
  String get expFormRecurringHelp;

  /// No description provided for @expFormMarkPaid.
  ///
  /// In pt, this message translates to:
  /// **'Já está paga'**
  String get expFormMarkPaid;

  /// No description provided for @expFormHasDueDate.
  ///
  /// In pt, this message translates to:
  /// **'Tem data de vencimento'**
  String get expFormHasDueDate;

  /// No description provided for @expFormHasDueDateSub.
  ///
  /// In pt, this message translates to:
  /// **'Contas a pagar / alertas no dashboard'**
  String get expFormHasDueDateSub;

  /// No description provided for @expFormExpenseDate.
  ///
  /// In pt, this message translates to:
  /// **'Data da despesa (competência)'**
  String get expFormExpenseDate;

  /// No description provided for @expFormDueDate.
  ///
  /// In pt, this message translates to:
  /// **'Data de vencimento'**
  String get expFormDueDate;

  /// No description provided for @expFormChooseDate.
  ///
  /// In pt, this message translates to:
  /// **'Escolher…'**
  String get expFormChooseDate;

  /// No description provided for @expFormCategoriesLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar categorias.'**
  String get expFormCategoriesLoadError;

  /// No description provided for @expFormPickCategory.
  ///
  /// In pt, this message translates to:
  /// **'Escolhe uma categoria.'**
  String get expFormPickCategory;

  /// No description provided for @expFormCreateError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao criar.'**
  String get expFormCreateError;

  /// No description provided for @expFormPlanCreated.
  ///
  /// In pt, this message translates to:
  /// **'Plano: {count} parcelas criadas.'**
  String expFormPlanCreated(int count);

  /// No description provided for @expFormPlanCreatedRef.
  ///
  /// In pt, this message translates to:
  /// **'Plano: {count} parcelas (ref. {ref}).'**
  String expFormPlanCreatedRef(int count, String ref);

  /// No description provided for @expFormCreated.
  ///
  /// In pt, this message translates to:
  /// **'Despesa criada.'**
  String get expFormCreated;

  /// No description provided for @expFormPickDue.
  ///
  /// In pt, this message translates to:
  /// **'Escolhe a data de vencimento.'**
  String get expFormPickDue;

  /// No description provided for @expFormRecurringFreqRequired.
  ///
  /// In pt, this message translates to:
  /// **'Escolhe a frequência da recorrência.'**
  String get expFormRecurringFreqRequired;

  /// No description provided for @expEditDescription.
  ///
  /// In pt, this message translates to:
  /// **'Descrição'**
  String get expEditDescription;

  /// No description provided for @expEditAmount.
  ///
  /// In pt, this message translates to:
  /// **'Valor (R\$)'**
  String get expEditAmount;

  /// No description provided for @expEditExpenseDate.
  ///
  /// In pt, this message translates to:
  /// **'Data da despesa'**
  String get expEditExpenseDate;

  /// No description provided for @expEditDueOptional.
  ///
  /// In pt, this message translates to:
  /// **'Vencimento (opcional)'**
  String get expEditDueOptional;

  /// No description provided for @expEditState.
  ///
  /// In pt, this message translates to:
  /// **'Estado'**
  String get expEditState;

  /// No description provided for @expEditRecurrenceMeta.
  ///
  /// In pt, this message translates to:
  /// **'Recorrência (metadado)'**
  String get expEditRecurrenceMeta;

  /// No description provided for @expEditRecurrenceNone.
  ///
  /// In pt, this message translates to:
  /// **'Nenhuma'**
  String get expEditRecurrenceNone;

  /// No description provided for @expEditInstallmentBanner.
  ///
  /// In pt, this message translates to:
  /// **'Parcela {n} de {total}. Alterações aplicam-se só a esta linha.'**
  String expEditInstallmentBanner(int n, int total);

  /// No description provided for @expEditRecurringAnchorBanner.
  ///
  /// In pt, this message translates to:
  /// **'Recorrência: ao alterar o valor, só as ocorrências geradas futuras e pendentes (data após hoje) actualizam; meses já passados mantêm o valor histórico.'**
  String get expEditRecurringAnchorBanner;

  /// No description provided for @incFormDescription.
  ///
  /// In pt, this message translates to:
  /// **'Descrição'**
  String get incFormDescription;

  /// No description provided for @incFormAmount.
  ///
  /// In pt, this message translates to:
  /// **'Valor (R\$)'**
  String get incFormAmount;

  /// No description provided for @incFormIncomeDate.
  ///
  /// In pt, this message translates to:
  /// **'Data do provento'**
  String get incFormIncomeDate;

  /// No description provided for @incFormNotes.
  ///
  /// In pt, this message translates to:
  /// **'Notas (opcional)'**
  String get incFormNotes;

  /// No description provided for @incDetailLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro.'**
  String get incDetailLoadError;

  /// No description provided for @familyLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro.'**
  String get familyLoadError;

  /// No description provided for @familySaveError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao salvar.'**
  String get familySaveError;

  /// No description provided for @saveChanges.
  ///
  /// In pt, this message translates to:
  /// **'Salvar alterações'**
  String get saveChanges;

  /// No description provided for @expFormAmountHint.
  ///
  /// In pt, this message translates to:
  /// **'ex. 12,50'**
  String get expFormAmountHint;

  /// No description provided for @expFormInstallmentNeedAmountLine.
  ///
  /// In pt, this message translates to:
  /// **'Informe o valor de cada parcela. Total = parcela × {count}.'**
  String expFormInstallmentNeedAmountLine(int count);

  /// No description provided for @expFormInstallmentPlanTotalLine.
  ///
  /// In pt, this message translates to:
  /// **'Total do plano: {total} ({count} × {installment}).'**
  String expFormInstallmentPlanTotalLine(
    String total,
    int count,
    String installment,
  );

  /// No description provided for @expFormFreqMonthly.
  ///
  /// In pt, this message translates to:
  /// **'Mensal'**
  String get expFormFreqMonthly;

  /// No description provided for @expFormFreqWeekly.
  ///
  /// In pt, this message translates to:
  /// **'Semanal'**
  String get expFormFreqWeekly;

  /// No description provided for @expFormFreqYearly.
  ///
  /// In pt, this message translates to:
  /// **'Anual'**
  String get expFormFreqYearly;

  /// No description provided for @incFormIntro.
  ///
  /// In pt, this message translates to:
  /// **'Regista entradas reais (salário, extras, etc.). O saldo do mês no dashboard usa a soma dos proventos deste período.'**
  String get incFormIntro;

  /// No description provided for @incFormDescHint.
  ///
  /// In pt, this message translates to:
  /// **'ex. Salário abril, Honorários cliente X'**
  String get incFormDescHint;

  /// No description provided for @incFormAmountHint.
  ///
  /// In pt, this message translates to:
  /// **'ex. 3.500,00'**
  String get incFormAmountHint;

  /// No description provided for @incFormIncomeDateCompetence.
  ///
  /// In pt, this message translates to:
  /// **'Data do provento (competência)'**
  String get incFormIncomeDateCompetence;

  /// No description provided for @incFormPickCategory.
  ///
  /// In pt, this message translates to:
  /// **'Escolhe o tipo de provento.'**
  String get incFormPickCategory;

  /// No description provided for @incFormCreatedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Provento registado.'**
  String get incFormCreatedSnackbar;

  /// No description provided for @incFormSaveButton.
  ///
  /// In pt, this message translates to:
  /// **'Salvar provento'**
  String get incFormSaveButton;

  /// No description provided for @incFormCategoriesLoadError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar tipos.'**
  String get incFormCategoriesLoadError;

  /// No description provided for @incFormNotesHint.
  ///
  /// In pt, this message translates to:
  /// **'Origem, referência, NIF… — só para o teu agregado'**
  String get incFormNotesHint;

  /// No description provided for @incFormCategoryLabel.
  ///
  /// In pt, this message translates to:
  /// **'Tipo de provento'**
  String get incFormCategoryLabel;

  /// No description provided for @incomeDeleteTitle.
  ///
  /// In pt, this message translates to:
  /// **'Eliminar provento?'**
  String get incomeDeleteTitle;

  /// No description provided for @incomeDeletedSnackbar.
  ///
  /// In pt, this message translates to:
  /// **'Eliminado.'**
  String get incomeDeletedSnackbar;

  /// No description provided for @incomeReadOnlyBanner.
  ///
  /// In pt, this message translates to:
  /// **'Provento de outro membro da família — só podes ver.'**
  String get incomeReadOnlyBanner;

  /// No description provided for @incomeDetailTypeLabel.
  ///
  /// In pt, this message translates to:
  /// **'Tipo'**
  String get incomeDetailTypeLabel;

  /// No description provided for @incomeDetailDateCompetenceLabel.
  ///
  /// In pt, this message translates to:
  /// **'Data (competência)'**
  String get incomeDetailDateCompetenceLabel;

  /// No description provided for @incomeDetailNotesLabel.
  ///
  /// In pt, this message translates to:
  /// **'Notas'**
  String get incomeDetailNotesLabel;

  /// No description provided for @incomeDeleteError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao eliminar.'**
  String get incomeDeleteError;

  /// No description provided for @unlockPinTooShort.
  ///
  /// In pt, this message translates to:
  /// **'PIN com pelo menos 4 dígitos'**
  String get unlockPinTooShort;

  /// No description provided for @unlockBioReason.
  ///
  /// In pt, this message translates to:
  /// **'Confirma a tua identidade para desbloquear o Well Paid'**
  String get unlockBioReason;

  /// No description provided for @unlockBioUnavailable.
  ///
  /// In pt, this message translates to:
  /// **'Biometria indisponível'**
  String get unlockBioUnavailable;

  /// No description provided for @famLeaveTitle.
  ///
  /// In pt, this message translates to:
  /// **'Sair da família'**
  String get famLeaveTitle;

  /// No description provided for @famLeaveBody.
  ///
  /// In pt, this message translates to:
  /// **'Se fores o único membro, a família será eliminada. Se fores titular, a titularidade passa para outro membro.'**
  String get famLeaveBody;

  /// No description provided for @famExitAction.
  ///
  /// In pt, this message translates to:
  /// **'Sair'**
  String get famExitAction;

  /// No description provided for @famRemoveMemberTitle.
  ///
  /// In pt, this message translates to:
  /// **'Remover membro'**
  String get famRemoveMemberTitle;

  /// No description provided for @famRemoveMemberConfirm.
  ///
  /// In pt, this message translates to:
  /// **'Remover {email}?'**
  String famRemoveMemberConfirm(String email);

  /// No description provided for @famRenameTitle.
  ///
  /// In pt, this message translates to:
  /// **'Nome da família'**
  String get famRenameTitle;

  /// No description provided for @famNameField.
  ///
  /// In pt, this message translates to:
  /// **'Nome'**
  String get famNameField;

  /// No description provided for @famInviteTitle.
  ///
  /// In pt, this message translates to:
  /// **'Convite'**
  String get famInviteTitle;

  /// No description provided for @famInviteValidUntil.
  ///
  /// In pt, this message translates to:
  /// **'Válido até {date}'**
  String famInviteValidUntil(String date);

  /// No description provided for @famCopyTokenButton.
  ///
  /// In pt, this message translates to:
  /// **'Copiar token'**
  String get famCopyTokenButton;

  /// No description provided for @famNoFamilyIntro.
  ///
  /// In pt, this message translates to:
  /// **'Cria uma família ou entra com um convite (token ou QR).'**
  String get famNoFamilyIntro;

  /// No description provided for @famCreateNameOptional.
  ///
  /// In pt, this message translates to:
  /// **'Nome da família (opcional)'**
  String get famCreateNameOptional;

  /// No description provided for @famJoinSectionTitle.
  ///
  /// In pt, this message translates to:
  /// **'Entrar com convite'**
  String get famJoinSectionTitle;

  /// No description provided for @famJoinTokenField.
  ///
  /// In pt, this message translates to:
  /// **'Cole o token do convite'**
  String get famJoinTokenField;

  /// No description provided for @famCreate.
  ///
  /// In pt, this message translates to:
  /// **'Criar família'**
  String get famCreate;

  /// No description provided for @famJoin.
  ///
  /// In pt, this message translates to:
  /// **'Entrar na família'**
  String get famJoin;

  /// No description provided for @famInviteQr.
  ///
  /// In pt, this message translates to:
  /// **'Gerar convite (QR)'**
  String get famInviteQr;

  /// No description provided for @famLeave.
  ///
  /// In pt, this message translates to:
  /// **'Sair da família'**
  String get famLeave;

  /// No description provided for @famMembersSection.
  ///
  /// In pt, this message translates to:
  /// **'Membros'**
  String get famMembersSection;

  /// No description provided for @famMemberCount.
  ///
  /// In pt, this message translates to:
  /// **'{current} / {max} membros'**
  String famMemberCount(int current, int max);

  /// No description provided for @famRoleOwner.
  ///
  /// In pt, this message translates to:
  /// **'Titular'**
  String get famRoleOwner;

  /// No description provided for @famRoleMember.
  ///
  /// In pt, this message translates to:
  /// **'Membro'**
  String get famRoleMember;

  /// No description provided for @famYouSuffix.
  ///
  /// In pt, this message translates to:
  /// **' (tu)'**
  String get famYouSuffix;

  /// No description provided for @famEditNameTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Editar nome'**
  String get famEditNameTooltip;

  /// No description provided for @dashCashflowTitle.
  ///
  /// In pt, this message translates to:
  /// **'Histórico mensal'**
  String get dashCashflowTitle;

  /// No description provided for @dashCashflowChartOptions.
  ///
  /// In pt, this message translates to:
  /// **'Período e previsão'**
  String get dashCashflowChartOptions;

  /// No description provided for @dashCashflowDynamicMode.
  ///
  /// In pt, this message translates to:
  /// **'Modo dinâmico'**
  String get dashCashflowDynamicMode;

  /// No description provided for @dashCashflowStartMonth.
  ///
  /// In pt, this message translates to:
  /// **'Início'**
  String get dashCashflowStartMonth;

  /// No description provided for @dashCashflowEndMonth.
  ///
  /// In pt, this message translates to:
  /// **'Fim'**
  String get dashCashflowEndMonth;

  /// No description provided for @dashCashflowForecastMonths.
  ///
  /// In pt, this message translates to:
  /// **'Meses de previsão'**
  String get dashCashflowForecastMonths;

  /// No description provided for @dashCashflowApply.
  ///
  /// In pt, this message translates to:
  /// **'Aplicar'**
  String get dashCashflowApply;

  /// No description provided for @dashCashflowLegendIncome.
  ///
  /// In pt, this message translates to:
  /// **'Proventos'**
  String get dashCashflowLegendIncome;

  /// No description provided for @dashCashflowLegendExpensePaid.
  ///
  /// In pt, this message translates to:
  /// **'Despesas pagas'**
  String get dashCashflowLegendExpensePaid;

  /// No description provided for @dashCashflowLegendExpenseForecast.
  ///
  /// In pt, this message translates to:
  /// **'Despesas previstas'**
  String get dashCashflowLegendExpenseForecast;

  /// No description provided for @dashCashflowEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Sem dados para este período.'**
  String get dashCashflowEmpty;

  /// No description provided for @dashCashflowTouchChartHint.
  ///
  /// In pt, this message translates to:
  /// **'Toque no gráfico para outro mês. Por defeito: mês com mais movimento.'**
  String get dashCashflowTouchChartHint;

  /// No description provided for @dashCashflowInsightPeakPaid.
  ///
  /// In pt, this message translates to:
  /// **'Pico de despesas pagas: {month} · {amount}'**
  String dashCashflowInsightPeakPaid(String month, String amount);

  /// No description provided for @dashCashflowError.
  ///
  /// In pt, this message translates to:
  /// **'Erro ao carregar o histórico mensal.'**
  String get dashCashflowError;

  /// No description provided for @dashCashflowFooterForecastTotal.
  ///
  /// In pt, this message translates to:
  /// **'Total previsto: {amount}'**
  String dashCashflowFooterForecastTotal(String amount);

  /// No description provided for @dashCashflowFooterBalance.
  ///
  /// In pt, this message translates to:
  /// **'Saldo no período: {amount}'**
  String dashCashflowFooterBalance(String amount);

  /// No description provided for @dashCashflowSemantics.
  ///
  /// In pt, this message translates to:
  /// **'Gráfico de histórico mensal de proventos e despesas'**
  String get dashCashflowSemantics;

  /// No description provided for @dashCashflowLoadedPoints.
  ///
  /// In pt, this message translates to:
  /// **'{count} meses no período'**
  String dashCashflowLoadedPoints(int count);

  /// No description provided for @dashCashflowRangeInvalid.
  ///
  /// In pt, this message translates to:
  /// **'O mês inicial não pode ser depois do mês final.'**
  String get dashCashflowRangeInvalid;

  /// No description provided for @dashCashflowA11yPickStartMonth.
  ///
  /// In pt, this message translates to:
  /// **'Escolher mês inicial do intervalo'**
  String get dashCashflowA11yPickStartMonth;

  /// No description provided for @dashCashflowA11yPickEndMonth.
  ///
  /// In pt, this message translates to:
  /// **'Escolher mês final do intervalo'**
  String get dashCashflowA11yPickEndMonth;

  /// No description provided for @dashCashflowA11yForecastDropdown.
  ///
  /// In pt, this message translates to:
  /// **'Número de meses de previsão após o intervalo'**
  String get dashCashflowA11yForecastDropdown;

  /// No description provided for @dashCashflowA11yForecastDecrease.
  ///
  /// In pt, this message translates to:
  /// **'Menos meses de previsão no gráfico'**
  String get dashCashflowA11yForecastDecrease;

  /// No description provided for @dashCashflowA11yForecastIncrease.
  ///
  /// In pt, this message translates to:
  /// **'Mais meses de previsão no gráfico'**
  String get dashCashflowA11yForecastIncrease;

  /// No description provided for @dashCashflowA11yApply.
  ///
  /// In pt, this message translates to:
  /// **'Aplicar filtros ao histórico mensal'**
  String get dashCashflowA11yApply;

  /// No description provided for @dashCashflowA11ySeriesToggle.
  ///
  /// In pt, this message translates to:
  /// **'{name}. Toque para mostrar ou ocultar no gráfico.'**
  String dashCashflowA11ySeriesToggle(String name);

  /// No description provided for @dashCashflowA11ySummary.
  ///
  /// In pt, this message translates to:
  /// **'Totais do período. Previsto: {forecast}. Saldo: {balance}.'**
  String dashCashflowA11ySummary(String forecast, String balance);

  /// No description provided for @dashCashflowDynamicWindowTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Ligado: o gráfico usa a janela dinâmica do servidor (últimos meses, independente só do mês do topo). Desligado: 6 meses fixos — do mês do topo voltando 5 meses. À direita: quantos meses futuros mostrar com despesas previstas (funciona nos dois modos).'**
  String get dashCashflowDynamicWindowTooltip;

  /// No description provided for @dashCashflowBarRollingLabel.
  ///
  /// In pt, this message translates to:
  /// **'Auto'**
  String get dashCashflowBarRollingLabel;

  /// No description provided for @dashCashflowBarFixedLabel.
  ///
  /// In pt, this message translates to:
  /// **'Topo'**
  String get dashCashflowBarFixedLabel;

  /// No description provided for @dashCashflowForecastBarShort.
  ///
  /// In pt, this message translates to:
  /// **'Futuro'**
  String get dashCashflowForecastBarShort;

  /// No description provided for @dashCashflowForecastBarTooltip.
  ///
  /// In pt, this message translates to:
  /// **'Meses futuros no eixo com despesas previstas (além do histórico carregado).'**
  String get dashCashflowForecastBarTooltip;

  /// No description provided for @dashCashflowInsightPeakIncome.
  ///
  /// In pt, this message translates to:
  /// **'Pico de proventos: {month} · {amount}'**
  String dashCashflowInsightPeakIncome(String month, String amount);

  /// No description provided for @dashHomeCategoriesFootnote.
  ///
  /// In pt, this message translates to:
  /// **'Totais por categoria são do mês escolhido no seletor acima.'**
  String get dashHomeCategoriesFootnote;

  /// No description provided for @shoppingListsTitle.
  ///
  /// In pt, this message translates to:
  /// **'Listas de compras'**
  String get shoppingListsTitle;

  /// No description provided for @shoppingListsMenuLabel.
  ///
  /// In pt, this message translates to:
  /// **'Listas de compras'**
  String get shoppingListsMenuLabel;

  /// No description provided for @shoppingListsActiveSection.
  ///
  /// In pt, this message translates to:
  /// **'Em planeamento'**
  String get shoppingListsActiveSection;

  /// No description provided for @shoppingListsHistorySection.
  ///
  /// In pt, this message translates to:
  /// **'Histórico'**
  String get shoppingListsHistorySection;

  /// No description provided for @shoppingListsEmpty.
  ///
  /// In pt, this message translates to:
  /// **'Ainda não tens listas. Cria uma para planear itens e preencher valores na loja.'**
  String get shoppingListsEmpty;

  /// No description provided for @shoppingListsNewList.
  ///
  /// In pt, this message translates to:
  /// **'Nova lista'**
  String get shoppingListsNewList;

  /// No description provided for @shoppingListUntitled.
  ///
  /// In pt, this message translates to:
  /// **'Lista sem título'**
  String get shoppingListUntitled;

  /// No description provided for @shoppingListItemsCount.
  ///
  /// In pt, this message translates to:
  /// **'{count} itens'**
  String shoppingListItemsCount(int count);

  /// No description provided for @shoppingListSubtotal.
  ///
  /// In pt, this message translates to:
  /// **'Subtotal: {amount}'**
  String shoppingListSubtotal(String amount);

  /// No description provided for @shoppingListNoPriceYet.
  ///
  /// In pt, this message translates to:
  /// **'Sem valor (preencher na loja)'**
  String get shoppingListNoPriceYet;

  /// No description provided for @shoppingListAddItem.
  ///
  /// In pt, this message translates to:
  /// **'Adicionar item'**
  String get shoppingListAddItem;

  /// No description provided for @shoppingListItemLabelHint.
  ///
  /// In pt, this message translates to:
  /// **'Descrição do item'**
  String get shoppingListItemLabelHint;

  /// No description provided for @shoppingListItemQuantity.
  ///
  /// In pt, this message translates to:
  /// **'Quantidade'**
  String get shoppingListItemQuantity;

  /// No description provided for @shoppingListItemAmountOptional.
  ///
  /// In pt, this message translates to:
  /// **'Valor unitário (opcional)'**
  String get shoppingListItemAmountOptional;

  /// No description provided for @shoppingListDeleteItem.
  ///
  /// In pt, this message translates to:
  /// **'Remover item'**
  String get shoppingListDeleteItem;

  /// No description provided for @shoppingListConfirmDeleteDraftTitle.
  ///
  /// In pt, this message translates to:
  /// **'Apagar lista?'**
  String get shoppingListConfirmDeleteDraftTitle;

  /// No description provided for @shoppingListConfirmDeleteDraftBody.
  ///
  /// In pt, this message translates to:
  /// **'Esta lista em rascunho será eliminada. Esta ação não pode ser desfeita.'**
  String get shoppingListConfirmDeleteDraftBody;

  /// No description provided for @shoppingListComplete.
  ///
  /// In pt, this message translates to:
  /// **'Fechar compra'**
  String get shoppingListComplete;

  /// No description provided for @shoppingListCompleteTitle.
  ///
  /// In pt, this message translates to:
  /// **'Lançar despesa'**
  String get shoppingListCompleteTitle;

  /// No description provided for @shoppingListTotalOverrideHint.
  ///
  /// In pt, this message translates to:
  /// **'Total pago na loja (opcional — substitui soma e desconto)'**
  String get shoppingListTotalOverrideHint;

  /// No description provided for @shoppingListDiscountHint.
  ///
  /// In pt, this message translates to:
  /// **'Desconto (opcional — subtrai da soma; incompatível com total manual)'**
  String get shoppingListDiscountHint;

  /// No description provided for @shoppingListDiscountOverrideConflict.
  ///
  /// In pt, this message translates to:
  /// **'Preenche só o total manual ou só o desconto, não ambos.'**
  String get shoppingListDiscountOverrideConflict;

  /// No description provided for @shoppingListTotalMismatchTitle.
  ///
  /// In pt, this message translates to:
  /// **'Total diferente da soma das linhas'**
  String get shoppingListTotalMismatchTitle;

  /// No description provided for @shoppingListTotalMismatchBody.
  ///
  /// In pt, this message translates to:
  /// **'Indicaste {manual} mas o subtotal das linhas é {subtotal}. A despesa usará o valor que indicaste. Continuar?'**
  String shoppingListTotalMismatchBody(String manual, String subtotal);

  /// No description provided for @shoppingListDescriptionOptional.
  ///
  /// In pt, this message translates to:
  /// **'Descrição da despesa (opcional)'**
  String get shoppingListDescriptionOptional;

  /// No description provided for @shoppingListMarkPaid.
  ///
  /// In pt, this message translates to:
  /// **'Já paguei'**
  String get shoppingListMarkPaid;

  /// No description provided for @shoppingListExpenseFromListPaidNote.
  ///
  /// In pt, this message translates to:
  /// **'A despesa fica como paga, sem parcelas nem recorrência. Na despesa aparecem só o nome da lista e o total.'**
  String get shoppingListExpenseFromListPaidNote;

  /// No description provided for @shoppingListExpenseDate.
  ///
  /// In pt, this message translates to:
  /// **'Data da despesa'**
  String get shoppingListExpenseDate;

  /// No description provided for @shoppingListViewExpense.
  ///
  /// In pt, this message translates to:
  /// **'Ver despesa'**
  String get shoppingListViewExpense;

  /// No description provided for @shoppingListAlignTotalToLinesButton.
  ///
  /// In pt, this message translates to:
  /// **'Atualizar despesa para a soma das linhas'**
  String get shoppingListAlignTotalToLinesButton;

  /// No description provided for @shoppingListAlignTotalSuccess.
  ///
  /// In pt, this message translates to:
  /// **'Total da despesa atualizado para coincidir com as linhas.'**
  String get shoppingListAlignTotalSuccess;

  /// No description provided for @shoppingListCompletedOn.
  ///
  /// In pt, this message translates to:
  /// **'Concluída em {date}'**
  String shoppingListCompletedOn(String date);

  /// No description provided for @shoppingListEditMetaTitle.
  ///
  /// In pt, this message translates to:
  /// **'Título e loja'**
  String get shoppingListEditMetaTitle;

  /// No description provided for @shoppingListTitleOptional.
  ///
  /// In pt, this message translates to:
  /// **'Título (opcional)'**
  String get shoppingListTitleOptional;

  /// No description provided for @shoppingListStoreOptional.
  ///
  /// In pt, this message translates to:
  /// **'Loja (opcional)'**
  String get shoppingListStoreOptional;

  /// No description provided for @shoppingListErrorLoad.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível carregar as listas.'**
  String get shoppingListErrorLoad;

  /// No description provided for @shoppingListReadOnlyDraft.
  ///
  /// In pt, this message translates to:
  /// **'Só o autor pode editar esta lista em rascunho.'**
  String get shoppingListReadOnlyDraft;

  /// No description provided for @shoppingListCompletedEditHint.
  ///
  /// In pt, this message translates to:
  /// **'Podes corrigir valores, quantidades e remover itens. A lista não pode ficar vazia.'**
  String get shoppingListCompletedEditHint;

  /// No description provided for @shoppingListNoItems.
  ///
  /// In pt, this message translates to:
  /// **'Sem itens. Adiciona produtos para planear a compra.'**
  String get shoppingListNoItems;

  /// No description provided for @shoppingListFooterEstimatedTotal.
  ///
  /// In pt, this message translates to:
  /// **'Total estimado'**
  String get shoppingListFooterEstimatedTotal;

  /// No description provided for @shoppingListFooterEstimatedNote.
  ///
  /// In pt, this message translates to:
  /// **'Soma de (valor unitário × quantidade) nas linhas com preço.'**
  String get shoppingListFooterEstimatedNote;

  /// No description provided for @shoppingListFooterUnitsSummary.
  ///
  /// In pt, this message translates to:
  /// **'{unitCount} itens no total'**
  String shoppingListFooterUnitsSummary(int unitCount);

  /// No description provided for @shoppingListFooterAddItemCompleted.
  ///
  /// In pt, this message translates to:
  /// **'Ainda podes acrescentar itens a esta lista concluída.'**
  String get shoppingListFooterAddItemCompleted;

  /// No description provided for @shoppingListInlineAmountHint.
  ///
  /// In pt, this message translates to:
  /// **'Unitário'**
  String get shoppingListInlineAmountHint;

  /// No description provided for @shoppingListEditLabelTitle.
  ///
  /// In pt, this message translates to:
  /// **'Nome do item'**
  String get shoppingListEditLabelTitle;

  /// No description provided for @shoppingListConfirmRemoveItemTitle.
  ///
  /// In pt, this message translates to:
  /// **'Remover item?'**
  String get shoppingListConfirmRemoveItemTitle;

  /// No description provided for @shoppingListConfirmRemoveItemBody.
  ///
  /// In pt, this message translates to:
  /// **'Este produto sai da lista.'**
  String get shoppingListConfirmRemoveItemBody;

  /// No description provided for @shoppingListEditItemTitle.
  ///
  /// In pt, this message translates to:
  /// **'Editar item'**
  String get shoppingListEditItemTitle;

  /// No description provided for @shoppingListCompleteSuccess.
  ///
  /// In pt, this message translates to:
  /// **'Despesa registada. A lista foi movida para o histórico.'**
  String get shoppingListCompleteSuccess;

  /// No description provided for @shoppingListFlushDraftError.
  ///
  /// In pt, this message translates to:
  /// **'Não foi possível sincronizar os itens. Verifica a ligação e tenta outra vez.'**
  String get shoppingListFlushDraftError;

  /// No description provided for @shoppingListCompleteInProgress.
  ///
  /// In pt, this message translates to:
  /// **'A sincronizar e a concluir…'**
  String get shoppingListCompleteInProgress;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'pt'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'pt':
      return AppLocalizationsPt();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
