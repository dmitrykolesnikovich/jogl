#version 330

in vec3 inColor;
in vec3 inPosition;

out vec3 color;

void main()
{
    gl_Position = vec4(inPosition, 1.0);
	color = inColor;
}