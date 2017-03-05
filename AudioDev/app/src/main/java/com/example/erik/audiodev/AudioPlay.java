package com.example.erik.audiodev;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AudioPlay extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_play);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // real time processing needed
                int durationMs = 3000;
                int count = (int)(44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
                // hold the samples to play
                short[] samples = new short[count];
                // fundamental frequency
                int fundHz = 200;
                // consonant sounding relative frequency scalings
                double[] RelConsf = {1, 2, 3, 4, 6}; int cons_len = 5;
                // dissonant sounding relative frequency scalings
                double[] RelDisf = {1, 1.33333, 3.162544, 4.5656777, 6.756476}; int dis_len = 5;
                boolean good = true;
                for(int i = 0; i < count; i += 2){
                    double sum = 0.0; // value to save as a sample
                    if (good) {
                        double[] pts = new double[ cons_len ];
                        for (int j = 0; j < cons_len; j++) {
                            pts[j] = i / ( (float)44100 / (fundHz*RelConsf[j]) ) * 2.0 * Math.PI;
                            pts[j] = Math.sin(pts[j]); // -1.0 < pts[j] < 1.0
                            sum += pts[j]/cons_len;
                        }
                    } else {
                        double[] pts = new double[ dis_len ];
                        for (int j = 0; j < dis_len; j++) {
                            pts[j] = i / ( (float)44100 / (fundHz*RelDisf[j]) ) * 2.0 * Math.PI;
                            pts[j] = Math.sin(pts[j]); // -1.0 < pts[j] < 1.0
                            sum += pts[j]/dis_len;
                        }
                    }
                    // clamp the sum value at abs(1.0)
                    if (sum > 1.0)
                        sum = 1.0;
                    else if (sum < -1.0)
                        sum = -1.0;
                    // write the sample values to play
                    samples[i + 0] = (short)(sum * 32000);
                    samples[i + 1] = (short)(sum * 32000);
                }
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                        count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
                track.write(samples, 0, count);
                track.play();
            }
        });
    }
}
