package jogl.desktop.examples

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderProgram
import singingbush.ResourceLoader

fun helloTriangle() {
    HelloTriangleSimple().setup()
}

class HelloTriangleSimple : GLEventListener, KeyListener {

    private val vertexData = floatArrayOf(
        -1f, -1f, 1f, 0f, 0f,
        +0f, +2f, 0f, 0f, 1f,
        +1f, -1f, 0f, 1f, 0f
    )
    private val elementData = shortArrayOf(0, 2, 1)

    private interface Buffer {
        companion object {
            const val VERTEX = 0
            const val ELEMENT = 1
            const val GLOBAL_MATRICES = 2
            const val MAX = 3
        }
    }

    private val bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX)
    private val vertexArrayName = GLBuffers.newDirectIntBuffer(1)
    private val clearColor = GLBuffers.newDirectFloatBuffer(4)
    private val clearDepth = GLBuffers.newDirectFloatBuffer(1)
    private val matBuffer = GLBuffers.newDirectFloatBuffer(16)
    private lateinit var program: Program
    private var start: Long = 0

    fun setup() {
        try {
            val glProfile = GLProfile.get(GLProfile.GL3)
            val glCapabilities = GLCapabilities(glProfile)
            window = GLWindow.create(glCapabilities)
            window.setTitle("Hello Triangle (simple)")
            window.setSize(1024, 768)
            window.setVisible(true)
            window.addGLEventListener(this)
            window.addKeyListener(this)
            animator = Animator(window)
            animator.start()
            window.addWindowListener(object : WindowAdapter() {
                override fun windowDestroyed(e: WindowEvent) {
                    animator.stop()
                    System.exit(0)
                }
            })
        } catch (e: GLException) {
            System.err.println("Couldn't get OpenGL 3 profile")
            e.printStackTrace()
            System.exit(1)
        }
    }

    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3
        initBuffers(gl)
        initVertexArray(gl)
        initProgram(gl)
        gl.glEnable(GL.GL_DEPTH_TEST)
        start = System.currentTimeMillis()
    }

    private fun initBuffers(gl: GL3) {
        val vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData)
        val elementBuffer = GLBuffers.newDirectShortBuffer(elementData)
        gl.glGenBuffers(Buffer.MAX, bufferName)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            (vertexBuffer.capacity() * java.lang.Float.BYTES).toLong(),
            vertexBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        gl.glBufferData(
            GL.GL_ELEMENT_ARRAY_BUFFER,
            (elementBuffer.capacity() * java.lang.Short.BYTES).toLong(),
            elementBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0)
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
        gl.glBufferData(
            GL2ES3.GL_UNIFORM_BUFFER,
            (16 * java.lang.Float.BYTES * 2).toLong(),
            null,
            GL2ES2.GL_STREAM_DRAW
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)
        gl.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, Uniform.GLOBAL_MATRICES, bufferName[Buffer.GLOBAL_MATRICES])
        checkError(gl, "initBuffers")
    }

    private fun initVertexArray(gl: GL3) {
        gl.glGenVertexArrays(1, vertexArrayName)
        gl.glBindVertexArray(vertexArrayName[0])
        run {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
            run {
                val stride = (2 + 3) * java.lang.Float.BYTES
                var offset = 0
                gl.glEnableVertexAttribArray(Attr.POSITION)
                gl.glVertexAttribPointer(Attr.POSITION, 2, GL.GL_FLOAT, false, stride, offset.toLong())
                offset = 2 * java.lang.Float.BYTES
                gl.glEnableVertexAttribArray(Attr.COLOR)
                gl.glVertexAttribPointer(Attr.COLOR, 3, GL.GL_FLOAT, false, stride, offset.toLong())
            }
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        }
        gl.glBindVertexArray(0)
        checkError(gl, "initVao")
    }

    private fun initProgram(gl: GL3) {
        program = Program(gl)
        checkError(gl, "initProgram")
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        // view matrix
        run {
            val view = FloatArray(16)
            FloatUtil.makeIdentity(view)
            for (i in 0..15) {
                matBuffer.put(i, view[i])
            }
            gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
            gl.glBufferSubData(
                GL2ES3.GL_UNIFORM_BUFFER,
                (16 * java.lang.Float.BYTES).toLong(),
                (16 * java.lang.Float.BYTES).toLong(),
                matBuffer
            )
            gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)
        }
        gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f))
        gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, clearDepth.put(0, 1f))
        gl.glUseProgram(program.name)
        gl.glBindVertexArray(vertexArrayName[0])

        // model matrix
        run {
            val now = System.currentTimeMillis()
            val diff = (now - start).toFloat() / 1000f
            val scale = FloatUtil.makeScale(FloatArray(16), true, 0.5f, 0.5f, 0.5f)
            val zRotation = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0f, 0f, diff)
            val modelToWorldMat = FloatUtil.multMatrix(scale, zRotation)
            for (i in 0..15) {
                matBuffer.put(i, modelToWorldMat[i])
            }
            gl.glUniformMatrix4fv(program.modelToWorldMatUL, 1, false, matBuffer)
        }
        gl.glDrawElements(GL.GL_TRIANGLES, elementData.size, GL.GL_UNSIGNED_SHORT, 0)
        gl.glUseProgram(0)
        gl.glBindVertexArray(0)
        checkError(gl, "display")
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL3
        val ortho = FloatArray(16)
        FloatUtil.makeOrtho(ortho, 0, false, -1f, 1f, -1f, 1f, 1f, -1f)
        for (i in 0..15) {
            matBuffer.put(i, ortho[i])
        }
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
        gl.glBufferSubData(GL2ES3.GL_UNIFORM_BUFFER, 0, (16 * java.lang.Float.BYTES).toLong(), matBuffer)
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)
        gl.glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3
        gl.glDeleteProgram(program.name)
        gl.glDeleteVertexArrays(1, vertexArrayName)
        gl.glDeleteBuffers(Buffer.MAX, bufferName)
    }

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            Thread { window.destroy() }.start()
        }
    }

    override fun keyReleased(e: KeyEvent) {}

    private inner class Program internal constructor(gl: GL3) {
        var name: Int
        var modelToWorldMatUL: Int

        init {
            val vertShader = ResourceLoader.vertexShader(gl, "shaders/hello-triangle", "vert")
            val fragShader = ResourceLoader.fragmentShader(gl, "shaders/hello-triangle", "frag")
            val shaderProgram = ShaderProgram()
            shaderProgram.add(vertShader)
            shaderProgram.add(fragShader)
            shaderProgram.init(gl)
            name = shaderProgram.program()
            shaderProgram.link(gl, System.err)
            modelToWorldMatUL = gl.glGetUniformLocation(name, "model")
            if (modelToWorldMatUL == -1) {
                System.err.println("uniform 'model' not found!")
            }
            val globalMatricesBI = gl.glGetUniformBlockIndex(name, "GlobalMatrices")
            if (globalMatricesBI == -1) {
                System.err.println("block index 'GlobalMatrices' not found!")
            }
            gl.glUniformBlockBinding(name, globalMatricesBI, Uniform.GLOBAL_MATRICES)
        }
    }

    private fun checkError(gl: GL, location: String) {
        val error = gl.glGetError()
        if (error != GL.GL_NO_ERROR) {
            val errorString: String
            errorString = when (error) {
                GL.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                GL.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                GL.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                GL.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                GL.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                else -> "UNKNOWN"
            }
            throw Error("OpenGL Error($errorString): $location")
        }
    }

    private interface Attr {
        companion object {
            const val POSITION = 0
            const val COLOR = 1
            const val NORMAL = 2
            const val TEXCOORD = 3
            const val DRAW_ID = 4
        }
    }

    private interface Uniform {
        companion object {
            const val MATERIAL = 0
            const val TRANSFORM0 = 1
            const val TRANSFORM1 = 2
            const val INDIRECTION = 3
            const val GLOBAL_MATRICES = 4
            const val CONSTANT = 0
            const val PER_FRAME = 1
            const val PER_PASS = 2
            const val LIGHT = 3
        }
    }

    private interface Stream {
        companion object {
            const val A = 0
            const val B = 1
        }
    }

    companion object {
        private lateinit var window: GLWindow
        private lateinit var animator: Animator
    }
}