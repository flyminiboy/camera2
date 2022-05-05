package com.example.camera2

import android.content.Context
import android.opengl.GLES30
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception

fun loadShaderSource(context: Context, rawId: Int) =
    with(context) {
        val source = StringBuilder()

        val br =
            BufferedReader(InputStreamReader(BufferedInputStream(resources.openRawResource(rawId))))
        try {
            var line = br.readLine()
            while (line != null) {
                source.append(line)
                source.append("\n")
                line = br.readLine()
            }
        } catch (e: Exception) {
        } finally {
            br.close()
        }

        source.toString()
    }


fun loadShader(shaderType: Int, shaderSource: String) =
    // 创建着色器对象
    GLES30.glCreateShader(shaderType).also { shader ->

        // 将着色器源码附加到着色器上
        GLES30.glShaderSource(shader, shaderSource)
        GLES30.glCompileShader(shader)
        // 检测是否编译成功
        val compiled = intArrayOf(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == GLES30.GL_FALSE) {
            val error = GLES30.glGetShaderInfoLog(shader)
            logE("glCompileShader info [ $error ]")
            GLES30.glDeleteShader(shader)
            return GLES30.GL_FALSE
        }
    }


fun createAndLinkProgrm(context: Context, vertexShader: Int, fragmentShader: Int) =
    // 构建着色器程序对象
    GLES30.glCreateProgram().also { program ->



        // 将着色器附加到着色器程序
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        // 链接程序
        GLES30.glLinkProgram(program)
        // 检测链接状态
        val linked = intArrayOf(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0)
        if (linked[0] == GLES30.GL_FALSE) {
            val error = GLES30.glGetProgramInfoLog(program)
            logE("glCompileShader info [ $error ]")
            return GLES30.GL_FALSE
        }

        // 在把着色器对象链接到程序对象以后，记得删除着色器对象，我们不再需要它们了
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

