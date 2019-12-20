import 'package:json_annotation/json_annotation.dart';

part 'model.g.dart';

@JsonSerializable()
class ImplementationInfo {
  final String name;
  final String version;
  final String projectInfo;

  ImplementationInfo({this.name, this.version, this.projectInfo});

  factory ImplementationInfo.fromJson(Map<String, dynamic> json) =>
      _$ImplementationInfoFromJson(json);
}

@JsonSerializable()
class BotInfo {
  final String apiVersion;
  final ImplementationInfo implementation;

  BotInfo({this.apiVersion, this.implementation});

  factory BotInfo.fromJson(Map<String, dynamic> json) =>
      _$BotInfoFromJson(json);
}

@JsonSerializable()
class RegisterCredentials {
  final String name;
  final String userId;

  RegisterCredentials({this.name, this.userId});

  factory RegisterCredentials.fromJson(Map<String, dynamic> json) =>
      _$RegisterCredentialsFromJson(json);

  Map<String, dynamic> toJson() => _$RegisterCredentialsToJson(this);
}

@JsonSerializable()
class NamedPlugin {
  final String id;
  final String name;

  NamedPlugin({this.id, this.name});

  factory NamedPlugin.fromJson(Map<String, dynamic> json) =>
      _$NamedPluginFromJson(json);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is NamedPlugin &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}

@JsonSerializable()
class Song {
  final String id;
  final NamedPlugin provider;
  final String title;
  final String description;
  final int duration;
  final String albumArtPath;

  Song({
    this.id,
    this.provider,
    this.title,
    this.description,
    this.duration,
    this.albumArtPath,
  });

  factory Song.fromJson(Map<String, dynamic> json) => _$SongFromJson(json);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Song &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          provider == other.provider;

  @override
  int get hashCode => id.hashCode ^ provider.hashCode;
}

@JsonSerializable()
class SongEntry {
  final Song song;
  final String userName;

  SongEntry({this.song, this.userName});

  factory SongEntry.fromJson(Map<String, dynamic> json) =>
      _$SongEntryFromJson(json);
}

@JsonSerializable()
class Volume {
  final int volume;
  final bool isSupported;

  Volume({this.volume, this.isSupported});

  factory Volume.fromJson(Map<String, dynamic> json) => _$VolumeFromJson(json);
}

@JsonSerializable()
class PlayerState {
  @JsonKey(fromJson: _stateFromJson)
  final PlayerStateType state;
  final SongEntry songEntry;
  final int progress;

  PlayerState({this.state, this.songEntry, this.progress});

  factory PlayerState.fromJson(Map<String, dynamic> json) =>
      _$PlayerStateFromJson(json);
}

enum PlayerStateType { play, pause, stop, error }

PlayerStateType _stateFromJson(dynamic value) {
  switch (value) {
    case "PLAY":
      return PlayerStateType.play;
    case "PAUSE":
      return PlayerStateType.pause;
    case "STOP":
      return PlayerStateType.stop;
    case "ERROR":
    default:
      return PlayerStateType.error;
  }
}

@JsonSerializable()
class PlayerStateChange {
  @JsonKey(toJson: _stateChangeToJson)
  final PlayerStateChangeAction action;

  PlayerStateChange(this.action);

  Map<String, dynamic> toJson() {
    return _$PlayerStateChangeToJson(this);
  }
}

enum PlayerStateChangeAction { play, pause, skip }

extension on PlayerStateChangeAction {
  String get text {
    switch (this) {
      case PlayerStateChangeAction.play:
        return "PLAY";
      case PlayerStateChangeAction.pause:
        return "PAUSE";
      case PlayerStateChangeAction.skip:
        return "SKIP";
    }
  }
}

String _stateChangeToJson(PlayerStateChangeAction action) {
  return action.text;
}
