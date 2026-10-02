"""Prepare short hummingbird calls and a continuous wing loop from the supplied recordings.

Requires numpy and soundfile/libsndfile. Leaves both source files untouched.
Usage: python tools/prepare_hummingbird_audio.py [source_directory]
The five phrase boundaries belong to this recording, not a generic silence slicer.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = Path('C:/Users/25773/Desktop/hummingbird_game_sound_pack (2)')
OUT = ROOT / 'src/main/resources/assets/guaniao/sounds/entity/hummingbird'
REPORT = ROOT / 'build/verification/hummingbird-audio'
SOURCE_HASHES = {
    'hummingbird_call_source.ogg': 'eb548b134e2720336167f24985a6aae6b9d51fe6d0047308b4cb95632e0ccc9d',
    'hummingbird_strafe.ogg': '3204c833d10512dffb461210b599f711fa84c26045f845a9e20ade7d149ffe2b',
}
# Complete contiguous chirp groups, with roughly 0.1 s of quiet room at either
# end. The fourth phrase includes a longer natural rapid-call sequence.
CALL_CUTS = [(.17, 2.29), (2.34, 4.31), (4.59, 6.51), (6.58, 8.93), (9.00, 10.03)]
CALL_GAIN = .85
WING_GAIN = .56


def db(value):
    return float(20 * np.log10(max(float(value), 1.0E-12)))


def measure(samples, sr):
    peak = float(np.max(np.abs(samples)))
    rms = float(np.sqrt(np.mean(samples ** 2)))
    return dict(frames=len(samples), seconds=len(samples) / sr, sample_rate=sr,
                channels=1, peak=peak, peak_dbfs=db(peak), rms=rms, rms_dbfs=db(rms),
                dc=float(np.mean(samples)))


def read_source(path):
    samples, sr = sf.read(path, dtype='float64', always_2d=True)
    assert samples.shape[1] == 1 and sr == 44100, 'This preparation targets the supplied mono 44.1 kHz pack'
    return samples[:, 0], sr


def call_phrase(source, sr, start, end):
    samples = source[round(start * sr):round(end * sr)].copy() * CALL_GAIN
    fade = round(.008 * sr)
    envelope = np.sin(np.linspace(0, np.pi / 2, fade)) ** 2
    samples[:fade] *= envelope
    samples[-fade:] *= envelope[::-1]
    return samples


def wing_loop(source, sr):
    """Join correlated stable flutter windows with an overlap, not a fade to silence."""
    overlap = round(.100 * sr)
    best = None
    # Avoid the attack at ~2.2 s and the decay after ~4.0 s. Match carrier phase
    # across two 100 ms regions before blending their small amplitude changes.
    for start in np.arange(2.52, 2.621, .005):
        for length in np.arange(1.35, 1.451, .002):
            if start + length > 4.02:
                continue
            first = round(start * sr)
            last = first + round(length * sr)
            samples = source[first:last]
            head, tail = samples[:overlap], samples[-overlap:]
            correlation = float(np.dot(head, tail) / np.sqrt(np.dot(head, head) * np.dot(tail, tail)))
            balance = abs(np.log(np.sqrt(np.mean(head ** 2)) / np.sqrt(np.mean(tail ** 2))))
            score = correlation - .1 * balance
            if best is None or score > best[0]:
                best = score, first, last, correlation
    _, first, last, correlation = best
    samples = source[first:last].copy()
    # Complementary raised-cosine windows have constant summed gain. Unlike a
    # start/end fade, this preserves flutter energy through the cyclic seam.
    weight = np.sin(np.linspace(0, np.pi / 2, overlap)) ** 2
    seam = samples[-overlap:] * (1 - weight) + samples[:overlap] * weight
    loop = np.concatenate((samples[overlap:-overlap], seam))
    loop -= np.mean(loop)
    loop *= WING_GAIN
    # Put the Vorbis file boundary near a zero crossing. The cyclic order is
    # unchanged; the interior overlap still joins the two source phases.
    crossings = np.flatnonzero((loop[:-1] <= 0) & (loop[1:] > 0)) + 1
    difference = np.diff(loop)
    # A zero crossing alone can select a quiet flutter trough. Keep the codec
    # boundary in a region whose 100 ms energy matches the overall loop too.
    half = round(.05 * sr)
    padded = np.concatenate((loop[-half:], loop, loop[:half]))
    energy = np.concatenate(([0.0], np.cumsum(padded ** 2)))
    overall = np.sqrt(np.mean(loop ** 2))
    stable = [i for i in crossings if .85 < np.sqrt((energy[i + 2 * half] - energy[i]) / (2 * half)) / overall < 1.15]
    assert stable, 'The source must offer a stable-energy zero crossing'
    index = min(stable, key=lambda i: abs(loop[i - 1]) + abs(loop[i])
                + .5 * abs(difference[i] - difference[i - 1]))
    loop = np.roll(loop, -int(index))
    return loop, dict(source_start=first / sr, source_end=last / sr,
                      crossfade_seconds=overlap / sr, crossfade_correlation=correlation,
                      circular_rotation_samples=int(index), gain=WING_GAIN,
                      gain_db=db(WING_GAIN), speed=1.0, fade_to_silence=False)


def write_ogg(path, samples, sr):
    assert np.max(np.abs(samples)) < .98, 'Source processing must retain headroom without a limiter'
    sf.write(path, samples, sr, format='OGG', subtype='VORBIS', compression_level=.25)
    decoded, rate = sf.read(path, dtype='float64', always_2d=True)
    assert rate == sr and decoded.shape == (len(samples), 1)
    assert np.max(np.abs(decoded)) < 1, 'No clipping after the actual Vorbis decode'
    return decoded[:, 0]


def decoded_loop_seam(samples, sr):
    differences = np.diff(samples)
    jump = float(abs(samples[-1] - samples[0]))
    p99 = float(np.quantile(np.abs(differences), .99))
    # The encoded/decoded join should look like an ordinary adjacent sample,
    # not a click hidden by measuring only the uncompressed preparation.
    assert jump <= max(p99 * 2, .002), (jump, p99)
    window = round(.05 * sr)
    wrap = np.concatenate((samples[-window:], samples[:window]))
    seam_rms = float(np.sqrt(np.mean(wrap ** 2)))
    rms = float(np.sqrt(np.mean(samples ** 2)))
    assert .70 < seam_rms / rms < 1.35, 'The decoded seam must not contain a periodic silence valley'
    return dict(wrap_sample_step=jump, ordinary_step_p99=p99,
                wrap_step_to_p99=jump / p99, seam_100ms_rms=seam_rms,
                seam_to_overall_rms=seam_rms / rms, decoded_seam_pass=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', nargs='?', default=str(DEFAULT_SOURCE))
    source_dir = Path(parser.parse_args().source)
    original = {name: (source_dir / name).read_bytes() for name in SOURCE_HASHES}
    for name, raw in original.items():
        assert hashlib.sha256(raw).hexdigest() == SOURCE_HASHES[name], 'Phrase boundaries require the original supplied recording: ' + name
    call, sr = read_source(source_dir / 'hummingbird_call_source.ogg')
    flutter, flutter_sr = read_source(source_dir / 'hummingbird_strafe.ogg')
    assert sr == flutter_sr
    OUT.mkdir(parents=True, exist_ok=True)
    REPORT.mkdir(parents=True, exist_ok=True)
    outputs = []
    decoded_calls = []
    for i, (start, end) in enumerate(CALL_CUTS, 1):
        path = OUT / f'call_{i}.ogg'
        decoded = write_ogg(path, call_phrase(call, sr, start, end), sr)
        decoded_calls.append(decoded)
        outputs.append(dict(id=f'guaniao:entity/hummingbird/call_{i}', filename=path.name,
                            source_start=start, source_end=end, gain=CALL_GAIN, gain_db=db(CALL_GAIN),
                            speed=1.0, fade_ms=8, **measure(decoded, sr)))
    loop, prep = wing_loop(flutter, sr)
    decoded_wing = write_ogg(OUT / 'wing_loop.ogg', loop, sr)
    outputs.append(dict(id='guaniao:entity/hummingbird/wing_loop', filename='wing_loop.ogg',
                        **prep, **measure(decoded_wing, sr), **decoded_loop_seam(decoded_wing, sr)))
    for entry in outputs:
        encoded = (OUT / entry['filename']).read_bytes()
        entry.update(bytes=len(encoded), sha256=hashlib.sha256(encoded).hexdigest())
    # Audition copies live only in ignored build output, never the mod JAR.
    sf.write(REPORT / 'calls-preview.wav', np.concatenate([part for call_clip in decoded_calls
             for part in (call_clip, np.zeros(round(.25 * sr)))]), sr, subtype='PCM_16')
    sf.write(REPORT / 'wing-loop-three-repeats.wav', np.tile(decoded_wing, 3), sr, subtype='PCM_16')
    assert all((source_dir / name).read_bytes() == raw for name, raw in original.items())
    report = dict(source_directory=str(source_dir), source_sha256=SOURCE_HASHES,
                  source_unchanged=True, source_call=measure(call, sr), source_wing=measure(flutter, sr),
                  source_call_voiced_group_threshold_rms=.03,
                  source_wing_active_interval_seconds=[2.15, 4.24],
                  tools=dict(numpy=np.__version__, soundfile=sf.__version__, libsndfile=sf.__libsndfile_version__),
                  human_listening_performed=False,
                  listening_limitation='The available assistant tools explicitly do not support audio input; acoustic structure and decoded-loop metrics were verified.',
                  no_speed_or_pitch_change=True, outputs=outputs,
                  max_call_seconds=max(entry['seconds'] for entry in outputs if entry['filename'].startswith('call_')),
                  min_call_pitch_for_duration_example=.9,
                  max_call_seconds_at_pitch_point9=max(len(c) / sr for c in decoded_calls) / .9,
                  previews=['calls-preview.wav', 'wing-loop-three-repeats.wav'])
    (REPORT / 'audio-preparation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps(report, ensure_ascii=False))


if __name__ == '__main__':
    main()
