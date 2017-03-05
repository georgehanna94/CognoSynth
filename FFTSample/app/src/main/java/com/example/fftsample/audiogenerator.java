package com.example.fftsample;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by George on 2017-03-04.
 * Made infinitely better by Erik later that same day.
 */

public class audiogenerator {

    /* Sound quality settings */
    private final int fundHz = 220; // fundamental frequency for audio generation
    private final int fade_len = 4000; // length of fade through in samples
    int cons_len = 5;
    private final double[] RelConsf = {1, 2, 3, 4, 6}; // consonant sounding relative frequency scalings
    int dis_len = 5;
    private final double[] RelDisf = {1, 1.33333, 3.162544, 4.5656777, 6.756476}; // dissonant sounding relative frequency scalings

    /* State tracking variables */
    private int fade_start = 0; // start of the last fade
    private int min_buf; // minimum number of samples needed to play
    private AudioTrack track; // the audiotrack used to play audio
    private short[] samples; // the samples to play (size of min_buf)
    private int delta = 0; // variable used to track the current phase position

    /* Sound shaping variables (could be volatile to communicate with the generator?) */
    public boolean good = true;
    public double[] scaling_new = {1, 0.75, 0.5, 0.1, 0.1}; // new amplitude scaling values
    public double[] scaling_old = {0.1, 0.1, 0.5, 0.75, 1}; // old amplitude scaling values

    /* Variables to be removed in actual implementation */
    private int count = 0;

    public audiogenerator() {
        this.min_buf = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        this.track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                min_buf, AudioTrack.MODE_STREAM);
        this.samples = new short[min_buf];
    }

    public void playsound() {
        /* Constant loop */
        while(stopped) {

            /* Read in the data from other thread */
            //good = true; // consonance switch
            System.arraycopy(scaling_new, 0, scaling_old, 0, scaling_new.length); // copy new into old
            // read in new amplitude scaling
            // BEGINNING OF DUMMY BLOCK
            if (good) {
                if (count == 0 || count == 10 || count == 20) {
                    for (int k = 0; k < 5; k++) {
                        scaling_new[k] = Math.abs(Math.random());
                    }
                }
                count++;
                if (count > 30) {
                    count = 0;
                    good = false;
                }
            } else {
                if (count == 0 || count == 10 || count == 20) {
                    for (int k = 0; k < 5; k++) {
                        scaling_new[k] = Math.abs(Math.random());
                    }
                }
                count++;
                if (count > 30) {
                    count = 0;
                    good = true;
                }
            }
            // END OF DUMMY BLOCK

            /* Compute values to play */
            this.fade_start = this.delta; // begin amplitude fade through
            for (int i = 0; i < this.min_buf; i += 2) {
                double sum = 0.0; // value to save as a sample
                if (this.good) {
                    double[] pts = new double[cons_len];
                    for (int j = 0; j < this.cons_len; j++) {
                        pts[j] = this.delta / ((float) 44100 / (this.fundHz * RelConsf[j])) * 2.0 * Math.PI;
                        pts[j] = Math.sin(pts[j]); // -1.0 < pts[j] < 1.0
                        if (this.delta - this.fade_start < this.fade_len) { // are we fading?
                            double old_scale = scaling_old[j] * (double) (fade_start + fade_len - delta) / fade_len;
                            double new_scale = scaling_new[j] * (double) (delta - fade_start) / fade_len;
                            sum += pts[j] * (new_scale + old_scale) / this.cons_len;
                        } else
                            sum += pts[j] * scaling_new[j] / this.cons_len;
                    }
                } else {
                    double[] pts = new double[dis_len];
                    for (int j = 0; j < this.dis_len; j++) {
                        pts[j] = this.delta / ((float) 44100 / (this.fundHz * RelDisf[j])) * 2.0 * Math.PI;
                        pts[j] = Math.sin(pts[j]); // -1.0 < pts[j] < 1.0
                        if (this.delta - this.fade_start < this.fade_len) { // are we fading?
                            double old_scale = scaling_old[j] * (double) (fade_start + fade_len - delta) / fade_len;
                            double new_scale = scaling_new[j] * (double) (delta - fade_start) / fade_len;
                            sum += pts[j] * (new_scale + old_scale) / this.dis_len;
                        } else
                            sum += pts[j] * scaling_new[j] / this.dis_len;
                    }
                }
                // clamp the sum value at abs(1.0)
                if (sum > 1.0)
                    sum = 1.0;
                else if (sum < -1.0)
                    sum = -1.0;

                /* Save the sample values to play */
                this.samples[i + 0] = (short) (sum * 32000);
                this.samples[i + 1] = (short) (sum * 32000);

                /* Increment the generation variable */
                this.delta++;
            }

            /* Begin playing the values */
            this.track.write(this.samples, 0, this.min_buf);
            this.track.play();
        }
    }
}
