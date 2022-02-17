import 'package:flutter_twilio_voice/flutter_twilio_voice.dart';

class FlutterTwilioVoiceCall {
  final String id;
  final String to;
  final String toDisplayName;
  final String toPhotoURL;
  final FlutterTwilioVoiceEventStatus status;
  final bool mute;
  final bool speaker;

  FlutterTwilioVoiceCall({
    this.to,
    this.toDisplayName,
    this.toPhotoURL,
    this.status,
    this.id,
    this.mute,
    this.speaker,
  });

  factory FlutterTwilioVoiceCall.fromMap(Map<String, dynamic> data) {
    return FlutterTwilioVoiceCall(
      id: data["id"],
      to: data["to"]??"",
      toDisplayName: data["toDisplayName"] ?? "",
      toPhotoURL: data["toPhotoURL"] ?? "",
      status: FlutterTwilioVoice.getEventType(data["status"]),
      mute: data["mute"],
      speaker: data["speaker"],
    );
  }
}
