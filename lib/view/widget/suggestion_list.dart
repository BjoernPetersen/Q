import 'package:flutter/material.dart';
import 'package:kiu/bot/connection_manager.dart';
import 'package:kiu/bot/model.dart';
import 'package:kiu/bot/permission.dart';
import 'package:kiu/data/dependency_model.dart';
import 'package:kiu/view/widget/result_list.dart';

class SuggestionList extends StatefulWidget {
  final NamedPlugin suggester;
  final List<Song> suggestions;

  const SuggestionList({
    Key key,
    @required this.suggester,
    @required this.suggestions,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() =>
      _SuggestionListState(suggester, suggestions);
}

class _SuggestionListState extends State<SuggestionList> {
  final NamedPlugin suggester;
  List<Song> suggestions;

  _SuggestionListState(this.suggester, this.suggestions);

  Future<void> _delete(ConnectionManager manager, Song song) async {
    final bot = await manager.getService();
    await bot.removeSuggestion(
        suggesterId: suggester.id,
        providerId: song.provider.id,
        songId: song.id);
    setState(() {
      suggestions.remove(song);
    });
  }

  @override
  Widget build(BuildContext context) {
    final manager = service<ConnectionManager>();
    if (manager.hasPermission(Permission.DISLIKE)) {
      return ResultList(
        results: suggestions,
        trailingBuilder: (_, song) {
          return IconButton(
            tooltip: "Remove suggestion",
            icon: Icon(Icons.thumb_down),
            onPressed: () => _delete(manager, song),
          );
        },
      );
    } else {
      return ResultList(results: suggestions);
    }
  }
}
