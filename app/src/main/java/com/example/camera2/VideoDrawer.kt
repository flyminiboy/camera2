package com.example.camera2

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.properties.Delegates

class VideoDrawer : IDrawer {

    // 顶点坐标
    private val mVertexCoord = floatArrayOf(
        -1.0f, -1.0f, // 左下
        1.0f, -1.0f, // 右下
        1.0f, 1.0f, // 右上
        -1.0f, 1.0f // 左上
    )

    private val mTextureCoord = floatArrayOf(
        0.0f, 0.0f, // 左上
        1.0f, 0.0f, // 右上
        1.0f, 1.0f, // 右下
        0.0f, 1.0f // 左下
    )

    // 将CPU内存坐标数据映射到直接内存，便于和OpenGL ES 交互

    private val mVertexBuffer by lazy {
        ByteBuffer.allocateDirect(mVertexCoord.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(mVertexCoord)
                position(0)
            }
    }

    private val mTextureBuffer by lazy {
        ByteBuffer.allocateDirect(mTextureCoord.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(mTextureCoord)
                position(0)
            }
    }

    private var mTextureId: Int? = null
    var mSurfaceTexture: SurfaceTexture? = null
    private val transformMatrix = FloatArray(16)
    private var mSurfaceTextureListener: ((surface: SurfaceTexture) -> Unit)? = { _ -> }

    var mProgram = 0
    var mVertexShader = 0
    var mFragmentShader = 0

    var mOESTextureLocation = 0
    var mTextureTransformLocation = 0

    private fun init() {
        // 加载着色器
        mVertexShader = loadShader(GLES30.GL_VERTEX_SHADER, getVertexShader())
        mFragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, getFragmentShader())
        // 连接程序
        mProgram = createAndLinkProgrm(mVertexShader, mFragmentShader)

        mOESTextureLocation = GLES30.glGetUniformLocation(mProgram, "uTexture")
        mTextureTransformLocation = GLES30.glGetUniformLocation(mProgram, "textureTransform")
    }

    override fun draw() {
        mTextureId?.apply {

            // 设置清空屏幕所用的颜色 当调用glClear函数，清除颜色缓冲之后，整个颜色缓冲都会被填充为glClearColor里所设置的颜色
            GLES30.glClearColor(0.2f, 0.3f, 0.3f, 1.0f)
            // 我们可以通过调用glClear函数来清空屏幕的颜色缓冲,glClear它接受一个缓冲位(Buffer Bit)来指定要清空的缓冲,这里我们只关心颜色
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            GLES30.glUseProgram(mProgram)

            // 更新纹理
            mSurfaceTexture?.apply {
                updateTexImage() // updateTexImage()方法只能在包OpenGLES环境的线程里调用
                getTransformMatrix(transformMatrix)
            }

            // 启用顶点 TODO 可以使用VAO VBO 优化
            // 使用了位置限定符
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 2 * 4, mVertexBuffer)
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 2 * 4, mTextureBuffer)

            GLES30.glEnableVertexAttribArray(0)
            GLES30.glEnableVertexAttribArray(1)

            // 激活纹理
            TextureUtil.activeateTeture(this)
            GLES30.glUniformMatrix4fv(mTextureTransformLocation, 1, false, transformMatrix, 0)
//            GLES30.glUniform1i(mOESTextureLocation, 0) // TODO

            // 绘制
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)

            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
        }
    }

    override fun release() {

    }

    override fun bindTextureId(textureId: Int) {
        logE("bindTextureId")
        init()
        mTextureId = textureId.apply {
            mSurfaceTexture = SurfaceTexture(this).apply {
                onSurfaceTextureAvailable(this)
            }
        }
    }

    private fun onSurfaceTextureAvailable(surface: SurfaceTexture) {
        mSurfaceTextureListener?.apply {
            this.invoke(surface)
        }
    }

    fun setSurfaceTextureAvailableLisener(listener: ((surface: SurfaceTexture) -> Unit)? = null) {
        mSurfaceTextureListener = listener
    }

    private fun getVertexShader(): String {
        return "#version 300 es\n" +
                "\n" +
                "layout (location = 0) in vec4 vPosition;\n" +
                "layout (location = 1) in vec4 tPosition;\n" +
                "uniform mat4 textureTransform;\n" +
                "out vec2 vCoordinate;\n" +
                "void main() {\n" +
                "gl_Position = vPosition;\n" +
                "vCoordinate = (textureTransform * tPosition).xy;\n" +
                " }\n"
    }

    //视频片元着色器
    private fun getFragmentShader(): String {
        //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
        return  "#version 300 es\n" +
                "#extension GL_OES_EGL_image_external_essl3:require\n" + // 太坑了 3.0中将GL_OES_EGL_image_external变为了GL_OES_EGL_image_external_essl3 在使用纹理扩展的时候，也就是uniform samplerExternalOES sTexture的时候。在3.0中我们使用GL_OES_EGL_image_external_essl3而不是GL_OES_EGL_image_external。
                "\n" +
                "precision mediump float;\n" +
                "in vec2 vCoordinate;\n" +
                "uniform samplerExternalOES uTexture;\n" +
                "out vec4 outColor;\n" +
                "void main() {\n" +
                "  outColor=texture(uTexture, vCoordinate);\n" +
                "}\n"
    }

}