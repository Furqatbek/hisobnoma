import 'package:flutter/widgets.dart';

import '../models/auth_store.dart';

/// Makes the [AuthStore] available to the widget tree.
class AuthScope extends InheritedNotifier<AuthStore> {
  const AuthScope({super.key, required AuthStore auth, required super.child})
      : super(notifier: auth);

  static AuthStore of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AuthScope>();
    assert(scope != null, 'AuthScope is missing in the widget tree');
    return scope!.notifier!;
  }
}
