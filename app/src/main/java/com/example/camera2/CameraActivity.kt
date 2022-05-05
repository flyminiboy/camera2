package com.example.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.example.camera2.databinding.ActivityCameraBinding
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityCameraBinding

    private val mCameraManager by lazy {
        applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private var mCameraDevice: CameraDevice? = null
    private var mCameraCaptureSession:CameraCaptureSession?=null
    private var mSurface: Surface?=null

    private lateinit var cameraPreview:Camera2SurfaceView

    private val mCameraThread = HandlerThread("camera")
    private val mCameraHandler by lazy {
        mCameraThread.start()
        Handler(mCameraThread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityCameraBinding.inflate(layoutInflater).apply {

            cameraPreview = Camera2SurfaceView(this@CameraActivity)
            root.addView(cameraPreview)
            setContentView(root)

            cameraPreview.setSurfaceTextureAvailableLisener {
                if (hasCameraPermission()) {
                    openCamera()
                } else {
                    requestCameraPermission()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Takes the user to the success fragment when permission is granted
                openCamera()
            } else {
                toast("Permission request denied")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        // 默认开启后置摄像头

        mCameraManager.openCamera("0", object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) { // 摄像头已经打开
                mCameraDevice = camera
                createSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }

        }, mCameraHandler)

    }

    private fun createSession() {
        // 预览
        val targets = Surface(cameraPreview.mSurfaceTexture).run {
            mSurface = this
            mutableListOf(this)
        }

        mCameraDevice?.apply {
            createCaptureSession(targets, object :CameraCaptureSession.StateCallback(){
                override fun onConfigured(session: CameraCaptureSession) {
                    mCameraCaptureSession = session
                    startPreivew()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }

            }, mCameraHandler)
        }
    }

    private fun startPreivew() {
        mCameraDevice?.apply {
            mCameraCaptureSession?.apply {
                val request = createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).run {
                    mSurface?.apply {
                        addTarget(this)
                    }
                    build()
                }

                setRepeatingRequest(request, null, mCameraHandler)
            }
        }
    }

}