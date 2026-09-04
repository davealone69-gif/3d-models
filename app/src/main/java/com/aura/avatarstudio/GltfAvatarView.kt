package com.aura.avatarstudio

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.aura.avatarstudio.renderer.GltfAvatarLoader
import com.aura.avatarstudio.renderer.HdAvatarRenderer

/** Touch-wired GL view with deterministic asynchronous avatar loading. */
class GltfAvatarView(
    private val context: Context,
    private val assetName: String,
    private val onAvatarLoadStateChanged: ((ready: Boolean, error: String?) -> Unit)? = null
) : GLSurfaceView(context) {

    private val renderer = HdAvatarRenderer(context)
    @Volatile private var loadStarted = false
    @Volatile private var surfaceReady = false

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser { egl, display ->
            val attribs = intArrayOf(
                javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE, 8,
                javax.microedition.khronos.egl.EGL10.EGL_GREEN_SIZE, 8,
                javax.microedition.khronos.egl.EGL10.EGL_BLUE_SIZE, 8,
                javax.microedition.khronos.egl.EGL10.EGL_ALPHA_SIZE, 8,
                javax.microedition.khronos.egl.EGL10.EGL_DEPTH_SIZE, 24,
                javax.microedition.khronos.egl.EGL10.EGL_STENCIL_SIZE, 8,
                javax.microedition.khronos.egl.EGL10.EGL_SAMPLE_BUFFERS, 1,
                javax.microedition.khronos.egl.EGL10.EGL_SAMPLES, 4,
                javax.microedition.khronos.egl.EGL10.EGL_NONE
            )
            val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            egl.eglChooseConfig(display, attribs, configs, 1, numConfigs)
            if (numConfigs[0] > 0) configs[0] else {
                val fallback = intArrayOf(
                    javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE, 8,
                    javax.microedition.khronos.egl.EGL_GREEN_SIZE, 8,
                    javax.microedition.khronos.egl.EGL_BLUE_SIZE, 8,
                    javax.microedition.khronos.egl.EGL_ALPHA_SIZE, 8,
                    javax.microedition.khronos.egl.EGL_DEPTH_SIZE, 24,
                    javax.microedition.khronos.egl.EGL_STENCIL_SIZE, 8,
                    javax.microedition.khronos.egl.EGL_NONE
                )
                egl.eglChooseConfig(display, fallback, configs, 1, numConfigs)
                configs[0]
            }
        }
        preserveEGLContextOnPause = true
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(
                gl: javax.microedition.khronos.opengles.GL10?,
                config: javax.microedition.khronos.egl.EGLConfig?
            ) {
                renderer.onSurfaceCreated(gl, config)
                surfaceReady = true
                loadStarted = false
                notifyState(false, null)
                startAvatarLoad()
            }

            override fun onSurfaceChanged(
                gl: javax.microedition.khronos.opengles.GL10?,
                width: Int,
                height: Int
            ) = renderer.onSurfaceChanged(gl, width, height)

            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) = renderer.onDrawFrame(gl)
        })
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private fun notifyState(ready: Boolean, error: String?) {
        post { onAvatarLoadStateChanged?.invoke(ready, error) }
    }

    private fun startAvatarLoad() {
        if (!surfaceReady || loadStarted) return
        loadStarted = true
        notifyState(false, null)
        Thread {
            val result = runCatching { GltfAvatarLoader(context).loadFromAssets(assetName) }
            queueEvent {
                result.fold(
                    onSuccess = { avatar ->
                        runCatching { renderer.setAvatar(avatar) }
                            .onSuccess { notifyState(true, null) }
                            .onFailure { error ->
                                loadStarted = false
                                notifyState(false, error.message ?: error.javaClass.simpleName)
                            }
                    },
                    onFailure = { error ->
                        loadStarted = false
                        notifyState(false, error.message ?: error.javaClass.simpleName)
                    }
                )
            }
        }.apply { name = "AvatarLoader" }.start()
    }

    fun reloadAvatar() {
        loadStarted = false
        notifyState(false, null)
        queueEvent {
            renderer.clearAvatar()
            startAvatarLoad()
        }
    }

    fun playAnimation(name: String) = queueEvent { renderer.playAnimation(name) }
    fun playAnimation(index: Int) = queueEvent { renderer.playAnimation(index) }
    fun playAnimation_old(name: String) = queueEvent { renderer.playAnimation(name) }

    fun updateAppearance(skinTone: String, eyeColor: String, hairColor: String, atmosphere: String) {
        queueEvent { renderer.updateAppearance(skinTone, eyeColor, hairColor, atmosphere) }
    }

    fun rotateCamera(dx: Float, dy: Float) = queueEvent { renderer.rotateCamera(dx, dy) }
    fun zoomCamera(factor: Float) = queueEvent { renderer.zoomCamera(factor) }

    private var lastX = 0f
    private var lastY = 0f
    private var baseDistance = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                baseDistance = 0f
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    val dx = event.getX(0) - event.getX(1)
                    val dy = event.getY(0) - event.getY(1)
                    baseDistance = kotlin.math.sqrt(dx * dx + dy * dy)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val dx = event.getX(0) - event.getX(1)
                    val dy = event.getY(0) - event.getY(1)
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (baseDistance > 0f && distance > 0f) zoomCamera(distance / baseDistance)
                    baseDistance = distance
                } else {
                    rotateCamera(event.x - lastX, event.y - lastY)
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_POINTER_UP -> baseDistance = 0f
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> baseDistance = 0f
        }
        return true
    }
}
