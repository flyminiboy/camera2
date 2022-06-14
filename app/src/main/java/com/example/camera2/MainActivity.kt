package com.example.camera2

import android.content.Intent
import android.opengl.EGL14
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.camera2.databinding.ActivityMainBinding
import javax.microedition.khronos.egl.EGL10


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
                startActivity(Intent(this@MainActivity, PlayActivity::class.java))
            }

            mainTestegl.setOnClickListener {
                test()
            }

            mainMovie.setOnClickListener {
                startActivity(Intent(this@MainActivity, MovieActivity::class.java))
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
        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )

    }

}

