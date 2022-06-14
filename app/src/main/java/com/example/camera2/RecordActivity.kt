package com.example.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.*
import android.media.MediaCodec.createPersistentInputSurface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.camera2.databinding.ActivityMainBinding
import com.example.camera2.databinding.ActivityRecordBinding
import java.io.File
import java.util.*


class RecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordBinding


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

    private var isRecord = false
    private var codec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrack: Int = 0
    private var path: String? = null
    private var startMux = false

    private val encoderThread = HandlerThread("encode")
    private val encoderHandler by lazy {
        encoderThread.start()
        Handler(encoderThread.looper)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater).apply {
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
                mainRecordVideo.text = if (!isRecord) {
                    startRecord()
                    "停止录制"
                } else {
                    stopRecord()
                    "开始录制"
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
            @RequiresApi(Build.VERSION_CODES.M)
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
        imageReader?.let { reader ->
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
                val sign = -1 // 后置 1是前置
                val deviceOrientationDegrees = display.rotation
                val sensorOrientationDegrees = 90 // 后置90 前置270 可以通过
//                val cc = cm.getCameraCharacteristics(it)
//                val orientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION)
                val rotation =
                    (sensorOrientationDegrees - deviceOrientationDegrees * sign + 360) % 360
                logE("save $deviceOrientationDegrees - $sensorOrientationDegrees - $rotation")
                setImageBitmap(rotateBitmapByDegree(bitmap, rotation))
//                setImageBitmap(bitmap)
            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos)
        }


    }

    fun rotateBitmapByDegree(bm: Bitmap, degree: Int): Bitmap? {
        var returnBm: Bitmap? = null
        // 根据旋转角度，生成旋转矩阵
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        } catch (e: OutOfMemoryError) {
        }
        if (returnBm == null) {
            returnBm = bm
        }
        if (bm != returnBm) {
            bm.recycle()
        }
        return returnBm
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

                            _startPreview()
                        }
                    },
                    null
                )
            }
        }


    }

    private fun _startPreview() {
        cDevice?.let { camera ->

            ccSession?.let { session ->
                // 设置预览时连续捕获图片数据 ==> 开始预览

                // 构建 CaptureRequest
                val crb = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                crb.addTarget(surface)
                if (startToRecord) {
                    codecSurface?.apply {
                        crb.addTarget(this)
                    }
                }

                session.setRepeatingRequest(
                    crb.build(),
                    null,
                    previewHandler
                )

                isPreview = true

                if (startToRecord) {
                    _startRecord()
                    startToRecord = false
                }
            }

        }
    }

    lateinit var surface: Surface
    @RequiresApi(Build.VERSION_CODES.M)
    private fun startPreview() {

        val st = binding.mainTv.surfaceTexture
        st?.let {

            surface = Surface(st) // 预览
            val targets = mutableListOf(surface)
            imageReader?.apply { // 拍照
                targets.add(this.surface)
            }
            // 创建持久surface
            codecSurface = createPersistentInputSurface()
            codecSurface?.apply {
                logE("PersistentInputSurface ${isValid}")
                targets.add(this)
            }

            configEncoder()

            cm.cameraIdList.map {
                val cc = cm.getCameraCharacteristics(it)
                val orientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION) // 图像传感器方向
                logE("相机 ${it} - [${orientation}]")
                val streamConfigurationMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val size = streamConfigurationMap?.run {
                    getOutputSizes(SurfaceTexture::class.java)[0]
                }?.apply {
                    st.setDefaultBufferSize(width, height)
                }

            }

            cDevice?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {

                    // 保存 session
                    ccSession = session

                    _startPreview()

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }

                override fun onClosed(session: CameraCaptureSession) {
                    super.onClosed(session)
                    logE("what fuck")
                }
            }, cameraHandler)

        }


    }

    var codecSurface: Surface? = null

    // MediaCodecList 获取所有可用的编解码器
    fun selectCodec(mimeType: String): List<MediaCodecInfo> {
        val mediaCodecInfos = mutableListOf<MediaCodecInfo>()
        MediaCodecList(MediaCodecList.ALL_CODECS).run {
             codecInfos.map { mediaCodecInfo->
                if (mediaCodecInfo.isEncoder) {
                    mediaCodecInfo.supportedTypes.map { type->
                        if (type.equals(mimeType)) {
                            mediaCodecInfos.add(mediaCodecInfo)
                        }
                    }
                }
            }
        }
        return mediaCodecInfos
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun configEncoder() { // 配置编码器


        // 创建编码器，根据指定的 MIME 类型
//        ● “video/x-vnd.on2.vp8” - VP8 video (i.e. video in .webm)
//        ● “video/x-vnd.on2.vp9” - VP9 video (i.e. video in .webm)
//        ● “video/avc” - H.264/AVC video
//        ● “video/mp4v-es” - MPEG4 video
//        ● “video/3gpp” - H.263 video
//        ● “audio/3gpp” - AMR narrowband audio
//        ● “audio/amr-wb” - AMR wideband audio
//        ● “audio/mpeg” - MPEG1/2 audio layer III
//        ● “audio/mp4a-latm” - AAC audio (note, this is raw AAC packets, not packaged in LATM!)
//        ● “audio/vorbis” - vorbis audio
//        ● “audio/g711-alaw” - G.711 alaw audio
//        ● “audio/g711-mlaw” - G.711 ulaw audio
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

        selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC).map {
            logE("${it.name}")
            val capabilities = it.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val vcap = capabilities.videoCapabilities
            logE("[${vcap.bitrateRange}]")
            logE("[${vcap.supportedWidths.lower} - ${vcap.supportedWidths.upper}]")
            logE("[${vcap.supportedHeights.lower} - ${vcap.supportedHeights.upper}]")
            logE("==============================================")
        }

        // MediaFormat 使用”key-value”键值对的形式存储多媒体数据格式信息 视频数据


        val mediaFormat = MediaFormat.createVideoFormat( //TODO  很重要，尤其是宽高要选择设备支持
            MediaFormat.MIMETYPE_VIDEO_AVC, 960, 720
        ).apply {
            setInteger( // 这个地方很重要，一定要配置对，使用 input buffer的方式 和 使用 input surface 不一样
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface // COLOR_FormatYUV420Flexible
            ) // 指明video编码器的颜色格式 具体选择哪种颜色格式与输入的视频数据源颜色格式有关
//            原始数据 编码器
//            NV12(YUV420sp) ———> COLOR_FormatYUV420PackedSemiPlanar
//            NV21 ———-> COLOR_FormatYUV420SemiPlanar
//            YV12(I420) ———-> COLOR_FormatYUV420Planar
            // w * h * fps * factory
            // 关于 factory
//            通常情况下，在网络流媒体使用场景中，可以将Factor设置为0.1~0.2，这样能在保证画面损失不严重的情况下生成出来的视频文件大小较小；
//            在普通本地浏览的使用场景中，可以将Factor设置为0.25~0.5，这样可以保证画面清晰度不会因为编码而造成过多肉眼可见的损失，这样生成出来的视频文件也相对较大；
//            在高清视频处理的使用场景中，可以将Factor设置为0.5以上
            // 不同设备的不同编码器会有一个支持的范围，不要超过最大值，否则可能引发crash configexception

            setInteger(MediaFormat.KEY_BIT_RATE, 500_000) // 比特率
            setInteger(MediaFormat.KEY_FRAME_RATE, 30) // 帧率
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2) // I帧间隔
        }

        codec?.apply {

            setCallback(EncodecCallback()) // 异步模式

            //  surface 表示解码器要渲染的 Surface
            // crypto用于指定一个MediaCrypto对象，以便对媒体数据进行安全解密
            // flags指明配置的是编码器
            configure(
                mediaFormat,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE
            ) // 配置以后才可以创建Surface
            // 配置之后，启动之前，创建surface

            mCodecStatus = 1


//            static class PersistentSurface extends Surface {
//            @SuppressWarnings("unused")
//            PersistentSurface() {} // used by native
//
//            @Override
//            public void release() {
//                native_releasePersistentInputSurface(this);
//                super.release();
//            }
//
//            private long mPersistentObject;
//        };

            codecSurface?.apply {
                logE("设置输入surface")
                setInputSurface(this) // API23 开始支持 使用已经创建好的持久性输入表面
            }

//createInputSurface()
//            codecSurface = createInputSurface()

            logE("完成编码器配置[${codecSurface?.isValid}]")

            //初始化 MediaMuxer（混合器） 将H264文件打包成MP4
            val file = File(filesDir, UUID.randomUUID().toString() + ".mp4")
            path = file.let { it ->
                mediaMuxer =
                    MediaMuxer(it.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                mediaMuxer?.setOrientationHint(90)
                it.absolutePath
            }

        }
    }

    /**
     * 手动记录编码器的状态 默认  Uninitialized -> 0
     *  reset
     *  stop
     * Uninitialized -> 0
     *  configure
     * Configured -> 1
     *  start
     * Executing -> 2
     *
     * Error -> -1
     *
     *  release
     * Released -> 3
     *
     */
    private var mCodecStatus: Int = 0

    private fun _startRecord() {
        cDevice?.let { camera ->

            ccSession?.let { session ->
                // 设置预览时连续捕获图片数据 ==> 开始预览

                session.stopRepeating()
                session.abortCaptures()

                // 构建 CaptureRequest
                val crb = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                crb.addTarget(surface)

                codecSurface?.apply {
                    crb.addTarget(this)
                }


                session.setRepeatingRequest(
                    crb.build(),
                    null,
                    null
                )

                isRecord = true
                codec?.start()
                logE("开始录制")

                mCodecStatus = 2
            }

        }
    }

    var startToRecord = false
    @RequiresApi(Build.VERSION_CODES.M)
    private fun startRecord() {
        if (isRecord) {
            return
        }

        // 判断编码器状态 如果已经是stop/release/reset 则再次进行config操作
        when (mCodecStatus) {
            1 -> {
                ccSession?.apply {
                    stopRepeating()
                    abortCaptures()
                }
                startToRecord = true
                _startPreview()
            }
            -1, 0 -> {
                configEncoder()

                ccSession?.apply {
                    stopRepeating()
                    abortCaptures()
                }
                startToRecord = true
                _startPreview()

            }
        }

    }

    private fun stopRecord() {
        if (!isRecord) {
            return
        }


        codec?.apply {
            signalEndOfInputStream() // 告知编码器我们要结束编码 这只能与从由 createInputSurface 创建的 Surface 接收输入的编码器一起使用。
            // mediacodec状态进入到 Uninitialized 需要再次进行config，才可以再次start
            stop()
            mCodecStatus = 0

            mediaMuxer?.apply { // 这个段代码不写，视频黑屏无法播放，报错 moov atom not found
                // 加一个状态判断
                if (startMux) {
                    stop()
                    startMux = false
                }
            }

            ccSession?.let { session ->
                session.stopRepeating()
                session.abortCaptures()
                _startPreview()
            }

            isRecord = false

            logE("video save [ $path ]")
        }

    }

    inner class EncodecCallback : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // TODO buffer 模式 接受到 YUV 数据
            // surface 模式不需要处理这部分代码
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {

            // 将编码好的H264数据进行封装

            logE("onOutputBufferAvailable")
            // Timed out while waiting for request to complete.
            if (!isRecord) { // 一定释放，否则会出现 camera request not completed
                // 释放输出数据缓冲区
                codec.releaseOutputBuffer(index, false)
                return
            }
            // 获取有效数据
            codec.getOutputBuffer(index)?.apply {
                if (info.size > 0) {

                    mediaMuxer?.let { mm ->
                        // 设置数据开始偏移量
                        position(info.offset)
                        // 设置数据长度
                        limit(info.offset + info.size)
                        // 混合器写到MP4文件中
                        mm.writeSampleData(videoTrack, this, info)
                        // 释放输出数据缓冲区
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            // TODO  重置编码器
            codec.reset()
            if (startMux) {
                mediaMuxer?.apply {
                    this.stop()
                }
            }
            mCodecStatus = -1
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            // 添加视频轨道
            logE("onOutputFormatChanged")
            mediaMuxer?.let { mm ->
                videoTrack = mm.addTrack(format)
                mm.start() // 开始工作
                startMux = true
                logE("mediaMuxer 开始工作")
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO 做资源释放操作
    }

}

