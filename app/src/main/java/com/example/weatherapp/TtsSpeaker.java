package com.example.weatherapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TtsSpeaker {
    private TextToSpeech tts;
    public void init(Context ctx){
        tts = new TextToSpeech(ctx, s -> {
            if (s == TextToSpeech.SUCCESS) tts.setLanguage(Locale.getDefault());
        });
    }
    public void speak(String text){
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "weather_tts");
    }
    public void release(){ if (tts != null) tts.shutdown(); }
}
