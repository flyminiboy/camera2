package com.example.camera2

import android.graphics.SurfaceTexture
import android.media.*
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.camera2.databinding.ActivityMovieBinding
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.N)
class MovieActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieBinding

    // 复用器
    var path = ""
    private val muxer by lazy {
        val file = File(filesDir, UUID.randomUUID().toString() + ".mp4")
        if (!file.exists()) {
            file.createNewFile()
        }
        path = file.absolutePath
        MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }
    private var videoTrack: Int = 0
    private var startMux = false

    // 解封装器
    private val extractor = MediaExtractor()
    lateinit var decoder: MediaCodec
    lateinit var encoder: MediaCodec
    private var trackId = 0

    private lateinit var cameraPreview: GLSurfaceView
    val playRender = PlayRender()
    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieBinding.inflate(layoutInflater).apply {
            setContentView(root)

            movieSurface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mSurface = holder.surface
                    initDecoder()
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {

                }

            })

        }
    }

    fun initDecoder() {

        extractor.setDataSource(assets.openFd("1.mp4"))
        val count = extractor.trackCount

        for (i in 0 until count) {
            val format = extractor.getTrackFormat(i)
            format.getString(MediaFormat.KEY_MIME)?.apply {
                if (startsWith("video")) { // 视频
                    //
                    trackId = i
                    createVideoDecoder(format, this)
//                    createVideoEncoder()
                    return@apply
                }
                if (startsWith("audio")) { // 音频
                    extractor.selectTrack(i)
                    createAudioDecoder(format, this)
                    return@apply
                }
            }


        }

    }

    var putCount = 0
    var takeCount = 0
    var startTime = 0L

    // 阻塞队列，存放接封装以后的数据 使用数组，控制大小，避免OOM
    private val videoQueue = ArrayBlockingQueue<ByteBuffer>(50)

    var startRead = false
    var isEnd = false

    /**
     * 异步读取
     */
    fun read(capacity: Int) {
        if (startRead) {
            return
        }
        startRead = true
        extractor.selectTrack(trackId)
        logE("解码-读取视频数据开始")
        thread { // 开启一个读编码数据的工作线程
            do {

                val buffer = ByteBuffer.allocate(capacity)

                buffer.clear()
                // 检索当前编码的样本并将其存储在从给定偏移量开始的字节缓冲区中
                // 返回-1 表示没有更多的样本数据可用
                val size = extractor.readSampleData(buffer, 0)

                if (size < 0) {
                    logE("没有有效的样本数据")
                    break
                }

                videoQueue.put(buffer)

                // 类似 迭代器 next
                // 前进到下一个样本，如果没有更多的样本数据可用返回FALSE
            } while (extractor.advance())
            logE("解码-读取视频数据结束")
            isEnd = true
        }
    }

    /**
     * 同步读取
     */
    fun readSync(buffer: ByteBuffer): Int {
        //先清空数据
        buffer.clear()
        //选择要解析的轨道
        extractor.selectTrack(trackId)
        //读取当前帧的数据 -1没有有效数据
        val size = extractor.readSampleData(buffer, 0)
        //进入下一帧
        extractor.advance() // 返回FALSE 没有有效数据 ==> 流结束
        return size
    }

    private fun createAudioDecoder(format: MediaFormat, mime: String) {

    }

    var width = 0
    var height = 0
    /**
     * 异步 + surface 模式
     *
     * 通过 EGL 创建一个离屏渲染的 surface
     * 然后通过共享纹理，实现数据的传递
     */
    private fun createVideoDecoder(format: MediaFormat, mime: String) {
        width = format.getInteger(MediaFormat.KEY_WIDTH)
        height = format.getInteger(MediaFormat.KEY_HEIGHT)

        logE("原始视频尺寸【${width} * ${height}】")

        decoder = MediaCodec.createDecoderByType(mime)
        decoder.setCallback(DecodecCallback()) //

        // surface 指定在其上呈现此解码器输出的表面
        // 只有在config的时候设置了surface，才可以调用 decoder.setOutputSurface() 设置新的输出表面
        decoder.configure(format, mSurface, null, 0)

        // 开始解码
        decoder.start()
        startTime = System.currentTimeMillis()
    }

    private fun createVideoEncoder() {
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, width, height
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.setCallback(EncodecCallback())

        // surface
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        // 配置编解码器（例如编码器）以使用持久输入表面代替输入缓冲区。 这只能在配置之后和启动之前调用
        EGLCore.getInstance().init(mSurface)
//        encoder.setInputSurface(EGLCore.getInstance().mEGLSurface)
        encoder.start()
        logE("编码器开始工作")


    }

    /**
     * 编码器 surface 是生产者
     * 解码器 surface 是消费者
     */

    inner class EncodecCallback:MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // surface 作为生产者，已经将数据输入到了编码器
            logE("onInputBufferAvailable")
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            logE("onOutputBufferAvailable")
            // 消费编码好的数据，通过 mediamuxer 进行封装
            // 如何判断结束
            // 获取有效数据
            codec.getOutputBuffer(index)?.apply {
                if (info.size > 0) {

                    muxer.let { mm ->
                        // 设置数据开始偏移量
                        position(info.offset)
                        // 设置数据长度
                        limit(info.offset + info.size)
                        // 混合器写到MP4文件中
                        mm.writeSampleData(videoTrack, this, info)
                        // 释放输出数据缓冲区
                        codec.releaseOutputBuffer(index, false)
                    }
                } else {
                    logE("傻傻哈哈 ${System.currentTimeMillis() - startTime}")
                }


                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    logE("编码结束 ${System.currentTimeMillis() - startTime}")
                    codec.stop()
                    muxer.stop()
                    startMux = false
                }
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            logE("onError ${e}")
            codec.reset()
            if (startMux) {
                muxer.apply {
                    this.stop()
                }
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            logE("onOutputFormatChanged")
            muxer.let { mm ->
                videoTrack = mm.addTrack(format)
                mm.start() // 开始工作
                startMux = true
                logE("mediaMuxer 开始工作 - ${path}")
            }
        }
    }


    inner class DecodecCallback : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // 将数据输入解码器

            val byteBuffer = codec.getInputBuffer(index) // 向缓存区获取一块内存

            byteBuffer?.apply {

                clear()

                read(capacity()) // 单例线程开始工作
                val size = videoQueue.take()?.run {
                    this@apply.put(this) // 填充数据
                    limit()
                } ?: 0

                if (isEnd && videoQueue.isEmpty()) {
                    logE("解码结束 ${System.currentTimeMillis() - startTime}")
                    // send EOS to decoder
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    extractor.release()
                    codec.stop()
                    codec.release()
                } else {
                    codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0) // 将填充数据的内容放回缓存区
                }


                // 获取数据并填充 暂时这么处理
//                val size = readBuffer(this)
//                if (size < 0) {
//                    codec.queueInputBuffer(index, 0, 0, extractor.sampleTime, 0) // 将填充数据的内容放回缓存区
//                    logE("释放资源${System.currentTimeMillis() - startTime}") // 28461
//                    extractor.release()
//                    codec.stop()
//                    codec.release()
//                } else {
//                    codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0) // 将填充数据的内容放回缓存区
//                }

            }


        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            // surface 已经消费了解码数据
            // 所以这个地方只需要 将缓冲区返回到编解码器或在输出表面上呈现它
//            codec.releaseOutputBuffer(index, true)
            codec.releaseOutputBuffer(index, false)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {

        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {

        }

    }

}
