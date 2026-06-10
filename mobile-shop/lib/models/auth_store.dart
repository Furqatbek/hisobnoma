import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Logged-in web-customer session, persisted across app restarts.
class AuthStore extends ChangeNotifier {
  static const _tokenKey = 'auth_token_v1';
  static const _phoneKey = 'auth_phone_v1';
  static const _nameKey = 'auth_name_v1';

  final SharedPreferences? _prefs;

  String? _token;
  String? _phone;
  String? _name;

  AuthStore({SharedPreferences? prefs}) : _prefs = prefs; // ignore: prefer_initializing_formals

  static Future<AuthStore> load() async {
    final prefs = await SharedPreferences.getInstance();
    final store = AuthStore(prefs: prefs);
    store._token = prefs.getString(_tokenKey);
    store._phone = prefs.getString(_phoneKey);
    store._name = prefs.getString(_nameKey);
    return store;
  }

  bool get isLoggedIn => _token != null && _token!.isNotEmpty;

  String? get token => _token;

  String? get phone => _phone;

  String? get name => _name;

  void login({required String token, required String phone, String? name}) {
    _token = token;
    _phone = phone;
    _name = name;
    _prefs?.setString(_tokenKey, token);
    _prefs?.setString(_phoneKey, phone);
    if (name != null && name.isNotEmpty) {
      _prefs?.setString(_nameKey, name);
    } else {
      _prefs?.remove(_nameKey);
    }
    notifyListeners();
  }

  void logout() {
    _token = null;
    _phone = null;
    _name = null;
    _prefs?.remove(_tokenKey);
    _prefs?.remove(_phoneKey);
    _prefs?.remove(_nameKey);
    notifyListeners();
  }
}
