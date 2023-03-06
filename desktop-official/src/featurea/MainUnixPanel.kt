package featurea

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel
import com.jogamp.opengl.util.FPSAnimator
import kotlinx.coroutines.runBlocking
import java.lang.System as JvmSystem

class MainPanel : GLJPanel(DefaultGLCapabilities()), GLEventListener {

    private var isCreated: Boolean = false
    private var past: Long = -1L
    private var currentWidth: Int = 0
    private var currentHeight: Int = 0

    init {
        addGLEventListener(this)
        animator = FPSAnimator(60)
        animator.add(this)
        animator.start()
    }

    override fun init(drawable: GLAutoDrawable?) = runBlocking {
        // init
    }

    override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {
        currentWidth = width
        currentHeight = height
        // resize
    }

    override fun display(drawable: GLAutoDrawable?) = runBlocking {
        val now: Long = JvmSystem.nanoTime()
        if (past == -1L) {
            past = now
        }
        val elapsedTime: Float = (now - past) / 1_000_000f
        past = now
        if (!isCreated) {
            try {
                // create
            } finally {
                isCreated = true
            }
        }
        try {

            // update(elapsedTime)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun dispose(drawable: GLAutoDrawable?) {
        animator.stop()
        // destroy
    }

}

/*internals*/

private fun DefaultGLCapabilities(): GLCapabilities = GLCapabilities(GLProfile.getDefault())
