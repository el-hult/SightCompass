package com.elhult.sightcompass

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.elhult.sightcompass.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val sensorManager: SensorManager by lazy { getSystemService(SensorManager::class.java) }
    private lateinit var binding: ActivityMainBinding
    private val hasCamAccess: Boolean
        get() = (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PermissionChecker.PERMISSION_GRANTED)
    private val hasRotationVectorSensor: Boolean
        get() = (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
    private val compassValue = MutableLiveData(0.0)


    /* ***********************************************
           ACTIVITY LIFE CYCLE HANDLERS
     *********************************************** */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Camera Setup
        if (hasCamAccess) {
            startCamPreview()
        } else {
            getCamPermissionAndStart()
        }

        // Compass Setup
        compassValue.observe(this) {
            binding.textBox.text = getString(R.string.degreeFormatter).format(it)

        }
    }

    override fun onResume() {
        super.onResume()

        // Start Compass, if possible
        if (hasRotationVectorSensor) {
            val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            sensorManager.registerListener(
                this,
                orientationSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            AlertDialog.Builder(this)
                .setMessage(R.string.no_rot_vec_sensor_msg)
                .setCancelable(false)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()

        // Stop compass to save some power
        sensorManager.unregisterListener(this)
    }


    /* ***********************************************
            ROTATION VECTOR SENSING
     *********************************************** */
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // ignore these events
    }

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


    /* ***********************************************
                    CAMERA PREVIEW MANAGEMENT
    *********************************************** */

    /**
     * Nag the user for permission until they are granted
     * then also start the preview
     */
    private fun getCamPermissionAndStart() {
        val requestPermissionLauncher: ActivityResultLauncher<String> =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    startCamPreview()
                } else {
                    getCamPermissionAndStart()
                }
            }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Bind the camera to the PreviewView. The binding is also bound to the activity lifecycle
     * so I will not manually start/stop this binding in onPause and onResume
     */
    private fun startCamPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
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