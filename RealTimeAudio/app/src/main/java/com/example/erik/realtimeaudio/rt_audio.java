package com.example.erik.realtimeaudio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class rt_audio extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rt_audio);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                /* Defined variables */
                int fundHz = 220;
                int fade_len = 4000; int fade_start = 0;
                // consonant sounding relative frequency scalings
                double[] RelConsf = {1, 2, 3, 4, 6};
                int cons_len = 5;
                // dissonant sounding relative frequency scalings
                double[] RelDisf = {1, 1.33333, 3.162544, 4.5656777, 6.756476};
                int dis_len = 5;

                /* Start up routine */
                int min_buf = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                        min_buf, AudioTrack.MODE_STREAM);
                short[] samples = new short[min_buf];
                int delta = 0;
                boolean good = true;
                int scale_len = 5;
                double[] scaling_new = {1,.75,.5,.1,.1};
                double[] scaling_old = {.1,.1,.5,.75,1};

                boolean test = false;
                int count = 0;

                /* Constant loop */
                while (true) {

                    /* Read in the data from other thread */
                    //good = true; // consonance switch
                    System.arraycopy(scaling_new, 0, scaling_old, 0, scaling_new.length); // copy new into old
                    // read in new amplitude scaling
                    if (good)
                    {
                        if (count == 0 || count == 10 || count == 20) {
                            for (int k = 0; k < 5; k++) {
                                scaling_new[k] = Math.abs(Math.random());
                            }
                        }
                        count++;
                        if (count > 30) {
                            count = 0; good = false;
                        }
                    } else {
                        if (count == 0 || count == 10 || count == 20) {
                            for (int k = 0; k < 5; k++) {
                                scaling_new[k] = Math.abs(Math.random());
                            }
                        }
                        count++;
                        if (count > 30) {
                            count = 0; good = true;
                        }
                    }

                    /* Compute values to play */
                    fade_start = delta; // begin amplitude fade through
                    for (int i = 0; i < min_buf; i += 2) {
                        double sum = 0.0; // value to save as a sample
                        if (good) {
                            double[] pts = new double[cons_len];
                            for (int j = 0; j < cons_len; j++) {
                                pts[j] = delta / ((float) 44100 / (fundHz * RelConsf[j])) * 2.0 * Math.PI;
                                pts[j] = Math.sin(pts[j]); // -1.0 < pts[j] < 1.0
                                if (delta - fade_start < fade_len) { // are we fading?
                                    double old_scale = scaling_old[j]*(double)(fade_start+fade_len-delta)/fade_len;
                                    double new_scale = scaling_new[j]*(double)(delta-fade_start)/fade_len;
                                    sum += pts[j]*(new_scale+old_scale) / cons_len;
                                }
                                else
                                    sum += pts[j]*scaling_new[j] / cons_len;
                            }
                        } else {
                            double[] pts = new double[dis_len];
                            for (int j = 0; j < dis_len; j++) {
                                pts[j] = delta / ((float) 44100 / (fundHz * RelDisf[j])) * 2.0 * Math.PI;
                                pts[j] = Math.sin(pts[j]); // -1.0 < pts[j] < 1.0
                                if (delta - fade_start < fade_len) { // are we fading?
                                    double old_scale = scaling_old[j]*(double)(fade_start+fade_len-delta)/fade_len;
                                    double new_scale = scaling_new[j]*(double)(delta-fade_start)/fade_len;
                                    sum += pts[j]*(new_scale+old_scale) / dis_len;
                                }
                                else
                                    sum += pts[j]*scaling_new[j] / dis_len;
                            }
                        }
                        // clamp the sum value at abs(1.0)
                        if (sum > 1.0)
                            sum = 1.0;
                        else if (sum < -1.0)
                            sum = -1.0;

                        /* Save the sample values to play */
                        if (good) {
                            samples[i    ] = (short) (sum * 20000);
                            samples[i + 1] = (short) (sum * 20000);
                        } else {
                            samples[i    ] = (short) (sum * 32000);
                            samples[i + 1] = (short) (sum * 32000);
                        }

                        /* Increment the generation variable */
                        delta++;
                    }

                    /* Begin playing the values */
                    track.write(samples, 0, min_buf);
                    track.play();
                }
            }
        });
    }
}
