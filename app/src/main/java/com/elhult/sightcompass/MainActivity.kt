package com.elhult.sightcompass

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.elhult.sightcompass.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    val LOGTAG = "Tag"
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var binding: ActivityMainBinding
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                runCamera.value = true
            } else {
                getCameraAccess()
            }
        }
    private val hasCamAccess: Boolean
        get() = (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PermissionChecker.PERMISSION_GRANTED)

    val runCamera: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val compassValue = MutableLiveData<Double>(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // start the camera preview!
        runCamera.observe(this) {
            if (it) {
                initCameraPreview()
            }
        }
        if (!hasCamAccess) {
            getCameraAccess()
        } else {
            runCamera.value = true
        }

        // start the compass
        val sensorManager = getSystemService(SensorManager::class.java)
        compassValue.observe(this) {
            Log.v(LOGTAG, "New compass value: $it")
            binding.textBox.text = getString(R.string.degreeFormatter).format(it)

        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR ->
                            event.values?.let {
                                compassValue.value = Orientation.fromArray(it).compassVal
                            }
                        else -> { // ignore this case
                        }
                    }
                }
            }

            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
                // ignore these events
            }

        }
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(
            listener,
            orientationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }


    private fun getCameraAccess() {
        // the callback is set up so that it repeats until I get the permission
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun initCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview: Preview = Preview.Builder()
                .build()
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            preview.setSurfaceProvider(binding.previewView.surfaceProvider)
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
        }, ContextCompat.getMainExecutor(this))
    }
}