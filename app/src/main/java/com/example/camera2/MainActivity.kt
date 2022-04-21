package com.example.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
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

        logE("设备旋转角度 ${display?.rotation}")

        cm.cameraIdList.map {
            val cc = cm.getCameraCharacteristics(it)
            val orientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION) // 图像传感器方向
            logE("相机 ${it} - [${orientation}]")
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
            image.close()
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            with(binding.mainPic) {
                visibility = View.VISIBLE
                // 处理图片旋转问题
                setImageBitmap(adjustPhotoRotation(bitmap, 90))
            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos)
        }


    }

    fun adjustPhotoRotation(bm: Bitmap, orientationDegree: Int): Bitmap? {
        val m = Matrix()
        m.setRotate(orientationDegree.toFloat(), bm.width.toFloat() / 2, bm.height.toFloat() / 2)
        val targetX: Float
        val targetY: Float
        if (orientationDegree == 90) {
            targetX = bm.height.toFloat()
            targetY = 0f
        } else {
            targetX = bm.height.toFloat()
            targetY = bm.width.toFloat()
        }
        val values = FloatArray(9)
        m.getValues(values)
        val x1 = values[Matrix.MTRANS_X]
        val y1 = values[Matrix.MTRANS_Y]
        m.postTranslate(targetX - x1, targetY - y1)
        val bm1 = Bitmap.createBitmap(bm.height, bm.width, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        val canvas = Canvas(bm1)
        canvas.drawBitmap(bm, m, paint)
        return bm1
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
                            _startPreview()
                        }
                    },
                    null
                )
            }
        }


    }

    private fun _startPreview() {
        cDevice?.let { camera->

            ccSession?.let { session->
                // 设置预览时连续捕获图片数据 ==> 开始预览

                // 构建 CaptureRequest
                val crb = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                crb.addTarget(surface)

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
                        }

                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            super.onCaptureCompleted(session, request, result)
                        }

                        override fun onCaptureFailed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure
                        ) {
                            super.onCaptureFailed(session, request, failure)
                        }

                        override fun onCaptureProgressed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            partialResult: CaptureResult
                        ) {
                            super.onCaptureProgressed(session, request, partialResult)
                        }

                        override fun onCaptureSequenceAborted(
                            session: CameraCaptureSession,
                            sequenceId: Int
                        ) {
                            super.onCaptureSequenceAborted(session, sequenceId)
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
                        }

                        override fun onCaptureBufferLost(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            target: Surface,
                            frameNumber: Long
                        ) {
                            super.onCaptureBufferLost(session, request, target, frameNumber)
                        }

                    },
                    previewHandler
                )

                isPreview = true
            }

        }
    }

    lateinit var surface:Surface
    private fun startPreview() {

        val st = binding.mainTv.surfaceTexture
        st?.let {

            surface = Surface(st)
            val targets = mutableListOf(surface)
            imageReader?.let {
                targets.add(it.surface)
            }

            cDevice?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {

                    // 保存 session
                    ccSession = session

                    _startPreview()

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }
            }, cameraHandler)

        }


    }

}