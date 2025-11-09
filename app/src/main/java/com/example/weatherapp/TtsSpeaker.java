package com.example.weatherapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TtsSpeaker {
    private TextToSpeech tts;

    public void init(Context ctx, Runnable onReady){
        tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("id","ID"));
            if (onReady != null) onReady.run();
        });
    }

    public void speak(String text){
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "weather_tts");
    }

    public void shutdown(){ if (tts != null) tts.shutdown(); }
}
