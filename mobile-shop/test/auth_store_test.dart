import 'package:flutter_test/flutter_test.dart';
import 'package:hisobnoma_shop/models/auth_store.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  test('login stores session and notifies', () {
    final auth = AuthStore();
    var notifications = 0;
    auth.addListener(() => notifications++);

    auth.login(token: 't1', phone: '998901234567', name: 'Ali');

    expect(auth.isLoggedIn, true);
    expect(auth.phone, '998901234567');
    expect(auth.name, 'Ali');
    expect(notifications, 1);
  });

  test('logout clears the session', () {
    final auth = AuthStore();
    auth.login(token: 't1', phone: '998901234567');

    auth.logout();

    expect(auth.isLoggedIn, false);
    expect(auth.token, isNull);
    expect(auth.phone, isNull);
  });

  test('session survives a reload round-trip', () async {
    SharedPreferences.setMockInitialValues({});

    final auth = await AuthStore.load();
    auth.login(token: 'persisted-token', phone: '998901234567', name: 'Ali');
    await Future<void>.delayed(Duration.zero);

    final reloaded = await AuthStore.load();
    expect(reloaded.isLoggedIn, true);
    expect(reloaded.token, 'persisted-token');
    expect(reloaded.name, 'Ali');
  });

  test('logout is persisted too', () async {
    SharedPreferences.setMockInitialValues({
      'auth_token_v1': 'old',
      'auth_phone_v1': '998901234567',
    });

    final auth = await AuthStore.load();
    expect(auth.isLoggedIn, true);
    auth.logout();
    await Future<void>.delayed(Duration.zero);

    final reloaded = await AuthStore.load();
    expect(reloaded.isLoggedIn, false);
  });
}
