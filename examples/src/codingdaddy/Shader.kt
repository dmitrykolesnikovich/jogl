package codingdaddy

import com.jogamp.opengl.GL2ES2
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.IntBuffer
import java.nio.charset.Charset
import java.util.*

class ShaderProgram {

    var programId: Int = 0
    var vertexShaderId: Int = 0
    var fragmentShaderId: Int = 0
    private var isInitialized: Boolean = false
    private lateinit var gl: GL2ES2
    private val shaderAttributes = mutableMapOf<String, Int>()

    fun init(gl: GL2ES2, vertexShader: InputStream, fragmentShader: InputStream): Boolean {
        if (isInitialized) {
            error("Unable to initialize the shader program! (it was already initialized)")
        }
        this.gl = gl
        try {
            val vertexShaderCode: String = ShaderUtils.loadResource(vertexShader)
            val fragmentShaderCode: String = ShaderUtils.loadResource(fragmentShader)
            programId = gl.glCreateProgram()
            vertexShaderId = ShaderUtils.createShader(gl, programId, vertexShaderCode, GL2ES2.GL_VERTEX_SHADER)
            fragmentShaderId = ShaderUtils.createShader(gl, programId, fragmentShaderCode, GL2ES2.GL_FRAGMENT_SHADER)
            ShaderUtils.link(gl, programId)
            this.isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isInitialized
    }

    fun dispose(gl: GL2ES2) {
        isInitialized = false
        gl.glDetachShader(programId, vertexShaderId)
        gl.glDetachShader(programId, fragmentShaderId)
        gl.glDeleteProgram(programId)
    }

    fun locationOf(shaderAttribute: String): Int {
        if (!isInitialized) {
            error("Unable to get the attribute location! The shader program was not initialized!")
        }
        var location: Int? = shaderAttributes[shaderAttribute]
        if (location == null) {
            location = gl.glGetAttribLocation(programId, shaderAttribute)
            shaderAttributes[shaderAttribute] = location
        }
        return location
    }

}

object ShaderUtils {

    fun loadResource(inputStream: InputStream): String {
        inputStream.use { return Scanner(inputStream, "UTF-8").useDelimiter("\\A").next() }
    }

    fun createShader(gl: GL2ES2, programId: Int, shaderCode: String, shaderType: Int): Int {
        val shaderId: Int = gl.glCreateShader(shaderType)
        if (shaderId == 0) {
            error("Error creating shader. Shader id is zero.")
        }
        gl.glShaderSource(shaderId, 1, arrayOf(shaderCode), null)
        gl.glCompileShader(shaderId)
        val intBuffer: IntBuffer = IntBuffer.allocate(1)
        gl.glGetShaderiv(shaderId, GL2ES2.GL_COMPILE_STATUS, intBuffer)
        if (intBuffer[0] != 1) {
            gl.glGetShaderiv(shaderId, GL2ES2.GL_INFO_LOG_LENGTH, intBuffer)
            val size: Int = intBuffer[0]
            if (size > 0) {
                val byteBuffer = ByteBuffer.allocate(size)
                gl.glGetShaderInfoLog(shaderId, size, intBuffer, byteBuffer)
                val cb: CharBuffer = Charset.forName("UTF-8").decode(byteBuffer)
                println("error: ${String(Charset.forName("UTF-8").encode(cb).array())}")
            }
            error("Error compiling shader!")
        }
        gl.glAttachShader(programId, shaderId)
        return shaderId
    }

    fun link(gl: GL2ES2, programId: Int) {
        gl.glLinkProgram(programId)
        var intBuffer: IntBuffer = IntBuffer.allocate(1)
        gl.glGetProgramiv(programId, GL2ES2.GL_LINK_STATUS, intBuffer)
        if (intBuffer[0] != 1) {
            gl.glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, intBuffer)
            val size = intBuffer[0]
            if (size > 0) {
                val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
                gl.glGetProgramInfoLog(programId, size, intBuffer, byteBuffer)
                println(byteBuffer.toString())
            }
            error("Error linking shader program!")
        }
        gl.glValidateProgram(programId)
        intBuffer = IntBuffer.allocate(1)
        gl.glGetProgramiv(programId, GL2ES2.GL_VALIDATE_STATUS, intBuffer)
        if (intBuffer[0] != 1) {
            gl.glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, intBuffer)
            val size = intBuffer[0]
            if (size > 0) {
                val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
                gl.glGetProgramInfoLog(programId, size, intBuffer, byteBuffer)
                println(byteBuffer.toString())
            }
            error("Error validating shader program!")
        }
    }

}
