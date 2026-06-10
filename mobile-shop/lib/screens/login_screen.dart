import 'package:flutter/material.dart';

import '../api/catalog_api.dart';
import '../l10n/strings.dart';
import '../widgets/auth_scope.dart';
import 'my_orders_screen.dart';

class LoginScreen extends StatefulWidget {
  final CatalogApi api;

  const LoginScreen({super.key, required this.api});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phoneController = TextEditingController(text: '+998');
  final _codeController = TextEditingController();
  final _nameController = TextEditingController();

  bool _codeStage = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _phoneController.dispose();
    _codeController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  bool get _phoneValid {
    final digits = _phoneController.text.replaceAll(RegExp(r'[^0-9]'), '');
    return digits.length >= 9 && digits.length <= 15;
  }

  Future<void> _sendCode() async {
    if (!_phoneValid || _busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.requestOtp(_phoneController.text.trim());
      if (mounted) {
        setState(() => _codeStage = true);
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text(S.codeSent)));
      }
    } on ApiException catch (e) {
      if (mounted) {
        setState(() => _error =
            e.statusCode == 429 ? S.tooManyAttempts : S.loadError);
      }
    } catch (_) {
      if (mounted) setState(() => _error = S.loadError);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _verify() async {
    if (_codeController.text.trim().length < 4 || _busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final session = await widget.api.verifyOtp(
        _phoneController.text.trim(),
        _codeController.text.trim(),
        name: _nameController.text.trim(),
      );
      if (!mounted) return;
      AuthScope.of(context).login(
          token: session.token, phone: session.phone, name: session.name);
      Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => MyOrdersScreen(api: widget.api)));
    } on ApiException catch (e) {
      if (mounted) {
        setState(() => _error =
            e.statusCode == 429 ? S.tooManyAttempts : S.invalidCode);
      }
    } catch (_) {
      if (mounted) setState(() => _error = S.invalidCode);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(S.loginTitle)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Icon(Icons.lock_outline, size: 56, color: Colors.grey),
          const SizedBox(height: 12),
          const Text(S.loginHint, textAlign: TextAlign.center),
          const SizedBox(height: 24),
          TextField(
            controller: _phoneController,
            enabled: !_codeStage,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(
              labelText: S.phoneNumber,
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          if (!_codeStage)
            FilledButton(
              onPressed: _busy ? null : _sendCode,
              child: Text(_busy ? S.submitting : S.sendCode),
            )
          else ...[
            TextField(
              controller: _codeController,
              keyboardType: TextInputType.number,
              maxLength: 6,
              autofocus: true,
              decoration: const InputDecoration(
                labelText: S.smsCode,
                border: OutlineInputBorder(),
                counterText: '',
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _nameController,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: S.nameOptional,
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _busy ? null : _verify,
              child: Text(_busy ? S.submitting : S.confirmCode),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                TextButton(
                  onPressed: _busy ? null : () => setState(() => _codeStage = false),
                  child: const Text(S.changePhone),
                ),
                TextButton(
                  onPressed: _busy ? null : _sendCode,
                  child: const Text(S.resendCode),
                ),
              ],
            ),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!,
                style: const TextStyle(color: Colors.red),
                textAlign: TextAlign.center),
          ],
        ],
      ),
    );
  }
}
