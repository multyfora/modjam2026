#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform FirstContactConfig {
    float Intensity;
};

layout(std140) uniform SingularityConfig {
    vec4 Center; // xy = screen uv of the crystal, z = darkness radius in uv, w = intensity
};

const float SINGULARITY_CORE_RATIO = 0.3125; // DARK_CORE / DARK_RADIUS (2.5 / 8)

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    float I = clamp(Intensity, 0.0, 1.0);
    vec2 center = vec2(0.5);
    vec2 toCenter = texCoord - center;
    float dist = length(toCenter);

    float distortAmount = I * 0.15;
    vec2 uv = texCoord + toCenter * dist * dist * distortAmount;

    float aberration = I * 0.012 * (dist + 0.15);
    vec2 dir = normalize(toCenter + 1e-5);
    float r = texture(InSampler, uv + dir * aberration).r;
    float g = texture(InSampler, uv).g;
    float b = texture(InSampler, uv - dir * aberration).b;
    vec3 color = vec3(r, g, b);

    float vignette = smoothstep(0.85, 0.25, dist * (1.0 + I * 0.6));
    color *= mix(1.0, vignette, I);

    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(luma), clamp(I * dist * 0.6, 0.0, 1.0));

    color += (hash(texCoord * InSize + I * 100.0) - 0.5) * 0.05 * I;

    float sI = clamp(Center.w, 0.0, 1.0);
    if (sI > 0.001) {
        float sR = max(Center.z, 0.0001);
        float sD = distance(texCoord, Center.xy);
        float dark = (1.0 - smoothstep(sR * SINGULARITY_CORE_RATIO, sR, sD)) * sI;
        color *= (1.0 - dark * 0.97);
        float sluma = dot(color, vec3(0.299, 0.587, 0.114));
        color = mix(color, vec3(sluma * 0.05), dark);
    }

    float flash = smoothstep(0.92, 1.0, I);
    color = mix(color, vec3(1.0), flash * 0.9);

    fragColor = vec4(color, 1.0);
}
