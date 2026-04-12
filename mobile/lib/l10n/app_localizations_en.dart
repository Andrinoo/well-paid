// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Well Paid';

  @override
  String get settingsTitle => 'Settings';

  @override
  String get settingsLanguageTitle => 'Interface language';

  @override
  String get settingsLanguageSubtitle =>
      'App text in Brazilian Portuguese or US English.';

  @override
  String get langPortugueseBrazil => 'Portuguese (Brazil)';

  @override
  String get langEnglishUS => 'English (US)';

  @override
  String get settingsLanguageUpdated => 'Language updated.';

  @override
  String get save => 'Save';

  @override
  String get cancel => 'Cancel';

  @override
  String get ok => 'OK';

  @override
  String get copy => 'Copy';

  @override
  String get confirm => 'Confirm';

  @override
  String get delete => 'Delete';

  @override
  String get close => 'Close';

  @override
  String get errorGeneric => 'Error.';

  @override
  String get dioRequestTimeout =>
      'Request timed out. Check that the backend is running and the API URL is correct.';

  @override
  String get dioConnectionFailed =>
      'Could not reach the server. On Android emulator use 10.0.2.2; on Windows or iOS use 127.0.0.1 (--dart-define=API_BASE_URL=…).';

  @override
  String get dioNetworkFallback => 'Network error';

  @override
  String get tryAgain => 'Try again';

  @override
  String get requiredField => 'Required';

  @override
  String get valueInvalid => 'Invalid amount.';

  @override
  String get homeDashboardTitle => 'Dashboard';

  @override
  String get tooltipSettings => 'Settings';

  @override
  String get tooltipSecurity => 'Security';

  @override
  String get tooltipFamily => 'Family';

  @override
  String get tooltipRefreshDashboard => 'Refresh dashboard';

  @override
  String get tooltipLogout => 'Log out';

  @override
  String get navHome => 'Home';

  @override
  String get navExpenses => 'Expenses';

  @override
  String get navIncomes => 'Income';

  @override
  String get navGoals => 'Goals';

  @override
  String get navReserve => 'Reserve';

  @override
  String get navQuickPanelToggleHint =>
      'Pull up or tap for To pay and Shopping lists shortcuts';

  @override
  String get menuMoreTooltip => 'More options';

  @override
  String get logoutConfirmTitle => 'Log out?';

  @override
  String get logoutConfirmBody =>
      'You will need to sign in again to access your account.';

  @override
  String get homeQuickExpenses => 'Expenses';

  @override
  String get homeQuickIncomes => 'Income';

  @override
  String get homeQuickGoals => 'Goals';

  @override
  String get homeQuickReserve => 'Reserve';

  @override
  String get homeDashboardError => 'Could not load the dashboard.';

  @override
  String get authLoginTitle => 'Sign in';

  @override
  String get authEmail => 'Email';

  @override
  String get authPassword => 'Password';

  @override
  String get authShowPassword => 'Show password';

  @override
  String get authHidePassword => 'Hide password';

  @override
  String get authPasswordRequired => 'Enter your password';

  @override
  String get authRememberCredentials =>
      'Remember email and password on this device';

  @override
  String get authForgotPassword => 'Forgot password?';

  @override
  String get authEnter => 'Sign in';

  @override
  String get authLoginError => 'Could not sign in';

  @override
  String get authNoAccountYet => 'No account yet?';

  @override
  String get authCreateAccount => 'Create account';

  @override
  String get authCopyright =>
      'Copyright © 2026 Andrino Cabral. All rights reserved.';

  @override
  String get authRegisterTitle => 'Create account';

  @override
  String get authRegisterSubtitle => 'Get started in a few steps.';

  @override
  String get authRegisterError => 'Could not register';

  @override
  String get authPasswordPolicyHint =>
      'Min. 8 characters, 1 uppercase, 1 number, and 1 special character.';

  @override
  String get authPasswordRulesError => 'Password does not meet the rules';

  @override
  String get authNameOptional => 'Name (optional)';

  @override
  String get authPhoneOptional => 'Phone (optional)';

  @override
  String get authConfirmPassword => 'Confirm password';

  @override
  String get authConfirmPasswordRequired => 'Confirm your password';

  @override
  String get authPasswordMismatch => 'Passwords do not match';

  @override
  String get authRegisterButton => 'Register';

  @override
  String get authVerifyEmailTitle => 'Confirm email';

  @override
  String get authVerifyEmailSubtitle =>
      'Enter the 6-digit code we sent to your email.';

  @override
  String get authVerifyEmailFromLink => 'Confirming your email…';

  @override
  String get authVerificationCodeLabel => 'Code';

  @override
  String get authVerificationCodeHint => '000000';

  @override
  String get authVerificationCodeError => 'Enter all 6 digits';

  @override
  String get authVerifyEmailButton => 'Confirm and sign in';

  @override
  String get authResendVerification => 'Resend code';

  @override
  String get authVerifyEmailSuccess => 'Email confirmed. Welcome!';

  @override
  String get authVerifyEmailError => 'Could not confirm';

  @override
  String get authResendVerificationError => 'Could not resend';

  @override
  String get authVerifyEmailAction => 'Confirm email';

  @override
  String authDevVerificationHint(String token, String code) {
    return 'Link token:\n$token\n\nCode:\n$code';
  }

  @override
  String get authAlreadyHaveAccount => 'I already have an account — sign in';

  @override
  String get authEmailRequired => 'Enter your email';

  @override
  String get authEmailInvalid => 'Invalid email';

  @override
  String get authForgotTitle => 'Reset password';

  @override
  String get authForgotSubtitle => 'Enter your email to receive instructions.';

  @override
  String get authForgotBody =>
      'We will send a code to reset your password in the app. If you do not see the email, check spam.';

  @override
  String get authForgotSend => 'Send instructions';

  @override
  String get authForgotError => 'Could not send request';

  @override
  String get authBackToLogin => 'Back to sign in';

  @override
  String get authDevModeTitle => 'Development mode';

  @override
  String authDevTokenHint(String token) {
    return 'The backend returned a test token. Save it and use it on the next screen.\n\n$token';
  }

  @override
  String get tokenCopied => 'Token copied';

  @override
  String get dashMonthSummary => 'Month summary';

  @override
  String get dashIncome => 'Income';

  @override
  String get dashExpenses => 'Expenses';

  @override
  String get dashBalance => 'Balance';

  @override
  String get dashByCategory => 'Spending by category';

  @override
  String get dashHomeChartTabCategory => 'Categories';

  @override
  String get dashHomeChartTabCashflow => 'Cash flow';

  @override
  String get dashPendingThisMonthTitle => 'Bills to pay';

  @override
  String get dashPendingThisMonthSubtitle => 'Tap to review and settle';

  @override
  String get chartViewCategoryExpenses => 'View expenses in this category';

  @override
  String get chartRegisterExpenseCta => 'Add expense';

  @override
  String get expenseListFilteredByCategory => 'Filtered by category';

  @override
  String get expenseListClearCategoryFilter => 'Clear';

  @override
  String get dashToPay => 'To pay';

  @override
  String get toPayScreenSubtitle =>
      'All pending expenses in chronological order by due date. Installments appear as separate lines. Colors show how close the due date is.';

  @override
  String get toPayViewAllExpenses => 'All expenses';

  @override
  String toPayDueOn(String date) {
    return 'Due $date';
  }

  @override
  String toPayCompetenceOn(String date) {
    return 'Booked $date (no due date)';
  }

  @override
  String get toPayProjectedHint =>
      'Scheduled — will appear as a charge a few days before the due date.';

  @override
  String get toPayOfflineCacheBanner =>
      'Showing saved data — the list may be incomplete until you are back online.';

  @override
  String toPayPendingTotal(String amount) {
    return 'Pending total: $amount';
  }

  @override
  String get toPayFilterAll => 'All';

  @override
  String get toPayFilterOverdue => 'Overdue';

  @override
  String get toPayFilterThisWeek => 'This week';

  @override
  String get toPaySectionOverdue => 'Overdue';

  @override
  String get toPaySectionThisWeek => 'Due this week';

  @override
  String get toPaySectionLater => 'Later';

  @override
  String get toPayFilterEmpty => 'Nothing matches this filter.';

  @override
  String get toPayLegendTitle => 'Due date colors';

  @override
  String get toPayLegendOverdue => 'Past due date';

  @override
  String get toPayLegendDueToday => 'Due today';

  @override
  String get toPayLegendDueSoon => 'Due in 1–3 days';

  @override
  String get toPayLegendUpcoming => 'Due in 4–10 days';

  @override
  String get toPayLegendSafe => 'Due in 11 or more days';

  @override
  String get toPayQuickPaySwitchTooltip =>
      'Turn on to confirm payment; the list updates after success. If something fails, the switch turns off again.';

  @override
  String expenseListNextInstallmentLine(String date) {
    return 'Next: $date';
  }

  @override
  String get expenseTagPar => 'PAR';

  @override
  String get expenseTagParA11y => 'Installment plan';

  @override
  String get expenseTagRec => 'REC';

  @override
  String get expenseTagRecA11y => 'Recurring expense';

  @override
  String get expenseNextDueSectionTitle => 'Next occurrence';

  @override
  String expenseNextDueCompetenceLine(String date) {
    return 'Booking date: $date';
  }

  @override
  String expenseNextDueDateLine(String date) {
    return 'Due: $date';
  }

  @override
  String get expenseInstallmentNextSectionTitle => 'Next installment';

  @override
  String get expenseInstallmentLastSectionTitle => 'Last installment';

  @override
  String get expensePayEarlyTitle => 'Pay in advance?';

  @override
  String get expensePayEarlyBody =>
      'This expense is for a month after the current one. Are you sure you want to mark it as paid now?';

  @override
  String get dashNothingPending => 'Nothing pending.';

  @override
  String get dashPendingTotal => 'Total pending';

  @override
  String get dashSeeAll => 'See all';

  @override
  String get dashUpcomingDue => 'Upcoming due dates';

  @override
  String get dashNoUpcomingInNextMonth => 'No bills due in the next month.';

  @override
  String get dashSeeMore => 'See more';

  @override
  String get dashGoals => 'Goals';

  @override
  String get dashNoActiveGoals => 'No active goals right now.';

  @override
  String get dashSeeGoals => 'View goals';

  @override
  String get dashEmergencyReserve => 'Emergency reserve';

  @override
  String get dashEmergencyReserveBalance => 'Balance saved';

  @override
  String get dashEmergencyReserveMonthly => 'Monthly target';

  @override
  String get dashEmergencyReserveTimesTarget => 'target multiple';

  @override
  String dashEmergencyReserveAnnualProgress(int percent) {
    return '$percent% of yearly goal';
  }

  @override
  String get dashEmergencyReserveMomentum => 'Keep going';

  @override
  String get dashEmergencyReserveAnnualDone => 'Yearly goal reached';

  @override
  String get dashEmergencyReserveStageFirst => 'First step';

  @override
  String get dashEmergencyReserveStageStart => 'Good pace';

  @override
  String get dashEmergencyReserveStageMid => 'Halfway there';

  @override
  String get dashEmergencyReserveStageStrong => 'Final stretch';

  @override
  String get dashEmergencyReserveStageDone => 'Yearly goal reached';

  @override
  String get dashEmergencyReserveFootnote =>
      'Each calendar month adds your target to this balance (in-app record; not your bank balance).';

  @override
  String get dashEmergencyReserveConfigure => 'Configure';

  @override
  String get emergencyReserveTitle => 'Emergency reserve';

  @override
  String get emergencyReserveIntro =>
      'Set how much you plan to set aside per month. The balance increases automatically each calendar month from when you enable the target.';

  @override
  String get emergencyReserveMonthlyLabel => 'Monthly amount (R\$)';

  @override
  String get emergencyReserveQuickPickTitle => 'Quick picks';

  @override
  String get emergencyReserveSave => 'Save';

  @override
  String get emergencyReserveSavedSnackbar => 'Reserve target updated.';

  @override
  String get emergencyReserveError => 'Could not save.';

  @override
  String get emergencyReserveAccrualListTitle => 'Monthly credits';

  @override
  String get emergencyReserveAccrualListEmpty => 'No monthly credits yet.';

  @override
  String get emergencyReserveAccrualListCredit => 'Credit applied';

  @override
  String get emergencyReserveAccrualEdit => 'Edit credit';

  @override
  String get emergencyReserveAccrualDelete => 'Remove credit';

  @override
  String emergencyReserveAccrualDeleteTitle(String month) {
    return 'Remove credit for $month?';
  }

  @override
  String get emergencyReserveAccrualDeleteBody =>
      'The balance will be adjusted. This month will not get automatic credit again (you can add it back manually).';

  @override
  String get emergencyReserveAccrualDeleteConfirm => 'Remove';

  @override
  String emergencyReserveAccrualEditTitle(String month) {
    return 'Credit for $month';
  }

  @override
  String get emergencyReserveAccrualUpdatedSnackbar => 'Credit updated.';

  @override
  String get emergencyReserveAccrualRemovedSnackbar => 'Credit removed.';

  @override
  String get emergencyReserveResetAction => 'Clear reserve and history';

  @override
  String get emergencyReserveResetTitle => 'Clear everything?';

  @override
  String get emergencyReserveResetBody =>
      'Removes the target, balance, and all monthly credits. This cannot be undone.';

  @override
  String get emergencyReserveResetConfirm => 'Clear';

  @override
  String get emergencyReserveResetSuccess => 'Reserve cleared.';

  @override
  String get reserveMilestoneBannerQuarter =>
      'You’ve reached a quarter of your reserve’s yearly goal — nice pace.';

  @override
  String get reserveMilestoneBannerHalf =>
      'Halfway through your saving year for the reserve.';

  @override
  String get reserveMilestoneBannerAlmost =>
      'Almost there: just a little left to hit the reserve’s yearly goal.';

  @override
  String get reserveMilestoneBannerComplete =>
      'Yearly reserve goal reached. Strong consistency.';

  @override
  String get settingsEmergencyReserve => 'Emergency reserve';

  @override
  String get settingsNotificationsSection => 'Notifications';

  @override
  String get settingsGoalStallReminderTitle => 'Stalled goals reminder';

  @override
  String get settingsGoalStallReminderSubtitle =>
      'A local notification if you have not updated a goal for about 3 weeks (based on last update time).';

  @override
  String get settingsGoalStallPermissionDenied =>
      'Enable notifications in system settings to receive the reminder.';

  @override
  String get goalStallNotifTitle => 'Goals without recent activity';

  @override
  String goalStallNotifBodySingle(String name) {
    return '“$name” — it’s been a while without an update. Time to check in?';
  }

  @override
  String goalStallNotifBodyTwo(String a, String b) {
    return '“$a” and “$b” — goals without updates for a while.';
  }

  @override
  String goalStallNotifBodyMany(String first, int count) {
    return '“$first” and $count more — don’t forget your goals.';
  }

  @override
  String get dashMarkPaidTooltip => 'Mark as paid';

  @override
  String get dashDueShort => 'Due';

  @override
  String get dashDueVerb => 'Due';

  @override
  String get dashFamilySuffix => ' · Family';

  @override
  String get dashGoalFamilySuffix => ' (family)';

  @override
  String dashPendingItemA11y(String description, String amount, String due) {
    return '$description, $amount, due $due';
  }

  @override
  String periodSummaryA11y(String label) {
    return 'Summary period, $label';
  }

  @override
  String get periodPrevMonth => 'Previous month';

  @override
  String get periodNextMonth => 'Next month';

  @override
  String get chartTotalExpenses => 'Total expenses';

  @override
  String get chartNoExpensesThisMonth => 'No expenses\nthis month.';

  @override
  String get chartNoExpensesRegistered => 'No expenses recorded\nthis month.';

  @override
  String chartSemanticsWithData(String total) {
    return 'Category spending chart, total $total';
  }

  @override
  String get chartSemanticsNoData =>
      'Category spending chart, no data this month';

  @override
  String get chartCategoriesHint =>
      'Categories appear when there are expenses this month.';

  @override
  String get chartDonutTapHint => 'Slice or grid — tap to highlight.';

  @override
  String get chartDonutSelectedHeading => 'Selected category';

  @override
  String chartDonutPctOfTotal(int pct) {
    return '$pct% of total';
  }

  @override
  String get chartCategoryOther => 'Other';

  @override
  String get goalsTitle => 'Goals';

  @override
  String get goalsRefresh => 'Refresh';

  @override
  String get goalsLoadError => 'Could not load goals.';

  @override
  String get goalsEmpty => 'No active goals yet.';

  @override
  String get goalsAddTooltip => 'New goal';

  @override
  String get goalsAggregateTitle => 'Overview';

  @override
  String goalsAggregateLine(int count, String saved, String target) {
    return '$count active goals · $saved of $target';
  }

  @override
  String get goalsScreenHint =>
      'Visible progress and combined totals help reinforce saving habits.';

  @override
  String get goalMilestoneBannerQuarter =>
      'You’ve reached a quarter of the goal — nice pace.';

  @override
  String get goalMilestoneBannerHalf => 'Halfway there. Keep the momentum.';

  @override
  String get goalMilestoneBannerAlmost =>
      'Almost there: just a little left to hit the goal.';

  @override
  String get goalMilestoneBannerComplete =>
      'Goal reached. Well done staying consistent.';

  @override
  String get goalMilestoneChipQuarter => '25%';

  @override
  String get goalMilestoneChipHalf => '50%';

  @override
  String get goalMilestoneChipAlmost => '90%';

  @override
  String get goalMilestoneChipComplete => 'OK';

  @override
  String get newGoalTitle => 'New goal';

  @override
  String get goalFormTitleLabel => 'Goal name';

  @override
  String get goalFormTargetLabel => 'Target amount';

  @override
  String get goalFormIntro =>
      'Set a target in your currency. You can track progress later.';

  @override
  String get goalFormSave => 'Create goal';

  @override
  String get goalFormCreatedSnackbar => 'Goal created.';

  @override
  String get goalSaveError => 'Could not save the goal.';

  @override
  String get goalFormInitialLabel => 'Already saved (optional)';

  @override
  String get goalFormInitialHint =>
      'How much you already have toward this goal when you create it.';

  @override
  String get goalDetailTitle => 'Goal';

  @override
  String get goalRemaining => 'Remaining';

  @override
  String get goalCompleted => 'Goal reached';

  @override
  String get goalContribute => 'Add amount';

  @override
  String get goalContributeTitle => 'Contribution';

  @override
  String get goalContributeAmountLabel => 'Amount';

  @override
  String get goalContributeNoteLabel => 'Note (optional)';

  @override
  String get goalContributeSaved => 'Contribution saved.';

  @override
  String get goalContributeError => 'Could not save contribution.';

  @override
  String get goalContributionsTitle => 'History';

  @override
  String get goalContributionsEmpty => 'No contributions yet.';

  @override
  String get goalContributionsOwnerOnly =>
      'Contribution history is only visible to the person who created the goal.';

  @override
  String get goalArchive => 'Archive';

  @override
  String get goalArchiveTitle => 'Archive goal?';

  @override
  String get goalArchiveBody =>
      'The goal will no longer count as active. You can reactivate it later.';

  @override
  String get goalArchivedSnackbar => 'Goal archived.';

  @override
  String get goalDelete => 'Delete';

  @override
  String get goalDeleteTitle => 'Delete goal?';

  @override
  String get goalDeleteBody =>
      'You can only delete when the saved balance is zero.';

  @override
  String get goalDeletedSnackbar => 'Goal deleted.';

  @override
  String get goalInactiveBadge => 'Archived';

  @override
  String get goalReactivate => 'Reactivate';

  @override
  String get goalReactivatedSnackbar => 'Goal reactivated.';

  @override
  String get goalLinearPaceCardTitle => 'Pace and forecast';

  @override
  String goalLinearPaceAvgPerMonth(String amount) {
    return 'Average pace so far: $amount/mo';
  }

  @override
  String goalLinearPaceEta(String monthYear) {
    return 'At this pace: goal ~by $monthYear';
  }

  @override
  String get goalLinearPaceDisclaimer =>
      'Simple estimate from what you’ve saved so far and the goal’s creation date; real pace may vary.';

  @override
  String goalLinearPaceListHint(String amount, String monthYear) {
    return '~$amount/mo · goal ~$monthYear';
  }

  @override
  String get goalLinearPaceInsufficientHistory =>
      'Not enough history yet to estimate pace.';

  @override
  String get expensePayConfirmTitle => 'Mark as paid?';

  @override
  String get expensePayConflict =>
      'This expense can no longer be paid (for example, it is already paid).';

  @override
  String expensePayInstallmentLine(int current, int total) {
    return 'Installment $current of $total';
  }

  @override
  String get expensesTitle => 'Expenses';

  @override
  String get expensesRefresh => 'Refresh';

  @override
  String get expensesNew => 'New';

  @override
  String get expensesNewLong => 'New expense';

  @override
  String get expensesRefreshList => 'Refresh list';

  @override
  String get expensesFilterAll => 'All';

  @override
  String get expensesFilterPending => 'Pending';

  @override
  String get expensesFilterPaid => 'Paid';

  @override
  String get expensesLoadError => 'Could not load.';

  @override
  String get expensesEmpty => 'No expenses for this filter.';

  @override
  String get expensePay => 'Pay';

  @override
  String get expenseMarkedPaid => 'Marked as paid.';

  @override
  String get expensePayError => 'Could not pay.';

  @override
  String expenseTileFamilyCategory(String category) {
    return '$category · Family';
  }

  @override
  String expenseTileDateLine(String date, String status) {
    return 'Date $date · $status';
  }

  @override
  String get expenseStatusPending => 'Pending';

  @override
  String get expenseStatusPaid => 'Paid';

  @override
  String expenseInstallmentChip(int n, int total) {
    return 'Installment $n/$total';
  }

  @override
  String get expenseRecurringMonthly => 'Recurring · monthly';

  @override
  String get expenseRecurringWeekly => 'Recurring · weekly';

  @override
  String get expenseRecurringYearly => 'Recurring · yearly';

  @override
  String expenseSharedWith(String name) {
    return 'Shared · $name';
  }

  @override
  String get expenseShared => 'Shared';

  @override
  String get expenseTitle => 'Expense';

  @override
  String get expenseLoadError => 'Could not load.';

  @override
  String get expenseCompetence => 'Date (period)';

  @override
  String get expenseDue => 'Due date';

  @override
  String get expenseStatusLabel => 'Status';

  @override
  String get expenseInstallmentsRow => 'Installments';

  @override
  String get expenseRecurrence => 'Recurrence';

  @override
  String get expenseShare => 'Sharing';

  @override
  String expenseShareWith(String name) {
    return 'With $name';
  }

  @override
  String get expenseShareFamily => 'Family';

  @override
  String get expenseMarkPaid => 'Mark as paid';

  @override
  String get expenseEdit => 'Edit';

  @override
  String get expenseDelete => 'Delete';

  @override
  String get expenseReadOnlyBanner =>
      'Another family member’s expense — view only.';

  @override
  String get expenseDeleteError => 'Could not delete.';

  @override
  String get expDelInstallmentTitle => 'Delete installment plan';

  @override
  String get expDelInstallmentPaidBody =>
      'This plan has paid installments. Delete the full history or only future pending installments?';

  @override
  String get expDelInstallmentMaybePaidBody =>
      'If any installments are paid, you can keep history and delete only future pending ones, or delete the whole plan.';

  @override
  String get expDelFutureOnly => 'Future pending only';

  @override
  String get expDelAllIncludingPaid => 'Everything (including paid)';

  @override
  String get expDelInstallmentSimpleTitle => 'Delete installment plan?';

  @override
  String expDelInstallmentSimpleBody(int total) {
    return 'All $total installments will be deleted.';
  }

  @override
  String get expDelRecurringTitle => 'Delete recurrence';

  @override
  String get expDelRecurringBody =>
      'You can cancel only future pending occurrences (after today), or end the series: remove all pending and unlink paid items from the series (they stay as normal expenses).';

  @override
  String get expDelCloseSeries => 'End series';

  @override
  String get expDelRecurringOccurrenceTitle => 'Delete recurring expense';

  @override
  String get expDelRecurringOccurrenceBody =>
      'Delete only this period or apply to the whole series?';

  @override
  String get expDelThisOnly => 'This one only';

  @override
  String get expDelWholeSeries => 'Whole series';

  @override
  String get expDelRemoveFromRecurrenceTitle => 'Remove from recurrence';

  @override
  String get expDelRemoveOccurrenceTitle => 'Delete occurrence?';

  @override
  String get expDelPaidUnlinkBody =>
      'This expense is already paid: it will not be removed from history, only detached from the recurring series.';

  @override
  String get expDelPendingDeleteBody =>
      'This pending line will be deleted; the rest of the series stays.';

  @override
  String get expDelSeriesScopeTitle => 'Series scope';

  @override
  String get expDelSeriesScopeBody =>
      'Cancel only future pending occurrences (after today), or end the entire series (pending + unlink paid from the series history)?';

  @override
  String get expDelSingleTitle => 'Delete expense?';

  @override
  String get expDelSingleBody => 'This action cannot be undone.';

  @override
  String get expDelSuccessInstallmentFuture => 'Future installments cancelled.';

  @override
  String get expDelSuccessInstallmentAll => 'Plan deleted.';

  @override
  String get expDelSuccessOccurrence => 'Occurrence deleted.';

  @override
  String get expDelSuccessOccurrenceUnlink => 'Removed from recurrence.';

  @override
  String get expDelSuccessRecurringFuture => 'Future pending cancelled.';

  @override
  String get expDelSuccessRecurringClose => 'Recurrence ended.';

  @override
  String get expDelSuccessSingle => 'Expense deleted.';

  @override
  String get newExpenseTitle => 'New expense';

  @override
  String get editExpenseTitle => 'Edit expense';

  @override
  String get expenseSaveError => 'Could not save.';

  @override
  String get expenseChangesSaved => 'Changes saved.';

  @override
  String get expenseEditOwnerOnly => 'Only the owner can edit this expense.';

  @override
  String get incomesTitle => 'Income';

  @override
  String get newIncomeTitle => 'New income';

  @override
  String get editIncomeTitle => 'Edit income';

  @override
  String get incomeEditOwnerOnly => 'Only the owner can edit this income.';

  @override
  String get incomeSaveError => 'Could not save.';

  @override
  String get incomeSaved => 'Saved.';

  @override
  String get incomeDetailTitle => 'Income';

  @override
  String get incomesAddLong => 'Add income';

  @override
  String get incomesRefresh => 'Refresh';

  @override
  String get incomesRefreshList => 'Refresh list';

  @override
  String get incomesLoadError => 'Could not load.';

  @override
  String get incomesEmpty => 'No income this month.';

  @override
  String get incomesListHint =>
      'Amounts in BRL with exact cents; the month follows the income date.';

  @override
  String incomeTileDateLine(String date) {
    return 'Date $date';
  }

  @override
  String get familyTitle => 'Family';

  @override
  String get securityTitle => 'Security';

  @override
  String get unlockTitle => 'Unlock';

  @override
  String get resetPasswordTitle => 'Reset password';

  @override
  String get categoryLabel => 'Category';

  @override
  String get noneDash => '—';

  @override
  String get secAppTitle => 'App security';

  @override
  String get secSetPinTitle => 'Set app PIN';

  @override
  String get secNewPinTitle => 'New PIN';

  @override
  String get secPinField => 'PIN (4–6 digits)';

  @override
  String get secRepeatPinField => 'Repeat PIN';

  @override
  String get secPinInvalidOrMismatch => 'Invalid PINs or they do not match.';

  @override
  String get secPinSavedSnackbar => 'App PIN saved.';

  @override
  String get secDisablePinTitle => 'Disable PIN';

  @override
  String get secCurrentPinField => 'Current PIN';

  @override
  String get secWrongPin => 'Incorrect PIN.';

  @override
  String get secPinDisabledSnackbar => 'PIN lock disabled.';

  @override
  String get secLockWithPin => 'Lock with PIN';

  @override
  String get secLockPinOnSub =>
      'When minimized, the app asks for the PIN when you return.';

  @override
  String get secLockPinOffSub =>
      'Off — only your online session (login) protects the app.';

  @override
  String get secBiometricTitle => 'Unlock with face or fingerprint';

  @override
  String get secBiometricOnSub =>
      'Uses Face ID, face unlock, or fingerprint — your device picks the method you enrolled with.';

  @override
  String get secBiometricOffSub =>
      'This device does not expose biometrics or face unlock to the app.';

  @override
  String get secChangePin => 'Change PIN';

  @override
  String get unlockIntro => 'Enter your app PIN to continue.';

  @override
  String get unlockPinLabel => 'PIN';

  @override
  String get unlockUseBiometric => 'Use biometrics';

  @override
  String get unlockUseFaceRecognition => 'Use face recognition';

  @override
  String get unlockUseFingerprint => 'Use fingerprint';

  @override
  String get unlockUseBiometricMixed => 'Use face or fingerprint';

  @override
  String get authResetSubtitle => 'Paste the code you received by email.';

  @override
  String get authResetTokenLabel => 'Recovery code';

  @override
  String get authResetTokenHint => 'Paste the full token';

  @override
  String get authResetTokenError => 'Paste the code you received by email';

  @override
  String get authNewPassword => 'New password';

  @override
  String get authConfirmNewPassword => 'Confirm new password';

  @override
  String get authSaveNewPassword => 'Save new password';

  @override
  String get authResetSuccess => 'Password updated. You can sign in.';

  @override
  String get authResetError => 'Could not reset password';

  @override
  String get authRequestNewCode => 'Request a new code';

  @override
  String get shareFamilyTitle => 'Share with family';

  @override
  String get shareFamilySubOn =>
      'Visible to the household; you can choose who to split with.';

  @override
  String get shareFamilySubOff => 'Join a family (invite) to use sharing.';

  @override
  String get shareSplitWith => 'Split with';

  @override
  String get shareWholeFamily => 'Whole family';

  @override
  String get expFormDescription => 'Description';

  @override
  String get expFormAmountInstallment => 'Installment amount (R\$)';

  @override
  String get expFormAmount => 'Amount (R\$)';

  @override
  String get expFormKindSingle => 'One-off';

  @override
  String get expFormKindInstallments => 'Installments';

  @override
  String get expFormKindRecurring => 'Recurring';

  @override
  String get expFormInstallmentsLabel => 'Installments (monthly period)';

  @override
  String get expFormInstallmentsHint => '1 to 24';

  @override
  String get expFormInstallmentRangeError => 'Use 1 to 24';

  @override
  String get expFormInstallmentInvalid => 'Invalid number';

  @override
  String get expFormRecurringFrequency => 'Frequency';

  @override
  String get expFormRecurringChoose => 'Choose…';

  @override
  String get expFormRecurringHelp =>
      'Fixed bills (utilities, internet, subscriptions): the app creates pending lines per month when you open the list. Changing the amount on the main line updates only future pending occurrences; history keeps old values.';

  @override
  String get expFormMarkPaid => 'Already paid';

  @override
  String get expFormHasDueDate => 'Has due date';

  @override
  String get expFormHasDueDateSub => 'Bills / dashboard reminders';

  @override
  String get expFormExpenseDate => 'Expense date (period)';

  @override
  String get expFormDueDate => 'Due date';

  @override
  String get expFormChooseDate => 'Choose…';

  @override
  String get expFormCategoriesLoadError => 'Could not load categories.';

  @override
  String get expFormPickCategory => 'Choose a category.';

  @override
  String get expFormCreateError => 'Could not create.';

  @override
  String expFormPlanCreated(int count) {
    return 'Plan: $count installments created.';
  }

  @override
  String expFormPlanCreatedRef(int count, String ref) {
    return 'Plan: $count installments (ref. $ref).';
  }

  @override
  String get expFormCreated => 'Expense created.';

  @override
  String get expFormPickDue => 'Choose the due date.';

  @override
  String get expFormRecurringFreqRequired => 'Choose recurrence frequency.';

  @override
  String get expEditDescription => 'Description';

  @override
  String get expEditAmount => 'Amount (R\$)';

  @override
  String get expEditExpenseDate => 'Expense date';

  @override
  String get expEditDueOptional => 'Due date (optional)';

  @override
  String get expEditState => 'Status';

  @override
  String get expEditRecurrenceMeta => 'Recurrence (metadata)';

  @override
  String get expEditRecurrenceNone => 'None';

  @override
  String expEditInstallmentBanner(int n, int total) {
    return 'Installment $n of $total. Changes apply only to this line.';
  }

  @override
  String get expEditRecurringAnchorBanner =>
      'Recurrence: when you change the amount, only generated future pending lines (date after today) update; past months keep historical values.';

  @override
  String get incFormDescription => 'Description';

  @override
  String get incFormAmount => 'Amount (R\$)';

  @override
  String get incFormIncomeDate => 'Income date';

  @override
  String get incFormNotes => 'Notes (optional)';

  @override
  String get incDetailLoadError => 'Error.';

  @override
  String get familyLoadError => 'Error.';

  @override
  String get familySaveError => 'Could not save.';

  @override
  String get saveChanges => 'Save changes';

  @override
  String get expFormAmountHint => 'e.g. 12.50';

  @override
  String expFormInstallmentNeedAmountLine(int count) {
    return 'Enter each installment amount. Total = installment × $count.';
  }

  @override
  String expFormInstallmentPlanTotalLine(
    String total,
    int count,
    String installment,
  ) {
    return 'Plan total: $total ($count × $installment).';
  }

  @override
  String get expFormFreqMonthly => 'Monthly';

  @override
  String get expFormFreqWeekly => 'Weekly';

  @override
  String get expFormFreqYearly => 'Yearly';

  @override
  String get incFormIntro =>
      'Record real inflows (salary, extras, etc.). The month balance on the dashboard sums income in this period.';

  @override
  String get incFormDescHint => 'e.g. April salary, Client X fees';

  @override
  String get incFormAmountHint => 'e.g. 3,500.00';

  @override
  String get incFormIncomeDateCompetence => 'Income date (period)';

  @override
  String get incFormPickCategory => 'Choose an income type.';

  @override
  String get incFormCreatedSnackbar => 'Income saved.';

  @override
  String get incFormSaveButton => 'Save income';

  @override
  String get incFormCategoriesLoadError => 'Could not load income types.';

  @override
  String get incFormNotesHint =>
      'Source, reference, tax ID… — for your household only';

  @override
  String get incFormCategoryLabel => 'Income type';

  @override
  String get incomeDeleteTitle => 'Delete income?';

  @override
  String get incomeDeletedSnackbar => 'Deleted.';

  @override
  String get incomeReadOnlyBanner =>
      'Another household member’s income — view only.';

  @override
  String get incomeDetailTypeLabel => 'Type';

  @override
  String get incomeDetailDateCompetenceLabel => 'Date (period)';

  @override
  String get incomeDetailNotesLabel => 'Notes';

  @override
  String get incomeDeleteError => 'Could not delete.';

  @override
  String get unlockPinTooShort => 'PIN must be at least 4 digits';

  @override
  String get unlockBioReason => 'Confirm your identity to unlock Well Paid';

  @override
  String get unlockBioUnavailable => 'Biometrics unavailable';

  @override
  String get famLeaveTitle => 'Leave family';

  @override
  String get famLeaveBody =>
      'If you are the only member, the family will be removed. If you are the owner, ownership passes to another member.';

  @override
  String get famExitAction => 'Leave';

  @override
  String get famRemoveMemberTitle => 'Remove member';

  @override
  String famRemoveMemberConfirm(String email) {
    return 'Remove $email?';
  }

  @override
  String get famRenameTitle => 'Family name';

  @override
  String get famNameField => 'Name';

  @override
  String get famInviteTitle => 'Invite';

  @override
  String famInviteValidUntil(String date) {
    return 'Valid until $date';
  }

  @override
  String get famCopyTokenButton => 'Copy token';

  @override
  String get famNoFamilyIntro =>
      'Create a family or join with an invite (token or QR).';

  @override
  String get famCreateNameOptional => 'Family name (optional)';

  @override
  String get famJoinSectionTitle => 'Join with invite';

  @override
  String get famJoinTokenField => 'Paste invite token';

  @override
  String get famCreate => 'Create family';

  @override
  String get famJoin => 'Join family';

  @override
  String get famInviteQr => 'Generate invite (QR)';

  @override
  String get famLeave => 'Leave family';

  @override
  String get famMembersSection => 'Members';

  @override
  String famMemberCount(int current, int max) {
    return '$current / $max members';
  }

  @override
  String get famRoleOwner => 'Owner';

  @override
  String get famRoleMember => 'Member';

  @override
  String get famYouSuffix => ' (you)';

  @override
  String get famEditNameTooltip => 'Edit name';

  @override
  String get dashCashflowTitle => 'Monthly history';

  @override
  String get dashCashflowChartOptions => 'Period & forecast';

  @override
  String get dashCashflowDynamicMode => 'Dynamic mode';

  @override
  String get dashCashflowStartMonth => 'Start';

  @override
  String get dashCashflowEndMonth => 'End';

  @override
  String get dashCashflowForecastMonths => 'Forecast months';

  @override
  String get dashCashflowApply => 'Apply';

  @override
  String get dashCashflowLegendIncome => 'Income';

  @override
  String get dashCashflowLegendExpensePaid => 'Paid expenses';

  @override
  String get dashCashflowLegendExpenseForecast => 'Forecast expenses';

  @override
  String get dashCashflowEmpty => 'No data for this period.';

  @override
  String get dashCashflowTouchChartHint =>
      'Tap the chart to pick a month. Default: month with the most activity.';

  @override
  String dashCashflowInsightPeakPaid(String month, String amount) {
    return 'Peak paid spending: $month · $amount';
  }

  @override
  String get dashCashflowError => 'Could not load monthly history.';

  @override
  String dashCashflowFooterForecastTotal(String amount) {
    return 'Forecast total: $amount';
  }

  @override
  String dashCashflowFooterBalance(String amount) {
    return 'Period balance: $amount';
  }

  @override
  String get dashCashflowSemantics => 'Monthly income and expenses chart';

  @override
  String dashCashflowLoadedPoints(int count) {
    return '$count months in range';
  }

  @override
  String get dashCashflowRangeInvalid =>
      'Start month cannot be after end month.';

  @override
  String get dashCashflowA11yPickStartMonth =>
      'Choose start month of the range';

  @override
  String get dashCashflowA11yPickEndMonth => 'Choose end month of the range';

  @override
  String get dashCashflowA11yForecastDropdown =>
      'Number of forecast months after the range';

  @override
  String get dashCashflowA11yForecastDecrease => 'Show fewer forecast months';

  @override
  String get dashCashflowA11yForecastIncrease => 'Show more forecast months';

  @override
  String get dashCashflowA11yApply => 'Apply filters to monthly history';

  @override
  String dashCashflowA11ySeriesToggle(String name) {
    return '$name. Tap to show or hide on the chart.';
  }

  @override
  String dashCashflowA11ySummary(String forecast, String balance) {
    return 'Period totals. Forecast: $forecast. Balance: $balance.';
  }

  @override
  String get dashCashflowDynamicWindowTooltip =>
      'On: chart uses the server’s rolling window (recent months, not tied only to the header month). Off: fixed 6 months — from the header month back 5 months. Right: how many future months show forecast spending (works in both modes).';

  @override
  String get dashCashflowBarRollingLabel => 'Auto';

  @override
  String get dashCashflowBarFixedLabel => 'Header';

  @override
  String get dashCashflowForecastBarShort => 'Ahead';

  @override
  String get dashCashflowForecastBarTooltip =>
      'Future months on the axis with forecast spending (beyond the loaded history).';

  @override
  String dashCashflowInsightPeakIncome(String month, String amount) {
    return 'Peak income: $month · $amount';
  }

  @override
  String get dashHomeCategoriesFootnote =>
      'Category totals match the month selected in the header.';

  @override
  String get shoppingListsTitle => 'Shopping lists';

  @override
  String get shoppingListsMenuLabel => 'Shopping lists';

  @override
  String get shoppingListsActiveSection => 'Planning';

  @override
  String get shoppingListsHistorySection => 'History';

  @override
  String get shoppingListsEmpty =>
      'No lists yet. Create one to plan items and add prices in the store.';

  @override
  String get shoppingListsNewList => 'New list';

  @override
  String get shoppingListUntitled => 'Untitled list';

  @override
  String shoppingListItemsCount(int count) {
    return '$count items';
  }

  @override
  String shoppingListSubtotal(String amount) {
    return 'Subtotal: $amount';
  }

  @override
  String get shoppingListNoPriceYet => 'No price yet (add in store)';

  @override
  String get shoppingListAddItem => 'Add item';

  @override
  String get shoppingListItemLabelHint => 'Item description';

  @override
  String get shoppingListItemQuantity => 'Quantity';

  @override
  String get shoppingListItemAmountOptional => 'Unit price (optional)';

  @override
  String get shoppingListDeleteItem => 'Remove item';

  @override
  String get shoppingListConfirmDeleteDraftTitle => 'Delete list?';

  @override
  String get shoppingListConfirmDeleteDraftBody =>
      'This draft list will be removed. This cannot be undone.';

  @override
  String get shoppingListComplete => 'Check out';

  @override
  String get shoppingListCompleteTitle => 'Create expense';

  @override
  String get shoppingListTotalOverrideHint =>
      'Amount paid at store (optional — replaces sum and discount)';

  @override
  String get shoppingListDiscountHint =>
      'Discount (optional — subtracts from line sum; not with manual total)';

  @override
  String get shoppingListDiscountOverrideConflict =>
      'Fill either the manual total or the discount, not both.';

  @override
  String get shoppingListTotalMismatchTitle => 'Total differs from line sum';

  @override
  String shoppingListTotalMismatchBody(String manual, String subtotal) {
    return 'You entered $manual but the line subtotal is $subtotal. The expense will use your amount. Continue?';
  }

  @override
  String get shoppingListDescriptionOptional =>
      'Expense description (optional)';

  @override
  String get shoppingListMarkPaid => 'Already paid';

  @override
  String get shoppingListExpenseFromListPaidNote =>
      'The expense is saved as paid, with no installments or recurrence. Only the list name and total appear on the expense.';

  @override
  String get shoppingListExpenseDate => 'Expense date';

  @override
  String get shoppingListViewExpense => 'View expense';

  @override
  String get shoppingListAlignTotalToLinesButton =>
      'Update expense to line sum';

  @override
  String get shoppingListAlignTotalSuccess =>
      'Expense total updated to match the lines.';

  @override
  String shoppingListCompletedOn(String date) {
    return 'Completed on $date';
  }

  @override
  String get shoppingListEditMetaTitle => 'Title and store';

  @override
  String get shoppingListTitleOptional => 'Title (optional)';

  @override
  String get shoppingListStoreOptional => 'Store (optional)';

  @override
  String get shoppingListErrorLoad => 'Could not load shopping lists.';

  @override
  String get shoppingListReadOnlyDraft =>
      'Only the author can edit this draft list.';

  @override
  String get shoppingListCompletedEditHint =>
      'You can fix amounts, quantities, and remove items. The list cannot be empty.';

  @override
  String get shoppingListNoItems =>
      'No items yet. Add products to plan your trip.';

  @override
  String get shoppingListFooterEstimatedTotal => 'Estimated total';

  @override
  String get shoppingListFooterEstimatedNote =>
      'Sum of (unit price × quantity) for lines with a price.';

  @override
  String shoppingListFooterUnitsSummary(int unitCount) {
    return '$unitCount items total';
  }

  @override
  String get shoppingListFooterAddItemCompleted =>
      'You can still add items to this completed list.';

  @override
  String get shoppingListInlineAmountHint => 'Unit';

  @override
  String get shoppingListEditLabelTitle => 'Item name';

  @override
  String get shoppingListConfirmRemoveItemTitle => 'Remove item?';

  @override
  String get shoppingListConfirmRemoveItemBody =>
      'This product will be removed from the list.';

  @override
  String get shoppingListEditItemTitle => 'Edit item';

  @override
  String get shoppingListCompleteSuccess =>
      'Expense saved. The list is now in history.';

  @override
  String get shoppingListFlushDraftError =>
      'Could not sync list items. Check your connection and try again.';

  @override
  String get shoppingListCompleteInProgress => 'Syncing and completing…';
}
