package com.example.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.*
import android.media.MediaCodec.createPersistentInputSurface
import android.opengl.EGL14
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
import java.io.File
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(this.root)

            mainCamera.setOnClickListener {
                startActivity(Intent(this@MainActivity, CameraActivity::class.java))
            }

            mainRecordVideo.setOnClickListener {
                startActivity(Intent(this@MainActivity, RecordActivity::class.java))
            }

            mainWaterMark.setOnClickListener {
                startActivity(Intent(this@MainActivity, WaterMarkActivity::class.java))
            }

            mainTestegl.setOnClickListener {
                test()
            }
        }

    }

    private fun test() {



//        static void *
//        fromEGLHandle(JNIEnv *_env, jmethodID mid, jobject obj) {
//            if (obj == NULL){
//                jniThrowException(_env, "java/lang/IllegalArgumentException",
//                    "Object is set to null.");
//            }
//            return (void*) (_env->CallIntMethod(obj, mid));
//        }



        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        logE("fuck ${null == EGL14.EGL_NO_SURFACE}")
        logE("fuck ${EGL14.EGL_NO_SURFACE}")
        logE("fuck ${EGL10.EGL_NO_SURFACE}")
        logE("fuck ${EGL10.EGL_NO_SURFACE == EGL14.EGL_NO_SURFACE}")
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)

    }

}

