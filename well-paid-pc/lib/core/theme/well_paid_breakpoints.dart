/// Larguras de referência para layout desktop (8pt grid).
abstract final class WellPaidBreakpoints {
  static const double compact = 600;
  static const double medium = 900;
  static const double expanded = 1200;
  static const double pageMaxWidth = 1320;

  static bool isCompactWidth(double w) => w < compact;
  static bool isMediumWidth(double w) => w >= compact && w < medium;
  static bool isExpandedWidth(double w) => w >= medium;
  static bool isWideChartsWidth(double w) => w >= medium;
}
