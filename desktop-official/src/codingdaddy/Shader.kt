package codingdaddy

import com.jogamp.opengl.GL2
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

class ShaderProgram {

    var programId: Int = 0
    var vertexShaderId: Int = 0
    var fragmentShaderId: Int = 0
    private var isInitialized: Boolean = false
    private lateinit var gl2: GL2
    private val shaderAttributes = mutableMapOf<String, Int>()

    fun init(gl2: GL2, vertexShader: InputStream, fragmentShader: InputStream): Boolean {
        if (isInitialized) {
            error("Unable to initialize the shader program! (it was already initialized)")
        }
        this.gl2 = gl2
        try {
            val vertexShaderCode: String = ShaderUtils.loadResource(vertexShader)
            val fragmentShaderCode: String = ShaderUtils.loadResource(fragmentShader)
            programId = gl2.glCreateProgram()
            vertexShaderId = ShaderUtils.createShader(gl2, programId, vertexShaderCode, GL2.GL_VERTEX_SHADER)
            fragmentShaderId = ShaderUtils.createShader(gl2, programId, fragmentShaderCode, GL2.GL_FRAGMENT_SHADER)
            ShaderUtils.link(gl2, programId)
            this.isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isInitialized
    }

    fun dispose(gl2: GL2) {
        isInitialized = false
        gl2.glDetachShader(programId, vertexShaderId)
        gl2.glDetachShader(programId, fragmentShaderId)
        gl2.glDeleteProgram(programId)
    }

    fun locationOf(shaderAttribute: String): Int {
        if (!isInitialized) {
            error("Unable to get the attribute location! The shader program was not initialized!")
        }
        var location: Int? = shaderAttributes[shaderAttribute]
        if (location == null) {
            location = gl2.glGetAttribLocation(programId, shaderAttribute)
            shaderAttributes[shaderAttribute] = location
        }
        return location
    }

}

object ShaderUtils {

    fun loadResource(inputStream: InputStream): String {
        inputStream.use { return Scanner(inputStream, "UTF-8").useDelimiter("\\A").next() }
    }

    fun createShader(gl2: GL2, programId: Int, shaderCode: String, shaderType: Int): Int {
        val shaderId: Int = gl2.glCreateShader(shaderType)
        if (shaderId == 0) {
            error("Error creating shader. Shader id is zero.")
        }
        gl2.glShaderSource(shaderId, 1, arrayOf(shaderCode), null)
        gl2.glCompileShader(shaderId)
        val intBuffer: IntBuffer = IntBuffer.allocate(1)
        gl2.glGetShaderiv(shaderId, GL2.GL_COMPILE_STATUS, intBuffer)
        if (intBuffer[0] != 1) {
            gl2.glGetShaderiv(shaderId, GL2.GL_INFO_LOG_LENGTH, intBuffer)
            val size: Int = intBuffer[0]
            if (size > 0) {
                val byteBuffer = ByteBuffer.allocate(size)
                gl2.glGetShaderInfoLog(shaderId, size, intBuffer, byteBuffer)
                println(byteBuffer.toString())
            }
            error("Error compiling shader!")
        }
        gl2.glAttachShader(programId, shaderId)
        return shaderId
    }

    fun link(gl2: GL2, programId: Int) {
        gl2.glLinkProgram(programId)
        var intBuffer: IntBuffer = IntBuffer.allocate(1)
        gl2.glGetProgramiv(programId, GL2.GL_LINK_STATUS, intBuffer)
        if (intBuffer[0] != 1) {
            gl2.glGetProgramiv(programId, GL2.GL_INFO_LOG_LENGTH, intBuffer)
            val size = intBuffer[0]
            if (size > 0) {
                val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
                gl2.glGetProgramInfoLog(programId, size, intBuffer, byteBuffer)
                println(byteBuffer.toString())
            }
            error("Error linking shader program!")
        }
        gl2.glValidateProgram(programId)
        intBuffer = IntBuffer.allocate(1)
        gl2.glGetProgramiv(programId, GL2.GL_VALIDATE_STATUS, intBuffer)
        if (intBuffer[0] != 1) {
            gl2.glGetProgramiv(programId, GL2.GL_INFO_LOG_LENGTH, intBuffer)
            val size = intBuffer[0]
            if (size > 0) {
                val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
                gl2.glGetProgramInfoLog(programId, size, intBuffer, byteBuffer)
                println(byteBuffer.toString())
            }
            error("Error validating shader program!")
        }
    }

}
