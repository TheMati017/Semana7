package com.example.semana7;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String IMAGE_URL = "https://picsum.photos/600/400";

    private ImageView mImageView;
    private TextView tvEstadoImagen, tvProgreso, tvSensor;
    private Button btnDescargar, btnTarea;
    private ProgressBar progressBar;

    private SensorManager sensorManager;
    private Sensor sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mImageView = findViewById(R.id.imageView);
        tvEstadoImagen = findViewById(R.id.tvEstadoImagen);
        tvProgreso = findViewById(R.id.tvProgreso);
        tvSensor = findViewById(R.id.tvSensor);
        btnDescargar = findViewById(R.id.btnDescargar);
        btnTarea = findViewById(R.id.btnTarea);
        progressBar = findViewById(R.id.progressBar);

        btnDescargar.setOnClickListener(v -> descargarImagen());
        btnTarea.setOnClickListener(v -> iniciarTarea());

        // Sensor de vector de rotación
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (sensor == null) {
            tvSensor.setText(R.string.sensor_no_disponible);
        }
    }

    // ---------- 1. Thread: descarga de imagen ----------
    private void descargarImagen() {
        btnDescargar.setEnabled(false);
        tvEstadoImagen.setText("Descargando…");

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = loadImageFromNetwork(IMAGE_URL);
                // post() devuelve la actualización al hilo principal (UI)
                mImageView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bitmap != null) {
                            mImageView.setImageBitmap(bitmap);
                            tvEstadoImagen.setText("Imagen descargada");
                        } else {
                            tvEstadoImagen.setText("Error al descargar la imagen");
                        }
                        btnDescargar.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private Bitmap loadImageFromNetwork(String urlString) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.connect();
            try (InputStream in = conn.getInputStream()) {
                return BitmapFactory.decodeStream(in);
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ---------- 2. Thread: tarea larga con progreso ----------
    // AsyncTask está obsoleta (deprecated desde API 30); el reemplazo recomendado
    // es Thread/Executor + runOnUiThread, que es lo que se usa aquí.
    private void iniciarTarea() {
        btnTarea.setEnabled(false);
        progressBar.setProgress(0);

        new Thread(() -> {
            for (int i = 1; i <= 100; i++) {
                try {
                    Thread.sleep(50); // simula trabajo pesado
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                final int progreso = i;
                runOnUiThread(() -> {
                    progressBar.setProgress(progreso);
                    tvProgreso.setText("Progreso: " + progreso + "%");
                });
            }
            runOnUiThread(() -> {
                tvProgreso.setText("Tarea completada");
                btnTarea.setEnabled(true);
            });
        }).start();
    }

    // ---------- 3. Sensor: vector de rotación ----------
    @Override
    protected void onResume() {
        super.onResume();
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientation);

        tvSensor.setText(String.format(Locale.US,
                "Azimut (Z): %.1f°\nInclinación (X): %.1f°\nRotación (Y): %.1f°\n\n"
                        + "Vector: x=%.3f  y=%.3f  z=%.3f",
                Math.toDegrees(orientation[0]),
                Math.toDegrees(orientation[1]),
                Math.toDegrees(orientation[2]),
                event.values[0], event.values[1], event.values[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
