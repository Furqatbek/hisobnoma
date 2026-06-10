/// Formats an amount in UZS with space-separated thousands: 12 000 сўм.
/// Decimals are shown only when present (max 2 digits).
String formatUzs(num? amount) {
  if (amount == null) return '—';

  final isNegative = amount < 0;
  final abs = amount.abs();

  final intPart = abs.truncate();
  final fraction = abs - intPart;

  final digits = intPart.toString();
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) buffer.write(' ');
    buffer.write(digits[i]);
  }

  var result = buffer.toString();
  if (fraction > 0) {
    var frac = (fraction * 100).round();
    if (frac >= 100) {
      // Rounding overflowed into the integer part (e.g. 1.999) — recurse once.
      return formatUzs(isNegative ? -(intPart + 1) : (intPart + 1));
    }
    if (frac % 10 == 0) {
      result += '.${frac ~/ 10}';
    } else {
      result += '.${frac.toString().padLeft(2, '0')}';
    }
  }

  return '${isNegative ? '-' : ''}$result сўм';
}
