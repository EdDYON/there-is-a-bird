#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform float NearPlane;
uniform float FarPlane;
uniform float FocusDistance;
uniform float Aperture;
uniform float FocalLength;
uniform float DofMultiplier;
uniform float LensDistortion;

in vec2 texCoord;
out vec4 fragColor;

float linearDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * NearPlane * FarPlane)
        / max(0.0001, FarPlane + NearPlane - z * (FarPlane - NearPlane));
}

vec2 lensUv(vec2 uv) {
    vec2 centered = uv * 2.0 - 1.0;
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 optical = vec2(centered.x * aspect, centered.y);
    float radiusSquared = dot(optical, optical);
    optical *= 1.0 - LensDistortion * min(radiusSquared, 2.5) * 0.22;
    centered = vec2(optical.x / max(aspect, 0.0001), optical.y);
    return clamp(centered * 0.5 + 0.5, vec2(0.001), vec2(0.999));
}

void main() {
    vec2 uv = lensUv(texCoord);
    vec3 center = texture(DiffuseSampler, uv).rgb;
    float depth = texture(DepthSampler, uv).r;
    float sceneDistance = linearDepth(depth);
    float relativeDistance = abs(sceneDistance - FocusDistance) / max(FocusDistance, 0.5);
    float apertureStrength = 2.8 / max(Aperture, 1.4);
    float focalStrength = pow(clamp(FocalLength / 85.0, 0.20, 2.50), 0.78);
    float coc = smoothstep(0.055, 1.55, relativeDistance)
        * apertureStrength
        * focalStrength
        * DofMultiplier;
    if (sceneDistance < FocusDistance) {
        coc *= 1.15;
    }
    float radius = clamp(coc * 6.5, 0.0, 9.0);
    vec2 texel = radius / max(OutSize, vec2(1.0));

    vec3 color = center * 0.26;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2( texel.x, 0.0))).rgb * 0.095;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(-texel.x, 0.0))).rgb * 0.095;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(0.0,  texel.y))).rgb * 0.095;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(0.0, -texel.y))).rgb * 0.095;
    color += texture(DiffuseSampler, lensUv(texCoord + texel)).rgb * 0.065;
    color += texture(DiffuseSampler, lensUv(texCoord - texel)).rgb * 0.065;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(texel.x, -texel.y))).rgb * 0.065;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(-texel.x, texel.y))).rgb * 0.065;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(texel.x * 1.65, texel.y * 0.35))).rgb * 0.05;
    color += texture(DiffuseSampler, lensUv(texCoord + vec2(-texel.x * 1.65, -texel.y * 0.35))).rgb * 0.05;

    fragColor = vec4(color, 1.0);
}
