package jogl.desktop.examples

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.fixedfunc.GLLightingFunc
import com.jogamp.opengl.glu.GLU
import com.jogamp.opengl.util.FPSAnimator
import com.jogamp.opengl.util.texture.Texture
import com.jogamp.opengl.util.texture.TextureIO
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.IOException
import javax.swing.*

// https://github.com/Syncleus/aparapi-examples/blob/master/src/main/java/com/aparapi/examples/nbody/Seq.java
fun nbody() {
    /**
     * Constant `bodies=Integer.getInteger("bodies", 8192)`
     */
    val bodies = Integer.getInteger("bodies", 8192)

    /**
     * Constant `width=`
     */
    var width = 0

    /**
     * Constant `height=`
     */
    var height = 0

    /**
     * Constant `running=`
     */
    var running = false
    var texture: Texture? = null

    /**
     *
     * main.
     *
     * @param _args an array of [java.lang.String] objects.
     */


    class NBodyKernel(private val bodies: Int) {
        protected val delT = .005f
        protected val espSqr = 1.0f
        protected val mass = 5f
        private val xyz // positions xy and z of bodies
                : FloatArray
        private val vxyz // velocity component of x,y and z of bodies
                : FloatArray

        /**
         * Constructor initializes xyz and vxyz arrays.
         *
         * @param _bodies The number of bodies to be simulated.
         */
        init {
            xyz = FloatArray(bodies * 3)
            vxyz = FloatArray(bodies * 3)
            val maxDist = 20f
            var body = 0
            while (body < bodies * 3) {
                val theta = (Math.random() * Math.PI * 2).toFloat()
                val phi = (Math.random() * Math.PI * 2).toFloat()
                val radius = (Math.random() * maxDist).toFloat()

                // get the 3D dimensional coordinates
                xyz[body + 0] = (radius * Math.cos(theta.toDouble()) * Math.sin(phi.toDouble())).toFloat()
                xyz[body + 1] = (radius * Math.sin(theta.toDouble()) * Math.sin(phi.toDouble())).toFloat()
                xyz[body + 2] = (radius * Math.cos(phi.toDouble())).toFloat()

                // divide into two 'spheres of bodies' by adjusting x
                if (body % 2 == 0) {
                    xyz[body + 0] += (maxDist * 1.5).toFloat()
                } else {
                    xyz[body + 0] -= (maxDist * 1.5).toFloat()
                }
                body += 3
            }
        }

        /**
         * Here is the kernel entrypoint. Here is where we calculate the position of each body
         */
        fun run() {
            val count = bodies * 3
            for (body in 0 until bodies) {
                val globalId = body * 3
                var accx = 0f
                var accy = 0f
                var accz = 0f
                val myPosx = xyz[globalId + 0]
                val myPosy = xyz[globalId + 1]
                val myPosz = xyz[globalId + 2]
                var i = 0
                while (i < count) {
                    val dx = xyz[i + 0] - myPosx
                    val dy = xyz[i + 1] - myPosy
                    val dz = xyz[i + 2] - myPosz
                    val invDist = 1f / Math.sqrt((dx * dx + dy * dy + dz * dz + espSqr).toDouble()).toFloat()
                    val s = mass * invDist * invDist * invDist
                    accx = accx + s * dx
                    accy = accy + s * dy
                    accz = accz + s * dz
                    i += 3
                }
                accx = accx * delT
                accy = accy * delT
                accz = accz * delT
                xyz[globalId + 0] = myPosx + vxyz[globalId + 0] * delT + accx * .5f * delT
                xyz[globalId + 1] = myPosy + vxyz[globalId + 1] * delT + accy * .5f * delT
                xyz[globalId + 2] = myPosz + vxyz[globalId + 2] * delT + accz * .5f * delT
                vxyz[globalId + 0] = vxyz[globalId + 0] + accx
                vxyz[globalId + 1] = vxyz[globalId + 1] + accy
                vxyz[globalId + 2] = vxyz[globalId + 2] + accz
            }
        }

        /**
         * Render all particles to the OpenGL context
         *
         * @param gl The OpenGL context to render to.
         */
        fun render(gl: GL2) {
            gl.glBegin(GL2.GL_QUADS)
            var i = 0
            while (i < bodies * 3) {
                gl.glTexCoord2f(0f, 1f)
                gl.glVertex3f(xyz[i + 0], xyz[i + 1] + 1, xyz[i + 2])
                gl.glTexCoord2f(0f, 0f)
                gl.glVertex3f(xyz[i + 0], xyz[i + 1], xyz[i + 2])
                gl.glTexCoord2f(1f, 0f)
                gl.glVertex3f(xyz[i + 0] + 1, xyz[i + 1], xyz[i + 2])
                gl.glTexCoord2f(1f, 1f)
                gl.glVertex3f(xyz[i + 0] + 1, xyz[i + 1] + 1, xyz[i + 2])
                i += 3
            }
            gl.glEnd()
        }
    }

    //System.load("/Library/Java/JavaVirtualMachines/jdk1.7.0_09.jdk/Contents/Home/jre/lib/libawt.dylib");
    //System.load("/Library/Java/JavaVirtualMachines/jdk1.7.0_09.jdk/Contents/Home/jre/lib/libjawt.dylib");
    val kernel = NBodyKernel(bodies)
    val frame = JFrame("NBody")
    val panel = JPanel(BorderLayout())
    val controlPanel = JPanel(FlowLayout())
    panel.add(controlPanel, BorderLayout.SOUTH)
    val startButton = JButton("Start")
    startButton.addActionListener {
        running = true
        startButton.isEnabled = false
    }
    controlPanel.add(startButton)
    controlPanel.add(JLabel("SEQ"))
    controlPanel.add(JLabel("   Particles"))
    controlPanel.add(JTextField("" + bodies, 5))
    controlPanel.add(JLabel("FPS"))
    val framesPerSecondTextField = JTextField("0", 5)
    controlPanel.add(framesPerSecondTextField)
    controlPanel.add(JLabel("Score("))
    val miniLabel = JLabel("<html><small>calcs</small><hr/><small>&micro;sec</small></html>")
    controlPanel.add(miniLabel)
    controlPanel.add(JLabel(")"))
    val positionUpdatesPerMicroSecondTextField = JTextField("0", 5)
    controlPanel.add(positionUpdatesPerMicroSecondTextField)
    val caps = GLCapabilities(null)
    val profile = caps.glProfile
    caps.doubleBuffered = true
    caps.hardwareAccelerated = true
    val canvas = GLCanvas(caps)
    val dimension = Dimension(Integer.getInteger("width", 742 - 64), Integer.getInteger("height", 742 - 64))
    canvas.preferredSize = dimension
    canvas.addGLEventListener(object : GLEventListener {
        private var ratio = 0.0
        private val xeye = 0f
        private val yeye = 0f
        private val zeye = 100f
        private val xat = 0f
        private val yat = 0f
        private val zat = 0f
        val zoomFactor = 1.0f
        private var frames = 0
        private var last = System.currentTimeMillis()
        override fun dispose(drawable: GLAutoDrawable) {}
        override fun display(drawable: GLAutoDrawable) {
            val gl = drawable.gl.gL2
            texture!!.enable(gl)
            texture!!.bind(gl)
            gl.glLoadIdentity()
            gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
            gl.glColor3f(1f, 1f, 1f)
            val glu = GLU()
            glu.gluPerspective(45.0, ratio, 1.0, 1000.0)
            glu.gluLookAt(xeye, yeye, zeye * zoomFactor, xat, yat, zat, 0f, 1f, 0f)
            if (running) {
                kernel.run()
            }
            kernel.render(gl)
            val now = System.currentTimeMillis()
            val time = now - last
            frames++
            if (time > 1000) { // We update the frames/sec every second
                if (running) {
                    val framesPerSecond = frames * 1000.0f / time
                    val updatesPerMicroSecond = (framesPerSecond * bodies * bodies / 1000000).toInt()
                    framesPerSecondTextField.text = String.format("%5.2f", framesPerSecond)
                    positionUpdatesPerMicroSecondTextField.text = String.format("%4d", updatesPerMicroSecond)
                }
                frames = 0
                last = now
            }
            gl.glFlush()
        }

        override fun init(drawable: GLAutoDrawable) {
            val gl = drawable.gl.gL2
            gl.glShadeModel(GLLightingFunc.GL_SMOOTH)
            gl.glEnable(GL.GL_BLEND)
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE)
            gl.glEnable(GL.GL_TEXTURE_2D)
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR)
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST)
            try {
                val textureStream = ClassLoader.getSystemClassLoader().getResourceAsStream("images/particle.jpg")
                val data = TextureIO.newTextureData(profile, textureStream, false, "jpg")
                texture = TextureIO.newTexture(data)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: GLException) {
                e.printStackTrace()
            }
        }

        override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, _width: Int, _height: Int) {
            width = _width
            height = _height
            val gl = drawable.gl.gL2
            gl.glViewport(0, 0, width, height)
            ratio = width.toDouble() / height.toDouble()
        }
    })
    panel.add(canvas, BorderLayout.CENTER)
    frame.contentPane.add(panel, BorderLayout.CENTER)
    val animator = FPSAnimator(canvas, 100)
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.pack()
    frame.isVisible = true
    animator.start()
}
