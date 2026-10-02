"""Gameplay corrections for the bundled guide when the website lags behind the mod."""

HERON = {
    'zh_cn': {
        'summary': '用生鳕鱼或生鲑鱼驯服后，夜鹭会陪伴主人，偶尔叼来一条鱼。',
        'commands': [
            '主人双手空着、潜行右键，可在自由、跟随、停留、归巢之间切换。副手拿生鳕鱼或生鲑鱼时优先投喂；主手、副手都可以喂鱼，用副手时请把主手空出来。归巢会寻找附近栖木休息。切换模式不会清除嘴里的鱼。',
            '跟随模式下，白天也会继续跟随。主人在远处时，夜鹭会实际飞过去，不会传送；接近后在主人周围盘旋。需要它停下时，可切换停留或归巢。送鱼的 32 格范围不限制普通跟随。',
        ],
        'fishing': [
            '驯服后，夜鹭在自由或跟随模式下会偶尔主动送鱼，不需要安排任务，也不需要池塘、岸边、钓鱼竿或水中的活鱼。',
            '默认每累计 5–10 分钟有效活动时间尝试一次，有 50% 概率直接在嘴里出现一条生鳕鱼或生鲑鱼，然后叼给主人。每次最多叼一条；没有获得鱼时，等待下一轮再尝试。',
        ],
        'gifts': [
            '主人在线、处于同一维度且默认在夜鹭 32 格内，夜鹭在黄昏或夜间安全、清醒地活动时才会累计送鱼时间。白天跟随不会累计送鱼时间；离线、休息、停留和归巢时暂停计时。',
            '嘴里已经有鱼时不会再生成。存档重进保留已叼的鱼和剩余等待时间，不补发离线期间的鱼。可在观鸟设置中关闭主动送鱼，或调整主人范围和尝试间隔。',
        ],
        'receiving': [
            '夜鹭会叼鱼来到主人附近，安全落地后短暂停留，把鱼放到主人面前的地面上；跟随模式下，交付后继续在主人周围盘旋。也可以不潜行、主手空着，在 3 格内对它右键，直接把鱼收进背包。',
            '背包已满且没有能合并的鱼堆时，它会继续叼着鱼。腾出空间后再空手右键收取；不要按住潜行，否则会切换伙伴指令。',
        ],
        'fright': [
            '野生夜鹭的警戒会逐级变成冻结观察、步行后退、奔跑、低飞或长距离飞离。反复追赶会积累惊吓，附近野生夜鹭也可能一起受惊。',
            '已驯服的夜鹭不会因普通玩家靠近或野鸟群受惊而逃走；受到攻击时仍会避险。',
        ],
        'settings': [
            '项目  /  默认值',
            '夜鹭驯服 / 主动送鱼  /  开启，可分别关闭',
            '每次有效投喂的驯服概率  /  1/3',
            '主人距离上限  /  32 格',
            '主动送鱼尝试间隔  /  300–600 秒',
            '送鱼间隔只累计黄昏或夜间安全、清醒的有效活动时间，主人还需在线、同维度并在距离上限内。白天跟随、主人离线、夜鹭休息或处于停留、归巢模式时不计时。每次尝试有 50% 概率直接叼出一条鱼送给主人，不需要水边、钓鱼竿或活鱼。主人距离上限仅用于送鱼，不限制普通跟随。',
        ],
    },
    'en_us': {
        'summary': 'Tame it with raw cod or salmon for a companion that occasionally brings you a fish.',
        'commands': [
            'As the owner, sneak and right-click with both hands empty to cycle through Free, Follow, Stay and Return to Nest. Raw cod or salmon in your off hand takes priority for feeding. Either hand can feed it; leave your main hand empty when using the off hand. Return to Nest looks for a nearby perch. Changing modes preserves any fish in its beak.',
            'Follow mode stays active during the day. When its owner is far away, the heron flies over without teleporting, then circles nearby. Choose Stay or Return to Nest when you want it to stop. The 32-block gift range does not limit ordinary following.',
        ],
        'fishing': [
            'Once tamed, a heron in Free or Follow mode may bring its owner a fish. No assigned task, pond, bank, fishing rod or live fish is needed.',
            'By default, it tries after every 5–10 minutes of eligible active time, with a 50% chance to place one raw cod or salmon directly in its beak and bring it to its owner. It carries at most one fish. An unsuccessful attempt waits for the next interval.',
        ],
        'gifts': [
            'The gift timer advances while the heron is safe and awake at dusk or nighttime, with its owner online, in the same dimension and within 32 blocks by default. Following during the day does not advance gift time. Offline time, rest, Stay and Return to Nest pause the timer.',
            'No new fish is generated while its beak is occupied. Reloading preserves a carried fish and the remaining wait; time spent offline does not generate extra gifts. Bird-watching settings let you disable gifts or adjust the owner range and attempt intervals.',
        ],
        'receiving': [
            'The heron carries its fish to its owner, lands safely, pauses briefly and places the fish on the ground in front of them. In Follow mode, it resumes circling nearby after delivery. You can also right-click it within three blocks with an empty main hand, without sneaking, to collect the fish directly into your inventory.',
            'If your inventory is full and has no matching fish stack with room, the heron keeps holding the fish. Free some space and right-click with an empty hand. Do not sneak, as that changes its companion command.',
        ],
        'fright': [
            'A wild heron’s alarm response progresses through freezing to observe, backing away, running, low flight and long-distance flight. Repeated chasing builds up fear and may also startle nearby wild herons.',
            'A tamed heron does not flee merely because an ordinary player approaches or wild birds nearby become frightened. It still avoids danger when attacked.',
        ],
        'settings': [
            'Setting  /  Default',
            'Night heron taming / fish gifts  /  Enabled; each can be disabled separately',
            'Taming chance per accepted feeding  /  1/3',
            'Maximum owner distance  /  32 blocks',
            'Interval between gift attempts  /  300–600 seconds',
            'Gift intervals count only safe, awake time at dusk or nighttime, with the owner online, in the same dimension and within range. Daytime following, offline time, rest, Stay and Return to Nest do not count. Each attempt has a 50% chance to place a fish directly in its beak for its owner. No waterside location, fishing rod or live fish is needed. The owner range applies to gifts, not ordinary following.',
        ],
    },
}


def paragraphs(texts):
    return [dict(type='p', id='', spans=[dict(text=text, link='')]) for text in texts]


def apply_gameplay_overrides(pages, lang):
    content = HERON[lang]
    page = next(p for p in pages if p['id'] == 'bird/night_heron')
    page['summary'] = content['summary']
    for section in page['sections']:
        if section['id'] in ('commands', 'fishing', 'gifts', 'receiving', 'fright'):
            section['blocks'] = paragraphs(content[section['id']])
        if section['id'] == 'fishing':
            section['title'] = '主动送鱼' if lang == 'zh_cn' else 'Fish gifts'
        elif section['id'] == 'gifts':
            section['title'] = '计时与保存' if lang == 'zh_cn' else 'Timing and saving'
    settings = next(p for p in pages if p['id'] == 'doc/settings')
    for section in settings['sections']:
        if section['id'] == 'night-heron':
            section['title'] = '夜鹭驯服与送鱼' if lang == 'zh_cn' else 'Night heron taming and fish gifts'
            section['blocks'] = paragraphs(content['settings'])
            section['blocks'].append(dict(type='p', id='', spans=[dict(
                text='夜鹭送鱼与收取' if lang == 'zh_cn' else 'Night heron fish gifts and collection',
                link='bird/night_heron#fishing')]))
