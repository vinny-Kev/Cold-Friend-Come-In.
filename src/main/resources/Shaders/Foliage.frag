#import "Common/ShaderLib/GLSLCompat.glsllib"

// Flat-shaded foliage: vertex colour lit by an ambient and one sun term, matching the low-poly look.

uniform vec3 m_SunDirection;
uniform vec4 m_SunColor;
uniform vec4 m_AmbientColor;

varying vec3 vNormal;
varying vec4 vColor;

void main() {
    float diffuse = max(dot(normalize(vNormal), -normalize(m_SunDirection)), 0.0);
    vec3 lit = vColor.rgb * (m_AmbientColor.rgb + m_SunColor.rgb * diffuse * 0.7);
    gl_FragColor = vec4(lit, 1.0);
}
