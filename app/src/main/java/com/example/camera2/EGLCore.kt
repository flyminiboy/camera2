package com.example.camera2

import android.opengl.*
import android.opengl.EGLExt.EGL_OPENGL_ES3_BIT_KHR
import android.opengl.EGLExt.EGL_RECORDABLE_ANDROID
import android.view.Surface
import java.lang.RuntimeException

class EGLCore {

    companion object {

        // 一维数组表示 k-v结构
        // 2*index = 2*index+1 (index>=0&&index<length)
        private val CONFIG_ANDROID = intArrayOf(
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_RED_SIZE, 8, // 颜色缓冲区
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR, // EGLConfig 支持的渲染接口
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT, // 指示帧缓冲区配置必须支持的 EGL 表面类型和功能
            EGL_RECORDABLE_ANDROID, 1, // 指示 EGLConfig 是否支持 渲染到将图像记录到视频的 ANativeWindow 布尔值
//            public static final int EGL_FALSE                          = 0;
//            public static final int EGL_TRUE                           = 1;
            // 1 表示 TRUE
            EGL14.EGL_NONE // 固定结尾标识
        )

        private val CONFIG_Pbuffer_ANDROID = intArrayOf(
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_RED_SIZE, 8, // 颜色缓冲区
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR, // EGLConfig 支持的渲染接口
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, // 指示帧缓冲区配置必须支持的 EGL 表面类型和功能
            EGL_RECORDABLE_ANDROID, 1, // 指示 EGLConfig 是否支持 渲染到将图像记录到视频的 ANativeWindow 布尔值
//            public static final int EGL_FALSE                          = 0;
//            public static final int EGL_TRUE                           = 1;
            // 1 表示 TRUE
            EGL14.EGL_NONE // 固定结尾标识
        )

        private val ATTRIBUTE_LIST = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE
        )

    }

    private var mEGLDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun init(surface: Surface?=null, width:Int = 0, height:Int = 0) {

        // 创建EGLDisplay
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY).apply {

            if (this == EGL14.EGL_NO_DISPLAY) {
                throw RuntimeException("unable to get EGL14 display")
            }


            val version = IntArray(2)
            // 初始化EGL
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                mEGLDisplay = EGL14.EGL_NO_DISPLAY
                getEGLError()
            }

            if (surface == null) {
                createPBufferSurface(width, height)
            } else {
                createWindowSurface(surface)
            }
        }

    }

    private fun createWindowSurface(surface: Surface) {

        checkEGLDisplay()

        releaseSurface()

        // 确定可用表面
        // 1. 查询每个表面配置，找出最好的选择
        // 2. 指定一组需求，让EGL推荐最佳匹配   推荐使用这种方式

        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            val eglConfigs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(
                mEGLDisplay,
                CONFIG_ANDROID,
                0,
                eglConfigs,
                0,
                eglConfigs.size,
                numConfigs,
                0
            )

            val eglConfig = eglConfigs[0]

            val attribList = intArrayOf(
                EGL14.EGL_NONE
            )
            // 创建可用窗口
            // eglCreateWindowSurface 接受单一属性，该属性用于指定所要渲染的表面是前台缓冲区还是后台缓冲区
            // EGL_RENDER_BUFFER -> EGL_SINGLE_BUFFER EGL_BACK_BUFFER（默认）
            // window surface 是 back buffer
            // surface 理解为 front buffer
            // 双缓冲机制
            // swap
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, eglConfig, surface, attribList, 0)


            mEGLContext = EGL14.eglCreateContext(
                mEGLDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT, // 表示没有共享，不和其他上下文共享资源
                ATTRIBUTE_LIST, // 指定上下文使用的属性列表，只有一个可接受的属性 - EGL_CONTEXT_FLIENT_VERSION 指定所使用的OpenGL ES 版本
                0
            )
        }


    }

    fun createPBufferSurface(width: Int, height: Int) {
        checkEGLDisplay()

        releaseSurface()

        // 确定可用表面
        // 1. 查询每个表面配置，找出最好的选择
        // 2. 指定一组需求，让EGL推荐最佳匹配   推荐使用这种方式

        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            val eglConfigs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(
                mEGLDisplay,
                CONFIG_Pbuffer_ANDROID,
                0,
                eglConfigs,
                0,
                eglConfigs.size,
                numConfigs,
                0
            )

            val eglConfig = eglConfigs[0]

            // 属性
            // EGL_WIDTH (单位像素)
            // EGL_HEIGHT (单位像素)
            // EGL_LARGEST_PBUFFER 如果请求的大小不可用，旋转最大的可用pbuffer 布尔值，EGL_TRUE EGL_FALSE
            val attribList = intArrayOf(
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_LARGEST_PBUFFER, EGL14.EGL_TRUE,
                EGL14.EGL_NONE
            )
            // 创建离屏渲染表面
            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, eglConfig, attribList, 0)


            mEGLContext = EGL14.eglCreateContext(
                mEGLDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT, // 表示没有共享，不和其他上下文共享资源
                ATTRIBUTE_LIST, // 指定上下文使用的属性列表，只有一个可接受的属性 - EGL_CONTEXT_FLIENT_VERSION 指定所使用的OpenGL ES 版本
                0
            )
        }

    }

    /**
     * 指定当前上下文
     *
     * 关联特定的EGLContext和渲染表面
     */
    fun makeCurrent(surface: EGLSurface) {
        checkEGLDisplay()
        checkEGLSurface()
        // 指定EGL显示连接
        // 指定EGL绘图表面
        // 指定EGL读取表面
        // 指定连接到该表面的渲染上下文
        if (!EGL14.eglMakeCurrent(mEGLDisplay, surface, surface, mEGLContext)) {
            getEGLError()
        }
    }

    fun swap() {

        checkEGLDisplay()
        checkEGLSurface()

        EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)
    }

    fun release() {
        checkEGLDisplay()
        checkEGLSurface()

        EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(mEGLDisplay)
        mEGLSurface = EGL14.EGL_NO_SURFACE
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLDisplay = EGL14.EGL_NO_DISPLAY

    }

    private fun releaseSurface() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY && mEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            mEGLSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun checkEGLDisplay() {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("no display, please call init first")
        }
    }

    private fun checkEGLSurface() {
        if (mEGLSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("no surface, please call init first")
        }
    }

    private fun getEGLError() {
        when (val error = EGL14.eglGetError()) {
            EGL14.EGL_SUCCESS -> return
            else -> throw RuntimeException(GLUtils.getEGLErrorString(error))
        }
    }

}