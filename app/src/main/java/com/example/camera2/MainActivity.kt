package com.example.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.camera2.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private val cameraThread = HandlerThread("camera").apply {
        start()
    }
    private val cameraHandler = Handler(cameraThread.looper)

    private val previewThread = HandlerThread("preview").apply {
        start()
    }
    private val previewHandler = Handler(previewThread.looper)

    private var isPreview = false

    private val cm by lazy {
        applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var cDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var ccSession: CameraCaptureSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(this.root)

            this.mainTakePhoto.setOnClickListener {
                if (!isPreview) {
                    return@setOnClickListener
                }
                takePhoto()
            }

            this.mainRecordVideo.setOnClickListener {
                if (!isPreview) {
                    return@setOnClickListener
                }

            }

            this.mainTv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    if (hasCameraPermission()) {
                        openCamera()
                    } else {
                        requestCameraPermission()
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return true;
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

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

        /**
         * 设置图片尺寸，这里图片的话，选择最大的分辨率即可
         */
        imageReader = ImageReader.newInstance(
            1920, 1080, ImageFormat.JPEG, 1
        ).apply {
            setOnImageAvailableListener({ reader ->

            }, null)
        }

        cm.openCamera("0", object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) { // 摄像头已经打开
                logE(Thread.currentThread().name + " [ onOpened ]")
                cDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                logE(Thread.currentThread().name + " [ onDisconnected ]")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                logE(Thread.currentThread().name + " [ onError ]")
            }

        }, cameraHandler)


    }

    private fun savePic() {
        imageReader?.let { reader->
            val image = reader.acquireNextImage()
            val planes = image.planes
            val buffer = planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            with(binding.mainPic) {
                visibility = View.VISIBLE
                // TODO 处理图片旋转问题
                setImageBitmap(bitmap)
            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos)
        }
    }

    private fun takePhoto() {
        cDevice?.let { camera ->
            ccSession?.let { session ->

                // 构建拍照 CaputreRequest
                val tprBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE) // 拍照模式
                imageReader?.let { reader ->
                    tprBuilder.addTarget(reader.surface)
                }

                // 拍照之前停止预览
                session.stopRepeating();

                session.capture(
                    tprBuilder.build(),
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            super.onCaptureCompleted(session, request, result)
                            logE("take photo onCaptureCompleted")
                            savePic()
                            // TODO 恢复预览
                        }
                    },
                    null
                )
            }
        }

    }

    private fun startPreview() {

        val st = binding.mainTv.surfaceTexture
        st?.let {

            val surface = Surface(st)
            val targets = mutableListOf(surface)
            imageReader?.let {
                targets.add(it.surface)
            }

            cDevice?.let { camera ->

                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {

                        // 保存 session
                        ccSession = session

                        // 构建 CaptureRequest
                        val crb = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        crb.addTarget(surface)

                        // 发送请求
                        // 预览
                        // 拍照

                        // //设置预览时连续捕获图片数据 ==> 开始预览
                        session.setRepeatingRequest(
                            crb.build(),
                            object : CameraCaptureSession.CaptureCallback() {

                                override fun onCaptureStarted(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    timestamp: Long,
                                    frameNumber: Long
                                ) {
                                    super.onCaptureStarted(session, request, timestamp, frameNumber)
                                    logE(Thread.currentThread().name + " [ onCaptureStarted ]")
                                }

                                override fun onCaptureCompleted(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    result: TotalCaptureResult
                                ) {
                                    super.onCaptureCompleted(session, request, result)
                                    logE(Thread.currentThread().name + " [ onCaptureCompleted ]")
                                }

                                override fun onCaptureFailed(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    failure: CaptureFailure
                                ) {
                                    super.onCaptureFailed(session, request, failure)
                                    logE(Thread.currentThread().name + " [ onCaptureFailed ]")
                                }

                                override fun onCaptureProgressed(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    partialResult: CaptureResult
                                ) {
                                    super.onCaptureProgressed(session, request, partialResult)
                                    logE(Thread.currentThread().name + " [ onCaptureProgressed ]")
                                }

                                override fun onCaptureSequenceAborted(
                                    session: CameraCaptureSession,
                                    sequenceId: Int
                                ) {
                                    super.onCaptureSequenceAborted(session, sequenceId)
                                    logE(Thread.currentThread().name + " [ onCaptureSequenceAborted ]")
                                }

                                override fun onCaptureSequenceCompleted(
                                    session: CameraCaptureSession,
                                    sequenceId: Int,
                                    frameNumber: Long
                                ) {
                                    super.onCaptureSequenceCompleted(
                                        session,
                                        sequenceId,
                                        frameNumber
                                    )
                                    logE(Thread.currentThread().name + " [ onCaptureSequenceAborted ]")
                                }

                                override fun onCaptureBufferLost(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    target: Surface,
                                    frameNumber: Long
                                ) {
                                    super.onCaptureBufferLost(session, request, target, frameNumber)
                                    logE(Thread.currentThread().name + " [ onCaptureBufferLost ]")
                                }

                            },
                            previewHandler
                        )

                        isPreview = true

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }
                }, cameraHandler)

            }

        }


    }

}