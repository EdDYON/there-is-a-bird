"""Import the author's exported Bedrock assets without rewriting their bytes.

Usage: python tools/export_hummingbird.py [path/to/exported/asset/directory]
The original model.animation.json, geometry and PNG remain authoritative.
A separate runtime animation file adapts their tracks to GeckoLib 4.4.9 and
the existing disjoint body, wings and eyes controllers.
"""
import argparse
import copy
import hashlib
import json
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/guaniao'
WINGS = {'wing_left', 'wing_right'}
BODY = ['root', 'all', 'head', 'waist', 'hip', 'body', 'tail', 'Wing',
        'leg_left', 'leg_right', 'foot_left', 'foot_right']
DEFAULT_SOURCE = Path('C:/Users/25773/Desktop/模型/蜂鸟')
SOURCE_FILES = {'animation':'model.animation.json',
                'geometry':'蜂鸟26.9.26Anim.geo.json', 'texture':'texture.png'}


def number(v):
    if isinstance(v, str):
        v = v.strip()
        try:
            return float(v)
        except ValueError:
            return v
    return v


def vector(point, channel):
    # Input has already passed through Blockbench's Bedrock exporter. GeckoLib
    # applies the Bedrock rotation conversion; a second reflection is wrong.
    return [number(point[axis]) for axis in 'xyz']


def scalar(v):
    # GeckoLib 4.4.9's Molang lexer does not support scientific notation.
    # Gson JSON numbers can use it, but e.g. "2.775e-17" inside a generated
    # expression is parsed as a subtraction. Keep the value in plain decimal.
    if isinstance(v, (int, float)):
        return '0' if v == 0 else format(Decimal(str(v)), 'f')
    return v


def catmull_channel(frames, channel):
    """Exact Blockbench/THREE.SplineCurve polynomial, not GeckoLib's easing.

    GeckoLib 4.4.9 drops Bedrock ``lerp_mode`` when baking keyframes. Its
    catmullrom easing also lacks adjacent control points. The author's two
    numerical spline tracks are therefore compiled to equivalent Molang.
    The original times, control points and endpoint duplication are retained.
    """
    assert all(f['interpolation'] == 'catmullrom' and len(f['data_points']) == 1 for f in frames)
    points = [vector(f['data_points'][0], channel) for f in frames]
    assert all(isinstance(v, (int, float)) for p in points for v in p), 'Expression spline requires an explicit adapter'
    result = []
    for axis in range(3):
        if all(p[axis] == points[0][axis] for p in points):
            result.append(points[0][axis])
            continue
        expression = scalar(points[-1][axis])
        for i in reversed(range(len(frames) - 1)):
            p0 = points[max(0, i-1)][axis]
            p1, p2 = points[i][axis], points[i+1][axis]
            p3 = points[min(len(points)-1, i+2)][axis]
            c0, c1 = p1, (p2-p0)/2
            c2 = p0-2.5*p1+2*p2-.5*p3
            c3 = -.5*p0+1.5*p1-1.5*p2+.5*p3
            start, end = frames[i]['time'], frames[i+1]['time']
            t = 'math.clamp((query.anim_time-'+scalar(start)+')/'+scalar(end-start)+',0,1)'
            polynomial = '((('+scalar(c3)+'*'+t+'+'+scalar(c2)+')*'+t+'+'+scalar(c1)+')*'+t+'+'+scalar(c0)+')'
            expression = '(query.anim_time<'+scalar(end)+'?'+polynomial+':'+expression+')'
        result.append(expression)
    return result


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def unwrap_vector(value):
    if isinstance(value, list):
        assert len(value) == 3
        return [number(v) for v in value]
    assert isinstance(value, dict)
    if 'vector' in value:
        return unwrap_vector(value['vector'])
    if 'post' in value:
        return unwrap_vector(value['post'])
    raise AssertionError('Expected an exported vector: '+str(value))


def source_clips(source):
    """Normalize this six-clip Bedrock export only; do not convert coordinates."""
    clips = copy.deepcopy(source['animations'])
    assert len(clips) == 6
    for animation in clips.values():
        for channels in animation['bones'].values():
            for channel, track in list(channels.items()):
                if isinstance(track, list) or 'vector' in track:
                    channels[channel] = unwrap_vector(track)
                    continue
                frames = sorted(track.items(), key=lambda item: float(item[0]))
                if any(frame.get('lerp_mode') == 'catmullrom' for _, frame in frames):
                    spline = [dict(time=float(time), interpolation=frame['lerp_mode'],
                                   data_points=[dict(zip('xyz',unwrap_vector(frame)))])
                              for time, frame in frames]
                    channels[channel] = catmull_channel(spline, channel)
                else:
                    # Keep the user's rounded timestamp strings, rather than
                    # reconstructing older bbmodel times or re-rounding them.
                    channels[channel] = {time: unwrap_vector(frame) for time, frame in frames}
    return clips


def replace_time(value, clock='variable.flight_time'):
    if isinstance(value,str):return value.replace('query.anim_time',clock)
    if isinstance(value,list):return [replace_time(v,clock) for v in value]
    if isinstance(value,dict):return {k:replace_time(v,clock) for k,v in value.items()}
    return value


def sync_stroke_phase(value):
    """Only authored 8 Hz/16 Hz waveforms follow the continuous wing phase.

    Backward flight can advance that phase slightly faster than age time.
    Clip timelines, low-frequency breathing and blinks retain their clock.
    """
    if isinstance(value,str):
        return value.replace('variable.flight_time','variable.wing_stroke_time') if '2880' in value else value
    if isinstance(value,list):return [sync_stroke_phase(v) for v in value]
    if isinstance(value,dict):return {k:sync_stroke_phase(v) for k,v in value.items()}
    return value


def neutral():
    return {name:dict(rotation=[0,0,0],position=[0,0,0],scale=[1,1,1]) for name in BODY}


def body(source, flight=False):
    bones = neutral()
    for name, channels in source['bones'].items():
        if name in BODY:
            bones[name].update(copy.deepcopy(channels))
    if flight:
        bones=replace_time(bones)
    return dict(loop=source.get('loop',False),animation_length=source.get('animation_length',1),bones=bones)


def blend_flight(hover, fly):
    """Preserve hover's timeline and continuously blend the author's flight pose."""
    def blend(value, target):
        if isinstance(value, list):
            return [h if h == f else 'math.lerp(('+scalar(h)+'),('+scalar(f)+'),variable.forward_blend)'
                    for h, f in zip(value, target)]
        return {k: v if k == 'lerp_mode' else blend(v, target) for k, v in value.items()}
    result = copy.deepcopy(hover)
    for name, channels in result['bones'].items():
        for channel in channels:
            target = fly['bones'][name][channel]
            assert isinstance(target, list), 'Author forward flight must be a continuous expression pose'
            channels[channel] = blend(channels[channel], target)
    def reverse_pitch(track, direction):
        if isinstance(track,list):
            return ['('+scalar(track[0])+')'+direction+'10*math.clamp(variable.backward_blend,0,1)',*track[1:]]
        return {k:v if k=='lerp_mode' else reverse_pitch(v,direction) for k,v in track.items()}
    # Backward flight stays near the upright hover pose; never use the prone
    # forward endpoint for reversing. Ten degrees is a bounded Minecraft pose
    # parameter, not a measured aerodynamic angle. Counter-pitch the head so
    # the beak retains the author's hovering attitude.
    result['bones']['all']['rotation']=reverse_pitch(result['bones']['all']['rotation'],'-')
    result['bones']['head']['rotation']=reverse_pitch(result['bones']['head']['rotation'],'+')
    return result


def first(value):
    if isinstance(value,list):return value
    if 'post' in value:return value['post']
    return first(value[min(value,key=float)])


def transition(start,end,duration):
    bones={}
    for bone in BODY:
        bones[bone]={}
        for channel in ('rotation','position','scale'):
            bones[bone][channel]={'0':first(start['bones'][bone][channel]),str(duration):first(end['bones'][bone][channel])}
    return dict(loop=False,animation_length=duration,bones=bones)


def add_bank(clip):
    def roll(v):
        if isinstance(v,list):
            return [v[0],v[1],scalar(v[2])+' + variable.bank_angle']
        return {k:roll(value) if k in ('pre','post') or k not in ('lerp_mode',) else value for k,value in v.items()}
    clip['bones']['all']['rotation']=roll(clip['bones']['all']['rotation'])


def runtime_clips(original):
    clips=copy.deepcopy(original)
    idle=body(original['animation.idle'])
    hover=body(original['animation.fly_idle'],True)
    fly=body(original['animation.fly'],True)
    # The user-supplied Bedrock forward endpoint is already correct.
    fly['loop']=True
    sleep=body(original['animation.sleep'])
    sleep['animation_length']=8
    nectar=copy.deepcopy(hover)
    nectar['animation_length']=1
    nectar['bones']['all']['rotation']=[-25,0,'math.cos(variable.flight_time*2880)*0.35']
    nectar['bones']['head']['rotation']=[25,0,0]
    nectar['bones']['head']['position']=[0,0,-.15]
    names={'idle':idle,'hover':hover,'fly':fly,'flight':blend_flight(hover,fly),'sleep':sleep,
           'idle_diff_1':body(original['animation.idle_diff_1'],True),
           'idle_diff_2':body(original['animation.idle_diff_2']),
           'nectar':nectar,'nectar_loop':copy.deepcopy(nectar),
           'nectar_enter':transition(hover,nectar,.25),'nectar_exit':transition(nectar,hover,.25),
           'takeoff':transition(idle,hover,.2),'land':transition(hover,idle,.25),
           'sleep_enter':transition(idle,sleep,.35),'wake':transition(sleep,idle,.3)}
    drop=copy.deepcopy(idle)
    drop.update(loop=False,animation_length=.45)
    drop['bones']['head']['rotation']={'0':[0,0,0],'.18':[-20,0,0],'.32':[-20,0,0],'.45':[-5,0,0]}
    names['drop_seed']=drop
    for name,clip in names.items():
        add_bank(clip)
        clips['animation.body.'+name]=clip
    flying={name:replace_time(copy.deepcopy(original['animation.fly_idle']['bones'][name]),'variable.wing_stroke_time') for name in sorted(WINGS)}
    for channels in flying.values():
        channels['rotation'][0]='variable.wing_pitch'
        # Equal signed local yaw changes move one outward-facing wing forward
        # and the other backward. The four-degree bound is a visual adaptation;
        # root supplies a smoothed acceleration response, not a constant lean.
        channels['rotation'][1]='('+scalar(channels['rotation'][1])+')+math.clamp(variable.wing_yaw_bias,-4,4)'
    clips['animation.wings.fly']=dict(loop=True,animation_length=1,bones=flying)
    clips['animation.wings.idle']=dict(loop=True,animation_length=1,bones={name:dict(rotation=[0,0,0],position=[0,0,0]) for name in sorted(WINGS)})
    clips['animation.eyes.idle']=dict(loop=True,animation_length=original['animation.idle']['animation_length'],bones={'eye':copy.deepcopy(original['animation.idle']['bones']['eye'])})
    clips['animation.eyes.sleep']=dict(loop=True,animation_length=1,bones={'eye':dict(scale=[1,.2,1])})
    clips['animation.eyes.wake']=dict(loop=False,animation_length=.3,bones={'eye':dict(scale={'0':[1,.2,1],'.3':[1,1,1]})})
    for suffix in ('idle_diff_1','idle_diff_2'):
        source = original['animation.'+suffix]
        wings = {name:copy.deepcopy(source['bones'].get(name, dict(rotation=[0,0,0],position=[0,0,0]))) for name in sorted(WINGS)}
        eyes = copy.deepcopy(source['bones'].get('eye',dict(scale=[1,1,1])))
        if suffix == 'idle_diff_1':
            wings = replace_time(wings,'variable.wing_stroke_time')
        clips['animation.wings.'+suffix]=dict(loop=False,animation_length=source['animation_length'],bones=wings)
        clips['animation.eyes.'+suffix]=dict(loop=False,animation_length=source['animation_length'],bones={'eye':eyes})
    for name,clip in clips.items():
        if name.startswith(('animation.body.','animation.wings.','animation.eyes.')):
            clip['bones']=sync_stroke_phase(clip['bones'])
        if name.startswith('animation.body.'):
            assert not (set(clip['bones']) & (WINGS|{'eye'}))
        elif name.startswith('animation.wings.'):
            assert set(clip['bones']) == WINGS
            assert all(set(v)<= {'rotation','position','scale'} for v in clip['bones'].values())
        elif name.startswith('animation.eyes.'):
            assert set(clip['bones'])=={'eye'} and set(clip['bones']['eye'])=={'scale'}
    return {'format_version':'1.8.0','animations':clips}


def thumbnail(geo, texture):
    import numpy as np
    from PIL import Image
    from render_handbook_thumbnails import render, pos, rot, translate, rotation
    bones={b['name']:b for b in geo['minecraft:geometry'][0]['bones']}
    transforms={}
    def visit(name):
        if name in transforms:return transforms[name]
        b=bones[name];parent=b.get('parent')
        pp=pos(bones[parent]['pivot']) if parent else np.zeros(3)
        local=translate(pos(b['pivot'])-pp)@rot(b.get('rotation',[0,0,0]))
        transforms[name]=(visit(parent) if parent else np.eye(4))@local
        return transforms[name]
    cubes=[];view=rotation([0,135,0],[0,0,0])
    for name,b in bones.items():
        for c in b.get('cubes',[]):
            pivot=pos(c.get('pivot',b['pivot']));size=np.array(c['size']);center=pos(np.array(c['origin'])+size/2)
            transform=view@visit(name)@translate(pivot-pos(b['pivot']))@rot(c.get('rotation',[0,0,0]))@translate(center-pivot)
            cubes.append(({**c,'origin':list(-size/2)},transform))
    with Image.open(texture) as skin:
        image=render(cubes,skin,[32,32],128,(0,0,0))
    assert image.getbbox() is not None
    path=ASSETS/'textures/gui/handbook/hummingbird.png'
    path.parent.mkdir(parents=True,exist_ok=True)
    image.save(path,optimize=True)
    return path


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source',nargs='?',default=str(DEFAULT_SOURCE))
    source_dir=Path(parser.parse_args().source)
    raw={kind:(source_dir/name).read_bytes() for kind,name in SOURCE_FILES.items()}
    source=json.loads(raw['animation'].decode('utf-8-sig'))
    geo=json.loads(raw['geometry'].decode('utf-8-sig'))
    bones=geo['minecraft:geometry'][0]['bones']
    assert len(bones)==15 and sum(len(b.get('cubes',[])) for b in bones)==18
    assert set(b['name'] for b in bones)==set(BODY)|WINGS|{'eye'}
    clips=runtime_clips(source_clips(source))
    paths={'geometry':ASSETS/'geo/hummingbird.geo.json',
           'animation':ASSETS/'animations/hummingbird.animation.json',
           'texture':ASSETS/'textures/entity/hummingbird.png'}
    for kind,path in paths.items():
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(raw[kind])
    runtime_path=ASSETS/'animations/hummingbird_runtime.animation.json'
    dump(runtime_path,clips)
    from PIL import Image
    with Image.open(paths['texture']) as image:assert image.size==(32,32)
    poster=thumbnail(geo,paths['texture'])
    source_assets={kind:dict(filename=SOURCE_FILES[kind],sha256=hashlib.sha256(data).hexdigest())
                   for kind,data in raw.items()}
    for kind,path in paths.items():
        assert path.read_bytes()==raw[kind]
        assert (source_dir/SOURCE_FILES[kind]).read_bytes()==raw[kind]
    dump(ROOT/'build/verification/hummingbird/asset-provenance.json',{
        'source_directory':str(source_dir),'source_assets':source_assets,
        'source_unchanged':True,'original_assets_byte_identical':True,
        'bones':15,'cubes':18,'geometry_conversion':'None: user-exported Bedrock geometry copied verbatim',
        'animation_conversion':'Original source copied verbatim; runtime unwraps vector containers without coordinate reflection',
        'curve_reference':'https://github.com/JannisX11/blockbench/blob/master/js/animations/keyframe.js',
        'geckolib_spline_adapter':'Exact uniform Catmull-Rom polynomials in runtime only; GeckoLib 4.4.9 discards lerp_mode',
        'flight_blend':'Original fly_idle/fly endpoints unchanged; forward_blend plus continuous wing_stroke_time',
        'runtime_forward_pitch_correction':False,
        'runtime_directional_pose':{'backward_blend':'0..1 upright hover plus at most 10 degrees backward torso pitch, opposite head compensation',
                                    'wing_pitch':'Positive Bedrock X: hover 40, forward 32.5, reverse 47.5',
                                    'wing_stroke_time':'Continuous phase for authored 8 Hz/16 Hz body and wing waveforms',
                                    'wing_yaw_bias':'Smoothed lateral acceleration response, capped at +/-4 degrees',
                                    'bank_angle':'Acceleration-based roll, applied once on body.all Z',
                                    'parameter_status':'Bounded Minecraft visual parameters, not measured aerodynamic values'},
        'original_animations':list(source['animations']),
        'runtime_animations':[n for n in clips['animations'] if n.startswith(('animation.body.','animation.wings.','animation.eyes.'))],
        'outputs':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()
                   for p in (*paths.values(),runtime_path,poster)}})
    print('Imported hummingbird: 3 byte-identical original assets, 15 bones, 18 cubes, 6 original clips +',
          len(clips['animations'])-6,'runtime clips; source unchanged')


if __name__=='__main__':main()
