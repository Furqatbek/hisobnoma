import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/util/format.dart';

void main() {
  group('formatUzs', () {
    test('formats thousands with spaces', () {
      expect(formatUzs(12000), '12 000 сўм');
      expect(formatUzs(1234567), '1 234 567 сўм');
      expect(formatUzs(100), '100 сўм');
      expect(formatUzs(0), '0 сўм');
    });

    test('shows decimals only when present', () {
      expect(formatUzs(9999.5), '9 999.5 сўм');
      expect(formatUzs(9999.55), '9 999.55 сўм');
      expect(formatUzs(12000.0), '12 000 сўм');
    });

    test('handles null', () {
      expect(formatUzs(null), '—');
    });

    test('handles negative amounts', () {
      expect(formatUzs(-5000), '-5 000 сўм');
    });

    test('rounds fractional overflow up', () {
      expect(formatUzs(1.999), '2 сўм');
    });
  });
}
