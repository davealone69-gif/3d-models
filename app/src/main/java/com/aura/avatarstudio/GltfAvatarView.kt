@file:Suppress("TooManyFunctions", "MagicNumber")

package com.aura.avatarstudio

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.aura.avatarstudio.api.GeneratedAvatarStore
import com.aura.avatarstudio.renderer.GltfAvatarLoader
import com.aura.avatarstudio.renderer.HdAvatarRenderer
import java.io.File

/** Touch-wired GL view with deterministic asynchronous avatar loading. */
class GltfAvatarView(
    private val context: Context,
    private val assetName: String,
    private val onAvatarLoadStateChanged: ((ready: Boolean, error: String?) -> Unit)? = null
) : GLSurfaceView(context) {

    private val renderer = HdAvatarRenderer(context)
    @Volatile private var loadStarted = false
    @Volatile private var generatedLoadStarted = false
    @Volatile private var surfaceReady = false

    init {
        GeneratedAvatarStore.initialize(context)
        setEGLContextClientVersion(3)
        setEGLConfigChooser { egl, display ->
            val attribs = intArrayOf(
                12324, 8, 12323, 8, 12322, 8, 12321, 8,
                12325, 24, 12326, 8, 12338, 1, 12337, 4, 12344
            )
            val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            egl.eglChooseConfig(display, attribs, configs, 1, numConfigs)
            if (numConfigs[0] > 0) configs[0] else {
                val fallback = intArrayOf(
                    12324, 8, 12323, 8, 12322, 8, 12321, 8,
                    12325, 24, 12326, 8, 12344
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
                generatedLoadStarted = false
                notifyState(false, null)
                startAvatarLoad()
            }

            override fun onSurfaceChanged(
                gl: javax.microedition.khronos.opengles.GL10?,
                width: Int,
                height: Int
            ) = renderer.onSurfaceChanged(gl, width, height)

            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                renderer.onDrawFrame(gl)
                if (!generatedLoadStarted) {
                    GeneratedAvatarStore.consumeNew()?.let { file -> loadGeneratedAvatar(file) }
                }
            }
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

    private fun loadGeneratedAvatar(file: File) {
        if (!surfaceReady || generatedLoadStarted) return
        generatedLoadStarted = true
        notifyState(false, null)
        Thread {
            val result = runCatching { GltfAvatarLoader(context).loadGlb(file.readBytes()) }
            queueEvent {
                result.fold(
                    onSuccess = { avatar ->
                        runCatching { renderer.setAvatar(avatar) }
                            .onSuccess {
                                generatedLoadStarted = false
                                notifyState(true, null)
                            }
                            .onFailure { error ->
                                generatedLoadStarted = false
                                notifyState(false, error.message ?: error.javaClass.simpleName)
                            }
                    },
                    onFailure = { error ->
                        generatedLoadStarted = false
                        notifyState(false, error.message ?: error.javaClass.simpleName)
                    }
                )
            }
        }.apply { name = "GeneratedAvatarLoader" }.start()
    }

    /** Consume and load the newest AI-generated GLB immediately. */
    fun reloadGeneratedAvatar() {
        if (!surfaceReady || generatedLoadStarted) return
        GeneratedAvatarStore.consumeNew()?.let { file -> loadGeneratedAvatar(file) }
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
