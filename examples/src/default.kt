package jogl.desktop.examples

import codingdaddy.*
import com.jogamp.common.nio.Buffers
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLJPanel
import codingdaddy.Cube
import codingdaddy.ShaderProgram
import java.io.InputStream
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun default() {
    class Renderer : GLEventListener {

        private lateinit var shaderProgram: ShaderProgram
        private lateinit var vertexBuffer: FloatBuffer
        private lateinit var indexBuffer: IntBuffer
        private lateinit var colorBuffer: FloatBuffer

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        override fun init(glAutoDrawable: GLAutoDrawable) {
            val gl: GL2ES2 = glAutoDrawable.gl.gL2ES2
            val vertexShader: InputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("shaders/default.vs")
            val pixelShader: InputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("shaders/default.fs")
            shaderProgram = ShaderProgram()
            if (!shaderProgram.init(gl, vertexShader, pixelShader)) {
                error("shader initialization failed")
            }
            vertexBuffer = Buffers.newDirectFloatBuffer(Cube.vertices.size).put(Cube.vertices)
            indexBuffer = Buffers.newDirectIntBuffer(Cube.indices.size).put(Cube.indices)
            colorBuffer = Buffers.newDirectFloatBuffer(Cube.colors.size).put(Cube.colors)
            gl.glEnable(GL2ES2.GL_DEPTH_TEST)
            gl.glDepthMask(true)


        }

        override fun reshape(glAutoDrawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
            // no op
        }

        override fun display(glAutoDrawable: GLAutoDrawable) {
            val gl: GL2ES2 = glAutoDrawable.gl.gL2ES2
            val colorLocation: Int = shaderProgram.locationOf("inColor")
            val positionLocation: Int = shaderProgram.locationOf("inPosition")
            gl.glClearColor(0.2f, 0.3f, 0.4f, 1.0f)
            gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT or GL2ES2.GL_DEPTH_BUFFER_BIT)
            /*gl.glUseProgram(shaderProgram.programId)
            gl.glEnableVertexAttribArray(positionLocation)
            gl.glEnableVertexAttribArray(colorLocation)
            gl.glVertexAttribPointer(positionLocation, 3, GL2ES2.GL_FLOAT, false, 0, vertexBuffer.rewind())
            gl.glVertexAttribPointer(colorLocation, 3, GL2ES2.GL_FLOAT, false, 0, colorBuffer.rewind())
            gl.glDrawElements(GL2ES2.GL_TRIANGLES, Cube.indices.size, GL2ES2.GL_UNSIGNED_INT, indexBuffer.rewind())
            gl.glDisableVertexAttribArray(positionLocation)
            gl.glDisableVertexAttribArray(colorLocation)*/
            gl.glUseProgram(0)
        }

        override fun dispose(glAutoDrawable: GLAutoDrawable) {
            val gl: GL2ES2 = glAutoDrawable.gl.gL2ES2
            shaderProgram.dispose(gl)
        }

    }

    val glProfile: GLProfile = GLProfile.get(GLProfile.GL2ES2)    
    val glCapabilities: GLCapabilities = GLCapabilities(glProfile)
    SwingUtilities.invokeLater {
        val frame: JFrame = JFrame("Cube Example")
        frame.setSize(640, 480)
        val panel: GLJPanel = GLJPanel(glCapabilities)
        panel.addGLEventListener(Renderer())
        panel.size = frame.size
        frame.contentPane.add(panel)
        frame.isVisible = true
    }

}
