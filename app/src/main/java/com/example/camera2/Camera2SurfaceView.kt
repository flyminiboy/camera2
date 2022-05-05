package com.example.camera2

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Camera2SurfaceView(context: Context) : GLSurfaceView(context) {

    init {

        setEGLContextClientVersion(3)
        setRenderer(Camera2Render())
        renderMode = RENDERMODE_WHEN_DIRTY

    }

    private var program: Int? = null
    private var oesTextureLoc: Int? = null
//    private var uTextureMatrixLocation: Int? = null

    private val textures = intArrayOf(1)
    private var mOESTextureId: Int? = null
    private val transformMatrix = FloatArray(16)

    var mSurfaceTexture: SurfaceTexture? = null
    private var surfaceTextureListener: ((surface: SurfaceTexture) -> Unit)? = { _ -> }

    private val vertexCoords = floatArrayOf(
        -1.0f, -1.0f, // 左下
        1.0f, -1.0f, // 右下
        1.0f, 1.0f, // 右上
        -1.0f, 1.0f // 左上
    )

    private val textureCoords = floatArrayOf(
        0.0f, 1.0f, // 左上
        1.0f, 1.0f, // 右上
        1.0f, 0.0f, // 右下
        0.0f, 0.0f // 左下
    )

    private val vertexCoordsBuffer by lazy {
        ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexCoords)
                position(0)
            }
    }

    private val textureBuffer by lazy {
        ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(textureCoords)
                position(0)
            }
    }

    inner class Camera2Render : Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

            // 加载shader，链接程序
            val vertexShader = loadShaderSource(context, R.raw.vertex_shader).run {
                loadShader(GLES30.GL_VERTEX_SHADER, this)
            }
            val fragmentShader = loadShaderSource(context, R.raw.fragment_shader).run {
                loadShader(GLES30.GL_FRAGMENT_SHADER, this)
            }

            program = createAndLinkProgrm(context, vertexShader, fragmentShader)


            // 创建并绑定纹理
            GLES30.glGenTextures(1, textures, 0)
            mOESTextureId = textures[0].apply {
                GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, this)
                mSurfaceTexture = SurfaceTexture(this)
                mSurfaceTexture?.apply {
                    onSurfaceTextureAvailable(this)
                    setOnFrameAvailableListener {
                        requestRender()
                    }
                }
            }

            // 为当前绑定的纹理对象设置环绕、过滤方式
            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_REPEAT
            )
            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_REPEAT
            )
            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )


        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES30.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {

            // 绘制
            program?.let {
                // 设置清空屏幕所用的颜色 当调用glClear函数，清除颜色缓冲之后，整个颜色缓冲都会被填充为glClearColor里所设置的颜色
                GLES30.glClearColor(0.2f, 0.3f, 0.3f, 1.0f)
                // 我们可以通过调用glClear函数来清空屏幕的颜色缓冲,glClear它接受一个缓冲位(Buffer Bit)来指定要清空的缓冲,这里我们只关心颜色
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

                // 激活程序对象
                // 在glUseProgram函数调用之后，每个着色器调用和渲染调用都会使用这个程序对象（也就是之前写的着色器)了
                GLES30.glUseProgram(it)

                mSurfaceTexture?.apply {
                    updateTexImage()
                    getTransformMatrix(transformMatrix)
                }

                GLES30.glVertexAttribPointer(
                    0,
                    2,
                    GLES30.GL_FLOAT,
                    false,
                    2 * 4,
                    vertexCoordsBuffer
                )
                GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 2 * 4, textureBuffer)

                GLES30.glEnableVertexAttribArray(0)
                GLES30.glEnableVertexAttribArray(1)

                oesTextureLoc = GLES30.glGetUniformLocation(it, "sTexture")
//                uTextureMatrixLocation = GLES30.glGetUniformLocation(it, "uTextureMatrix")

                GLES30.glActiveTexture(GLES30.GL_TEXTURE0) // 在绑定纹理之前先激活纹理单元 纹理单元GL_TEXTURE0默认总是被激活
                mOESTextureId?.apply {
                    // 绑定纹理
                    GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, this)
                    oesTextureLoc?.apply {
                        GLES30.glUniform1i(this, 0)
                    }
//                    uTextureMatrixLocation?.apply {
//                        GLES30.glUniformMatrix4fv(this, 1, false, transformMatrix, 0)
//                    }

                }

                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)

                GLES30.glDisableVertexAttribArray(0)
                GLES30.glDisableVertexAttribArray(1)

            }
        }

    }

    private fun onSurfaceTextureAvailable(surface: SurfaceTexture) {
        surfaceTextureListener?.apply {
            this.invoke(surface)
        }
    }

    fun setSurfaceTextureAvailableLisener(listener: ((surface: SurfaceTexture) -> Unit)? = null) {
        surfaceTextureListener = listener
    }

}