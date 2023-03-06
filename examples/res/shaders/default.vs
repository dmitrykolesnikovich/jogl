attribute vec3 inColor;
attribute vec3 inPosition;

varying vec3 color;

void main()
{
    gl_Position = vec4(inPosition, 1.0);
	color = inColor;
}