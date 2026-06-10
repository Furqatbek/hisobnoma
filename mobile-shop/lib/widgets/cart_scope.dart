import 'package:flutter/widgets.dart';

import '../models/cart.dart';

/// Makes the [CartStore] available to the widget tree and rebuilds
/// dependents when the cart changes. Plain Flutter, no extra packages.
class CartScope extends InheritedNotifier<CartStore> {
  const CartScope({super.key, required CartStore cart, required super.child})
      : super(notifier: cart);

  static CartStore of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<CartScope>();
    assert(scope != null, 'CartScope is missing in the widget tree');
    return scope!.notifier!;
  }
}
