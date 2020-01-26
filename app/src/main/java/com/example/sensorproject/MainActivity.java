package com.example.sensorproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements  SensorEventListener{

    ToggleButton toggle;

    private float lastX, lastY, lastZ;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    /*private PlaySound soundPlayer;
    private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;
    private long[] pattern = {50};*/
    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;
    private boolean buttonPressed = false;
    private float vibrateThreshold = 0;

    //private TextView currentX, currentY, currentZ, maxX, maxY, maxZ;

    public Vibrator v;

    private final int duration = 3; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 440; // hz

    private final byte generatedSnd[] = new byte[2 * numSamples];
    Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initializeViews();

        toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    buttonPressed = true;
                    playSound();
                    onResume();
                } else {
                    // The toggle is disabled
                    buttonPressed = false;
                    audioTrack.stop();
                    onPause();
                }
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = 0;
        } else {
            // fail! we dont have an accelerometer!
        }

        //initialize vibration
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
    }
    /*public void initializeViews() {
        currentX.setText("0");
        currentY.setText("0");
        currentZ.setText("0");

        maxX.setText("0");
        maxY.setText("0");
        maxZ.setText("0");
    }*/

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                genTone();
                handler.post(new Runnable() {
                    public void run() {
                        playSound();
                    }
                });
            }
        });
        thread.start();
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // clean current values
        //displayCleanValues();
        // display the current x,y,z accelerometer values
        //displayCurrentValues();
        // display the max x,y,z accelerometer values
        //displayMaxValues();

        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);
        freqOfTone = (deltaX+deltaY+deltaZ)*50;
        // if the change is below 2, it is just plain noise
        if (deltaX < 2)
            deltaX = 0;
        if (deltaY < 2)
            deltaY = 0;
        if (deltaZ < 2)
            deltaZ = 0;
        if ((deltaX > vibrateThreshold) || (deltaY > vibrateThreshold) || (deltaZ > vibrateThreshold)) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));

        }
    }
    /*public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }

    // display the max x,y,z accelerometer values
    public void displayMaxValues() {
        if (deltaX > deltaXMax) {
            deltaXMax = deltaX;
            maxX.setText(Float.toString(deltaXMax));
        }
        if (deltaY > deltaYMax) {
            deltaYMax = deltaY;
            maxY.setText(Float.toString(deltaYMax));
        }
        if (deltaZ > deltaZMax) {
            deltaZMax = deltaZ;
            maxZ.setText(Float.toString(deltaZMax));
        }
    }*/
    public byte[] genTone(){
        // fill out the array
        byte generatedSnd[] = new byte[2 * numSamples];
        double sample[] = new double[numSamples];
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        return generatedSnd;
    }
    Thread noiseThread;
    AudioTrack audioTrack;
    Runnable generator = new Runnable()
    {
        public void run()
        {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            /* 8000 bytes per second, 1000 bytes = 125 ms */
            while(buttonPressed)
            {
                byte[] tone = genTone();
                audioTrack.write(tone, 0, tone.length);
            }

        }
    };

    void playSound(){
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 8000,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        noiseThread = new Thread(generator);
        noiseThread.start();
    }

}

