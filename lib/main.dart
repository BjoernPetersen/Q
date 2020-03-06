import 'dart:async';

import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:kiu/data/dependency_model.dart';
import 'package:kiu/data/preferences.dart';
import 'package:kiu/view/common.dart';
import 'package:kiu/view/page/bot_page.dart';
import 'package:kiu/view/page/login_page.dart';
import 'package:kiu/view/page/queue_page.dart';
import 'package:kiu/view/page/state_page.dart';
import 'package:kiu/view/page/suggestions_page.dart';
import 'package:kiu/view/widget/basic_provider.dart';
import 'package:kiu/view/widget/share_handler.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DependencyModel().init();
  runApp(Kiu());
}

class Kiu extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BasicProvider(
      child: ShareHandler(
        child: MaterialApp(
          title: 'Kiu',
          theme: ThemeData(
            primaryColor: Color(0xFFBBC0CA),
            highlightColor: Color(0xFFB0B5C1),
          ),
          initialRoute: _initialRoute(),
          routes: {
            "/selectBot": (_) => BotPage(),
            "/login": (_) => LoginPage(),
            "/queue": (_) => QueuePage(),
            "/suggestions": (_) => SuggestionsPage(),
            "/state": (_) => StatePage(),
          },
          localizationsDelegates: [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
          ],
          supportedLocales: [
            const Locale('en'),
            const Locale('de'),
          ],
        ),
      ),
    );
  }
}

String _initialRoute() {
  final username = Preference.username.getString();
  if (username == null || username.isEmpty) {
    return "/login";
  } else {
    return "/queue";
  }
}
