#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;
uniform float FilterMode;

in vec2 texCoord;
out vec4 fragColor;

vec2 safeUV(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

float lum(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 sat(vec3 c, float s) {
    float y = lum(c);
    return clamp(mix(vec3(y), c, s), 0.0, 1.0);
}

vec3 contrast(vec3 c, float k) {
    return clamp((c - 0.5) * k + 0.5, 0.0, 1.0);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vignette(vec2 uv, float start, float end, float amount) {
    float d = length((uv - 0.5) * vec2(1.0, OutSize.y / max(OutSize.x, 1.0)));
    return 1.0 - smoothstep(start, end, d) * amount;
}

vec3 grain(vec3 c, vec2 uv, float amount) {
    float y = lum(c);
    float mid = 1.0 - abs(y * 2.0 - 1.0);
    float n = hash21(gl_FragCoord.xy + vec2(Time * 173.0, Time * 97.0)) - 0.5;
    return clamp(c + n * amount * (0.35 + 0.65 * mid), 0.0, 1.0);
}

vec3 sepia(vec3 c) {
    return clamp(vec3(
        dot(c, vec3(0.393, 0.769, 0.189)),
        dot(c, vec3(0.349, 0.686, 0.168)),
        dot(c, vec3(0.272, 0.534, 0.131))
    ), 0.0, 1.0);
}

vec3 thermal(float y) {
    y = clamp(y, 0.0, 1.0);
    vec3 c0 = vec3(0.02, 0.00, 0.12);
    vec3 c1 = vec3(0.10, 0.05, 0.75);
    vec3 c2 = vec3(0.85, 0.05, 0.35);
    vec3 c3 = vec3(1.00, 0.65, 0.05);
    vec3 c4 = vec3(1.00, 1.00, 0.75);
    if (y < 0.25) return mix(c0, c1, y / 0.25);
    if (y < 0.50) return mix(c1, c2, (y - 0.25) / 0.25);
    if (y < 0.75) return mix(c2, c3, (y - 0.50) / 0.25);
    return mix(c3, c4, (y - 0.75) / 0.25);
}

void main() {
    vec2 uv = safeUV(texCoord);
    vec3 src = texture(DiffuseSampler, uv).rgb;
    vec3 c = src;
    float y = lum(src);

    // 1 BLACK_AND_WHITE
    if (FilterMode == 1.0) {
        float g = clamp((y - 0.5) * 1.18 + 0.5, 0.0, 1.0);
        c = vec3(g);
    }
    // 2 FILM_GRAIN
    else if (FilterMode == 2.0) {
        c = grain(sat(src * vec3(1.03, 1.00, 0.95), 0.95), uv, 0.075);
        c *= vignette(uv, 0.30, 0.66, 0.22);
    }
    // 3 EXPOSURE
    else if (FilterMode == 3.0) {
        c = 1.0 - pow(1.0 - clamp(src, 0.0, 1.0), vec3(1.52));
        c = sat(c, 1.08);
    }
    // 4 COLOR_BALANCE (real-time approximation; keep CPU final WB if desired)
    else if (FilterMode == 4.0) {
        c = src * vec3(1.08, 1.00, 0.93);
        c = contrast(sat(c, 1.10), 1.06);
    }
    // 5 BW_HARD
    else if (FilterMode == 5.0) {
        float g = contrast(vec3(y), 1.70).r;
        c = vec3(smoothstep(0.025, 0.975, g));
    }
    // 6 VIVID
    else if (FilterMode == 6.0) {
        c = contrast(sat(src, 1.90), 1.25);
    }
    // 7 VINTAGE_WARM
    else if (FilterMode == 7.0) {
        c = src * vec3(1.18, 1.04, 0.76);
        c = contrast(sat(c, 0.82), 1.07);
        c = c * 0.88 + vec3(0.075, 0.052, 0.025);
        c *= vignette(uv, 0.31, 0.68, 0.28);
    }
    // 8 TEAL_ORANGE
    else if (FilterMode == 8.0) {
        vec3 teal = src * vec3(0.68, 1.04, 1.22);
        vec3 orange = src * vec3(1.23, 1.01, 0.68);
        c = mix(teal, orange, smoothstep(0.26, 0.80, y));
        c = contrast(sat(c, 1.23), 1.18);
    }
    // 9 DREAMY
    else if (FilterMode == 9.0) {
        c = 1.0 - pow(1.0 - src, vec3(1.48));
        c = mix(c, c * vec3(1.08, 0.98, 1.11), 0.72);
        c = mix(c, vec3(1.0, 0.94, 0.985), smoothstep(0.55, 1.0, lum(c)) * 0.22);
        c = sat(c, 0.90);
    }
    // 10 LOMO
    else if (FilterMode == 10.0) {
        c = contrast(sat(src, 1.75), 1.30);
        c *= vignette(uv, 0.26, 0.62, 0.58);
        c *= vec3(1.06, 0.98, 1.02);
    }
    // 11 SOFT_LIGHT
    else if (FilterMode == 11.0) {
        c = mix(src, 1.0 - pow(1.0 - src, vec3(1.35)), 0.48);
        c = sat(c, 0.92);
        c = contrast(c, 0.92);
    }
    // 12 DEEP_CONTRAST
    else if (FilterMode == 12.0) {
        c = contrast(sat(src, 1.12), 1.52);
        c = pow(c, vec3(1.05));
    }
    // 13 WARM_GLOW
    else if (FilterMode == 13.0) {
        c = 1.0 - pow(1.0 - src, vec3(1.30));
        c *= vec3(1.16, 1.03, 0.83);
        c = sat(c, 1.12);
    }
    // 14 COOL_CLEAR
    else if (FilterMode == 14.0) {
        c = contrast(src * vec3(0.88, 1.01, 1.18), 1.17);
        c = sat(c, 1.13);
    }
    // 15 SKY_BOOST
    else if (FilterMode == 15.0) {
        float blueMask = smoothstep(0.02, 0.35, src.b - max(src.r, src.g) * 0.72);
        c = mix(src, src * vec3(0.92, 1.05, 1.38), blueMask * 0.82);
        c = sat(c, 1.20);
    }
    // 16 FOREST_BOOST
    else if (FilterMode == 16.0) {
        float greenMask = smoothstep(0.0, 0.30, src.g - max(src.r, src.b) * 0.78);
        c = mix(src, src * vec3(0.90, 1.30, 0.89), greenMask * 0.75);
        c = contrast(sat(c, 1.20), 1.10);
    }
    // 17 SUNSET_BOOST
    else if (FilterMode == 17.0) {
        c = src * vec3(1.27, 0.96, 0.72);
        c = mix(c, vec3(c.r, c.g * 0.86, c.b * 1.05), smoothstep(0.25, 0.75, y));
        c = contrast(sat(c, 1.28), 1.12);
    }
    // 18 VINTAGE_COOL
    else if (FilterMode == 18.0) {
        c = src * vec3(0.88, 1.02, 1.13);
        c = c * 0.83 + vec3(0.055, 0.066, 0.082);
        c = sat(c, 0.78);
        c = grain(c, uv, 0.045);
    }
    // 19 FADED_FILM
    else if (FilterMode == 19.0) {
        c = contrast(sat(src, 0.68), 0.78);
        c = mix(c, vec3(0.90, 0.83, 0.70), 0.10);
        c = c * 0.82 + vec3(0.09);
        c = grain(c, uv, 0.035);
    }
    // 20 BLEACH_BYPASS
    else if (FilterMode == 20.0) {
        vec3 mono = vec3(y);
        c = mix(src, mono, 0.58);
        c = contrast(c, 1.62);
        c = sat(c, 0.72);
    }
    // 21 SEPIA
    else if (FilterMode == 21.0) {
        c = contrast(sepia(src), 1.16);
        c *= vignette(uv, 0.31, 0.69, 0.25);
    }
    // 22 CINE_BLUE_GOLD
    else if (FilterMode == 22.0) {
        vec3 shadow = src * vec3(0.62, 0.86, 1.26);
        vec3 high = src * vec3(1.30, 1.09, 0.62);
        c = mix(shadow, high, smoothstep(0.36, 0.74, y));
        c = contrast(sat(c, 1.15), 1.18);
    }
    // 23 CROSS_PROCESS
    else if (FilterMode == 23.0) {
        c.r = smoothstep(0.02, 0.86, src.r);
        c.g = pow(src.g, 0.82);
        c.b = pow(src.b, 1.23) * 1.12;
        c = contrast(sat(c, 1.35), 1.12);
    }
    // 24 TOY_CAMERA
    else if (FilterMode == 24.0) {
        c = contrast(sat(src, 1.48), 1.32);
        c *= vec3(1.08, 0.94, 1.03);
        c *= vignette(uv, 0.20, 0.58, 0.68);
        c = grain(c, uv, 0.055);
    }
    // 25 BW_SOFT
    else if (FilterMode == 25.0) {
        float g = (y - 0.5) * 0.83 + 0.55;
        c = vec3(clamp(g, 0.0, 1.0));
    }
    // 26 BW_HIGH_KEY
    else if (FilterMode == 26.0) {
        float g = 1.0 - pow(1.0 - y, 1.70);
        c = vec3(contrast(vec3(g), 1.05));
    }
    // 27 BW_LOW_KEY
    else if (FilterMode == 27.0) {
        float g = pow(y, 1.55);
        c = vec3(contrast(vec3(g), 1.45));
        c *= vignette(uv, 0.27, 0.66, 0.32);
    }
    // 28 BW_WARM
    else if (FilterMode == 28.0) {
        c = vec3(y) * vec3(1.08, 1.00, 0.86);
        c = contrast(c, 1.22);
    }
    // 29 BW_COOL
    else if (FilterMode == 29.0) {
        c = vec3(y) * vec3(0.84, 0.96, 1.13);
        c = contrast(c, 1.24);
    }
    // 30 SILVER
    else if (FilterMode == 30.0) {
        float g = contrast(vec3(y), 1.32).r;
        c = vec3(g) * vec3(0.92, 0.98, 1.05);
        c = grain(c, uv, 0.040);
    }
    // 31 CHARCOAL
    else if (FilterMode == 31.0) {
        float g = floor(clamp(contrast(vec3(y), 1.85).r, 0.0, 1.0) * 7.0) / 7.0;
        c = vec3(g);
    }
    // 32 NOIR_GRAIN
    else if (FilterMode == 32.0) {
        c = vec3(contrast(vec3(y), 1.68).r);
        c = grain(c, uv, 0.105);
        c *= vignette(uv, 0.24, 0.62, 0.52);
    }
    // 33 MISTY
    else if (FilterMode == 33.0) {
        c = contrast(sat(src, 0.72), 0.78);
        c = mix(c, vec3(0.80, 0.88, 0.92), 0.18);
        c = 1.0 - pow(1.0 - c, vec3(1.22));
    }
    // 34 DAWN
    else if (FilterMode == 34.0) {
        c = src * vec3(1.17, 0.96, 1.06);
        c = mix(c, vec3(1.0, 0.70, 0.76), smoothstep(0.50, 1.0, y) * 0.18);
        c = 1.0 - pow(1.0 - c, vec3(1.18));
    }
    // 35 MOONLIGHT
    else if (FilterMode == 35.0) {
        c = pow(src, vec3(1.18)) * vec3(0.64, 0.82, 1.24);
        c = sat(c, 0.72);
        c = contrast(c, 1.18);
    }
    // 36 ROMANTIC_PINK
    else if (FilterMode == 36.0) {
        c = 1.0 - pow(1.0 - src, vec3(1.27));
        c *= vec3(1.16, 0.91, 1.05);
        c = sat(c, 1.08);
    }
    // 37 AUTUMN
    else if (FilterMode == 37.0) {
        c = src * vec3(1.20, 1.04, 0.72);
        c.r += src.g * 0.08;
        c.g *= 0.94;
        c = contrast(sat(c, 1.30), 1.10);
    }
    // 38 SPRING
    else if (FilterMode == 38.0) {
        c = src * vec3(1.06, 1.17, 0.98);
        c = 1.0 - pow(1.0 - c, vec3(1.22));
        c = sat(c, 1.16);
    }
    // 39 WINTER
    else if (FilterMode == 39.0) {
        c = src * vec3(0.82, 0.98, 1.22);
        c = sat(c, 0.78);
        c = 1.0 - pow(1.0 - c, vec3(1.15));
    }
    // 40 SUMMER
    else if (FilterMode == 40.0) {
        c = src * vec3(1.13, 1.07, 0.88);
        c = contrast(sat(c, 1.42), 1.14);
    }
    // 41 CYBERPUNK
    else if (FilterMode == 41.0) {
        c = contrast(sat(src, 1.80), 1.32);
        c *= vec3(1.16, 0.78, 1.30);
        c.g += smoothstep(0.45, 1.0, src.b) * 0.16;
        c = clamp(c, 0.0, 1.0);
    }
    // 42 HORROR_GREEN
    else if (FilterMode == 42.0) {
        c = contrast(src, 1.38);
        c *= vec3(0.66, 1.22, 0.60);
        c = sat(c, 0.72);
        c *= vignette(uv, 0.25, 0.62, 0.52);
    }
    // 43 APOCALYPSE
    else if (FilterMode == 43.0) {
        c = sat(src, 0.58);
        c *= vec3(1.26, 0.82, 0.53);
        c = contrast(c, 1.42);
        c = grain(c, uv, 0.060);
        c *= vignette(uv, 0.29, 0.66, 0.30);
    }
    // 44 GLITCH_RGB
    else if (FilterMode == 44.0) {
        float band = floor(uv.y * 90.0);
        float kick = step(0.84, hash21(vec2(band, floor(Time * 24.0))));
        float shift = (hash21(vec2(band * 2.3, Time)) - 0.5) * 0.045 * kick;
        vec2 ur = safeUV(uv + vec2(shift + 0.004, 0.0));
        vec2 ug = safeUV(uv + vec2(shift, 0.0));
        vec2 ub = safeUV(uv + vec2(shift - 0.004, 0.0));
        c = vec3(
            texture(DiffuseSampler, ur).r,
            texture(DiffuseSampler, ug).g,
            texture(DiffuseSampler, ub).b
        );
        c = contrast(sat(c, 1.35), 1.18);
    }
    // 45 CHROMATIC_ABERRATION
    else if (FilterMode == 45.0) {
        vec2 dir = uv - 0.5;
        vec2 off = dir * 0.018;
        c = vec3(
            texture(DiffuseSampler, safeUV(uv + off)).r,
            src.g,
            texture(DiffuseSampler, safeUV(uv - off)).b
        );
        c = contrast(c, 1.12);
    }
    // 46 NEON
    else if (FilterMode == 46.0) {
        c = contrast(sat(src, 2.35), 1.48);
        c = pow(c, vec3(0.82));
        c *= vec3(1.08, 0.92, 1.18);
        c = clamp(c, 0.0, 1.0);
    }
    // 47 POSTERIZE
    else if (FilterMode == 47.0) {
        c = floor(sat(src, 1.45) * 6.0 + 0.5) / 6.0;
        c = contrast(c, 1.26);
    }
    // 48 DUOTONE_BLUE
    else if (FilterMode == 48.0) {
        vec3 dark = vec3(0.015, 0.035, 0.13);
        vec3 light = vec3(0.55, 0.88, 1.00);
        c = mix(dark, light, smoothstep(0.08, 0.93, y));
    }
    // 49 NIGHT_VISION
    else if (FilterMode == 49.0) {
        float boosted = 1.0 - pow(1.0 - y, 2.25);
        c = vec3(boosted * 0.25, boosted, boosted * 0.18);
        c = grain(c, uv, 0.07);
        c *= vignette(uv, 0.28, 0.65, 0.48);
    }
    // 50 THERMAL
    else if (FilterMode == 50.0) {
        c = thermal(contrast(vec3(y), 1.32).r);
    }

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
