# -*- coding: utf-8 -*-
# One-shot generator for the expanded bird notes in this project. Writes:
#   1. the note block into assets/guaniao/lang/zh_cn.json and en_us.json
#   2. the whole content/note/BirdNoteContent.java
import json, os, sys
from pathlib import Path

BASE = Path(__file__).resolve().parent
ZH = os.path.join(BASE, "src", "main", "resources", "assets", "guaniao", "lang", "zh_cn.json")
EN = os.path.join(BASE, "src", "main", "resources", "assets", "guaniao", "lang", "en_us.json")
JAVA = os.path.join(BASE, "src", "main", "java", "EdDYON", "guaniao", "content", "note", "BirdNoteContent.java")

# (pool, key, title, author, zh, en)  pool: 0=ordinary 1=interesting 2=rare 3=secret 4=mutation
D = [
(0,"village_bath","观鸟入门","王小麦",'''我在水塘边看见一只小雀在洗澡，扑腾得可欢了，洗完还抖了抖毛。城里来的朋友总问我去哪看鸟，我说：打一盆水，等着就行。''','''I saw a small bird bathing in the pond today, splashing away happily, then shaking its feathers dry. City friends always ask where to watch birds. I say: put out a bowl of water and wait.'''),
(0,"village_crumbs","麻雀的口粮","村口的小花",'''我每天往窗台上撒一把面包屑，麻雀们准点来吃。有一只胆子特别大，敢从我手心里啄食。妈妈说这会把鸟惯懒，可我觉得，它们只是信任我。''','''I scatter a handful of breadcrumbs on the windowsill every day and the sparrows come on time. One is bold enough to eat from my open palm. Mother says this spoils them, but I think they just trust me.'''),
(0,"village_poop","白点点","老奶奶",'''栅栏上总有白色的点点，一开始我还以为是石灰，后来才知道那是鸟粪。洗了三遍才弄干净，气死我了。可那些小家伙还每天准时来，我又舍不的赶它们走。''','''There are always little white spots on the fence. I thought it was lime at first; turned out it was bird droppings. Took three scrubs to get clean, I was furious. But the little ones still come on time and I can't bare to chase them away.'''),
(0,"village_barn","谷仓里的新房客","铁匠老张",'''谷仓房梁上来了一窝燕子，每天早上都叽叽喳喳。它们在梁上筑了个泥窝，我搬东西都得小心翼翼。不过它们吃虫子，庄稼上的害虫少了不少，这就算交房租了吧。''','''A pair of swallows moved into my barn rafters and chatter every morning. They built a mud nest up there, so I move the hay carefully now. Still, they eat the insects off the crops — I call that their rent.'''),
(0,"village_crow","小偷与乌鸦","村长",'''村里的王铁匠丢了三颗金纽扣，找了半天，原来被乌鸦叼到树顶的窝里了。它们就喜欢亮闪闪的东西，比村里的孩子还贪财。下次我丢东西，先往树上找找准没错。''','''Blacksmith Wang lost three gold buttons; it turned out a crow had carried them to its nest in the treetop. They love shiny things, greedier than the village kids. Next time I lose something, I'll search the trees first.'''),
(0,"village_puddle","雨后的水坑","阿毛",'''下雨之后地上会有小水坑，鸟们排着队来喝水洗澡，就像赶集一样。我躲在草堆后面看，有一只居然再我脚边打了个滚。妈妈说鸟不怕小孩，她说得对。''','''After the rain there are little puddles and the birds queue up to drink and bathe like it's market day. I hid behind the haystack and one rolled over right by my foot. Mother says birds aren't afraid of children. She's right.'''),
(0,"village_chicken","鸡？！","王小麦",'''今天村里的孩子指着一只鸡大喊「好大的鸟！」我差点笑岔气。鸡也是鸟，这话没错，可它们又不飞，最多扑腾两下。不过想想也对，鸡也是鸟纲的一员。行吧，孩子没说错。''','''A village kid pointed at a chicken and yelled 'what a big bird!' I almost choked laughing. Chickens are birds, sure, but they don't fly — at most a flutter. Then I thought about it: the handbook does list chickens as birds. Fine. The kid wasn't wrong.'''),
(0,"village_morning","清晨的歌","磨坊主",'''天没亮就被鸟叫吵醒，气得我抓起扫帚，可开门一看，天边那道光照着满树的歌声，火气一下子就没了。我天天听它们叫，却从没数清过到底有几只。''','''The birds wake me before sunrise. I grabbed a broom in a fury once, but when I opened the door, the light on the singing trees stole my anger away. I hear them every day and still can't count how many there are.'''),
(0,"village_dont_startle","别吓到它们","花匠",'''观鸟最重要的不是好眼睛，是别出声。我见过太多次，一群人冲过去，鸟全飞了，一根毛都看不全。慢慢走，蹲下来，它们反而会凑近。还有，别穿太亮眼的衣服，红的黄的往那一站，鸟立马跑。''','''The most important thing in bird watching isn't good eyes — it's silence. I've seen it too many times: a crowd rushes in and the birds are gone before you see a single feather. Walk slow, crouch, and they come closer. Also, don't wear bright clothes; the moment red or yellow shows, they run.'''),
(0,"village_feeding","喂鸟要手稳","农夫阿牛",'''手心里的米撒了三次，鸟倒是吃饱了，我裤子口袋空空的。后来我学会了，喂鸟的时候手别斗，不然米全进土里。它们吃完还会歪头看你，像在问：还有吗？''','''I dropped the millet from my palm three times before the birds were fed and my pockets were empty. I learned: keep your hand stedy when feeding, or all the grain ends up in the dirt. They tilt their heads at you after eating, like asking: any more?'''),
(0,"village_fountain","水池边","村长",'''村子中央的水池边每天都有鸟来喝水，麻雀、斑鸠、还有几只叫不上名字的。它们把这里当成了聚会的地方，人来人往也不怕。''','''Birds gather at the village fountain every day — sparrows, doves, and a few I can't name. They treat it as a meeting place, unbothered by the comings and goings.'''),
(0,"village_dove","屋檐下的斑鸠","木匠阿福",'''斑鸠在屋檐下做了个松松垮垮的窝，就那么几根树枝，看着随时会掉。可它家的孩子一只只都好好长大了。我修了几十年房子，觉得这手艺比我的还稳。''','''A dove built a sloppy nest under my eaves — a few twigs that look ready to fall. Yet its chicks all grow up fine. I've repaired houses for decades; that nest is steadier than my work.'''),
(0,"village_winter","冬天的麻雀","老奶奶",'''天冷的时候麻雀会把自己吃得圆滚滚的，缩成一团蹲在墙头，像小毛球。我给它们留了一碗谷子，放在窗台下。下雪的那几天，它们吃得最勤。''','''In the cold, sparrows fluff themselves into round little balls on the wall. I leave them a bowl of grain by the windowsill. On snowy days they eat the most.'''),
(0,"village_harvest","收麦的时候","王小麦",'''收麦子的时候，地里的鸟比平时多好几倍。它们跟在我后头，捡掉落的麦粒，倒像是给我帮工。忙完一天回头看，田埂上站了一排，像在送我收工。''','''At harvest time the birds multiply tenfold. They follow me picking up dropped grain, like hired hands. At the end of the day a row of them lines the ridge, as if seeing me off.'''),
(0,"village_scarecrow","稻草人没用","王小麦",'''我扎了个稻草人立在田里，第一只乌鸦居然落在它头上。后来我看明白了：它不怕稻草人，它怕我。我在地里一站，它们才飞走。这稻草人算是白扎了。''','''I built a scarecrow. The first crow landed right on its head. Then it hit me: they don't fear the scarecrow, they fear me. The scarecrow was a waste.'''),
(0,"village_swallow_omen","燕子回家","村长",'''每年开春，燕子准时回到老屋的梁上，跟约好了一样。村里人都说燕子回来是吉兆，来年顺遂。我活了这些年，还没见过它们迟到。''','''Every spring the swallows return to the old beam on time, as if scheduled. The village says swallows are a good omen. In all my years they have never been late.'''),
(0,"village_rooster","公鸡打鸣","屠夫阿壮",'''有人问我天天杀鸡，鸡算不算鸟。算的，先生说过，鸡也是鸟。可公鸡天天打鸣提醒全村人天亮，我总得对它们客气一点，手艺归手艺，敬意归敬意。''','''People ask if chickens count as birds. They do, sir says so. But the rooster wakes the whole village; I should show it some respect. Craft is craft, but respect is respect.'''),
(0,"village_chick","小鸡认亲","屠夫阿壮",'''刚孵出来的小鸡会跟着第一个会动的东西走，我见过一只跟着扫帚走了半天，气得它妈追着啄扫帚。它们认人不认亲，谁喂它就跟谁走。养鸟养鸡都一个道理。''','''Newly hatched chicks follow the first moving thing. I saw one trail a broom for an hour while its mother attacked the broom. Feed them and they follow you. Birds and chickens, same rule.'''),
(0,"village_blacksmith_shiny","打铁声","铁匠老张",'''冬天冷，麻雀会溜进我的铁匠铺取暖，蹲在炉子边上的梁上，看我打铁。叮当叮当的，它们也不飞走，估计是习惯了这动静。来了一个月了，快成我店里的伙计了。''','''In winter sparrows slip into my smithy to warm up, perching on the beam and watching me hammer. The clanging doesn't scare them anymore. A month in, they're practically my apprentices.'''),
(0,"village_mason","脚手架上的麻雀","石匠老刘",'''我在脚手架上干活，麻雀就在旁边看着，歪着头，像在监工。我凿一下，它们跳一下，我歇口气，它们也歇。完工那天，它们还专门来验收，转了一圈才走。''','''As I work on scaffolding, sparrows watch me sideways, like supervisors. I chisel, they hop; I rest, they rest. On finishing day they came to inspect, circled once, and left.'''),
(0,"village_garden","花匠的帮手","花匠",'''我园子里的玫瑰老长虫子，正愁呢，来了几只山雀，把虫子当点心吃了个干净。它们不要工钱，只要花开的时候别赶它们走就行。这买卖划算。''','''Bugs were eating my roses until some tits moved in and ate them all like sweets. They charge no wage, only that I don't chase them when the flowers bloom. A fair trade.'''),
(0,"village_apothecary","药草园的客人","药剂师",'''药草园里种着薄荷和鼠尾草，常有几只鸟来啄叶片。我观察了很久，它们只啄有虫眼的那几片——它们是在帮我除虫，不是偷药。药草照样能入药，它们还免费。''','''Mint and sage grow in my herb garden, and birds come to peck the leaves. I watched closely: they only peck the bug-eaten ones. They weed for me, free of charge.'''),
(0,"village_librarian","书页里的羽毛","图书管理员",'''上次整理旧书架，在一本书里翻出一根羽毛，压在某一页，像书签。书上讲的是「观鸟之道」。我猜是哪位老读者留下的。没舍得扔，又夹了回去。''','''While sorting old shelves I found a feather pressed between pages like a bookmark — in a book about bird watching. Some old reader left it. I put it back.'''),
(0,"village_priest","教堂的钟声","牧师",'''钟一响，房顶的鸟就扑棱棱全飞起来，绕一圈又落回去。我起初觉得它们是被吵到了，后来发现它们就是在等着这一声，飞起来转一圈，是它们的早课。''','''When the bell rings the roof birds burst into the air, circle once, and land again. I thought it startled them; now I think they wait for it. It is their morning prayer.'''),
(0,"village_inn","酒馆的剩饭","酒馆老板",'''后院倒剩饭的地方，每天固定有几十只鸟来开饭，比我的客人还准时。有只灰鸽子嘴最刁，专挑土豆皮。它吃完还知道跳到窗台上，等第二轮的剩饭。''','''Behind the inn, the scraps draw dozens of birds daily, more punctual than my customers. One gray dove is picky, eating only potato peels. After eating it hops to the windowsill for round two.'''),
(0,"village_messenger","送信要快","信使",'''我给人送信，一趟跑好几个村子。有只麻雀跟着我飞了一路，我停它也停，我跑它追，像是要跟我比赛。到了村口它飞走了，也不知道它家在哪村。送信的不如会飞的，羡慕。''','''I deliver letters across villages. A sparrow flew alongside me the whole way, stopping when I stopped. At the next village it flew off. A courier on foot can't outfly a bird. Envy.'''),
(0,"village_bell","钟楼的住客","钟楼守夜人",'''钟楼里住着一窝鸟，就安在钟的边上。我拉绳敲钟，它们吓得飞起来，钟停了我再上去，人家又回去了，还冲我叫两声，好像嫌我吵它们睡觉。''','''A family of birds nests right by the bell. When I ring it they flee; the moment it stops they're back, scolding me for disturbing their sleep.'''),
(0,"village_greengrocer","菜摊的偷嘴","菜贩",'''菜摊上的菜叶，天天有鸟来偷嘴。西红柿啄一个洞，白菜帮子啃两口。我把菜盖起来，它们就蹲在棚上看我，一脸「我看你往哪藏」。罢了，就当喂了邻居。''','''Birds nibble my vegetables every day — a hole in a tomato, two bites out of a cabbage. When I cover them, the birds sit on the awning staring, as if to say "there's nowhere to hide". Fine, consider them neighbors.'''),
(0,"village_shepherd","羊群里的鸟","牧童",'''放羊的时候，有鸟落在羊背上，低头找跳蚤吃。羊也不赶它，走一路吃一路。我数了数，五只羊背上有三只鸟，它们比我还省心，羊都不用我管了。''','''When I herd sheep, birds land on their backs to eat the fleas. The sheep don't mind. I counted five sheep with three birds riding. They keep the flock better then I do.'''),
(0,"village_cowherd","牛背上的鸟","放牛娃",'''我家老牛背上天天站一只鸟，牛走到哪它跟到哪，一步也不离。我喊它，它只抬头看我一眼，又低下头去。它大概是把自己当成老牛的影子了，不过比影子胖。''','''A bird stands on my old ox all day, following wherever it goes. When I call, it glances at me and looks away. It thinks it's the ox's shadow — a plumper shadow.'''),
(0,"village_waterfetch","打水的乐趣","打水的孩子",'''我每天去井边打水，鸟就在旁边的篱笆上等我。我打满水，它们就飞下来喝桶沿上漏的水，叽叽喳喳的，像是在谢我。我数过，最多的时候来了七只。''','''Birds wait for me on the fence when I fetch water. When I fill the bucket they fly down to drink the spills, chattering like thanks. I've counted up to seven at once.'''),
(0,"village_mushroom","蘑菇与鸟","采蘑菇的孩子",'''采蘑菇的时候，我在苔藓里翻到一个鸟窝，里面躺着三颗浅蓝的蛋，圆圆的，比鸡蛋小多了。我没敢碰，轻轻盖回去。第二天再去，蛋还在，鸟妈妈在头顶叫个不停。吓得我放下蘑菇就跑。追我到村口。''','''While picking mushrooms I found a nest in the moss with three pale blue eggs, round and small. I covered it back gently. Next day the eggs were there and the mother scolded me all the way to the village edge. I ran.'''),
(0,"village_dragonfly","追蜻蜓","追蜻蜓的孩子",'''我追蜻蜓，蜻蜓飞，有只鸟也在追蜻蜓。我和鸟赛跑，鸟赢了。蜻蜓左拐，它左拐，蜻蜓急转，它急转，一下就把蜻蜓逮住了。它落在我前面的树枝上，看了我一眼，像是在说：学着点。''','''I chased a dragonfly; a bird chased it too. We raced. The bird won. It mirrored every twist, caught it midair, then perched ahead and looked at me like: learn something.'''),
(0,"village_rain","下雨前","老奶奶",'''要下雨的时候，鸟就不叫了，躲进树里，路上也看不见一只。村里人都看天，我看鸟。鸟一安静，我就知道该收衣服了。这法子比我那风湿的膝盖还准。''','''Before rain the birds go quiet and vanish into the trees. Villagers watch the sky; I watch the birds. When they fall silent, I take in the laundry. It's more reliable than my aching knees.'''),
(0,"village_dusk","黄昏归巢","村长",'''天一擦黑，鸟就朝着各自的树梢飞，吵吵闹闹地往回赶，跟村里收工回家的人一样。我站在村口看，天上的路和地上的路，原来是同一条。''','''At dusk the birds hurry back to their trees, noisily, just like the villagers coming home from the fields. I stand at the village gate and think: the road in the sky and the road on the ground are the same road.'''),
(0,"village_hungry","深冬觅食","王小麦",'''深冬的时候，地上冻得梆硬，鸟还在地上啄。啄一下，冰面一个白点，啄半天，也只找到一两粒漏下的种子。我给它们撒了把谷子，它们先是不敢来，后来一只一只的，排着队吃。''','''In deep winter the ground is frozen hard, yet birds still peck it — a white chip per peck, a seed or two for an hour. I scattered grain for them. They hesitated, then queued up one by one.'''),
(0,"village_fisher_heron","池塘边的鹭","渔夫老周",'''池塘边住着一只鹭，专抢我钓鱼的生意。我抛竿它就在对面站着，我起竿它扑过去，我钓上来的鱼，有三分之一是它吃掉的。我气不过，它倒理直气壮，像那池塘是它家的。''','''A heron lives by my pond and steals my fishing business. When I cast it stands across; when I reel in it strikes. A third of my catch goes to it. It acts as if the pond belongs to it.'''),
(0,"village_roof","屋顶的合唱","钟楼守夜人",'''天亮前的钟楼顶上，全村的鸟一起开嗓，那声音一浪接一浪。我守了一夜，就为听这一阵。它们叫得越响，我就知道天快亮了，该换班了。没有比这更准的报晓。''','''Before dawn the rooftops of the whole village break into song, wave after wave. I keep watch all night just for this. The louder they sing, the closer the shift change. No better wake-up call.'''),
(0,"village_snow","雪地里的爪印","牧童",'''下雪之后，地里全是小鸟的爪印，细细密密的，像谁在上面画了好多的叉。我顺着爪印走，找到一排它们蹦跳的足迹，一直连到树林边。原来鸟也喜欢踩雪玩。''','''After snow, the fields are covered in tiny bird prints, fine and crisscrossed. I followed them to a row of hopping tracks leading to the wood. So birds like to play in the snow too.'''),
(0,"village_wind","起风的时候","磨坊主",'''风大的日子，鸟顶着风飞，翅膀扇得急，飞半天还在原地。我站在磨坊门口看，突然觉得它们挺倔，明明可以歇一歇等风停，偏要跟风较劲。''','''On windy days birds fly against the gale, beating hard, making no progress. I watch from the mill door and think how stubborn they are — they could rest and wait, but they fight the wind.'''),
(0,"village_nest_secret","鸟窝的方位","石匠老刘",'''我盖了三十年房子，后来才发现鸟比我讲究。它们做窝，窝口都背着风，还挑太阳晒得着的方向。我要是早二十年跟它们学，我盖的房也不至于冬天漏风。''','''After thirty years of building I learned birds are fussier than me. Their nests face away from the wind and toward the sun. Had I learned from them twenty years ago, my houses wouldn't leak wind in winter.'''),
(0,"village_sunflower","向日葵上的雀","花匠",'''我种了一片向日葵，结籽之后，麻雀天天来吃。它们站在花盘上，头一低一抬，籽就少了。我摘下来晒干想留种，一数，少了三成。算了，就当请客了。''','''I planted sunflowers; once they seeded, sparrows came daily. Head down, head up, seeds gone. When I dried the heads for seed I found a third missing. Consider it a party I hosted.'''),
(0,"village_well","井沿的早晨","打水的孩子",'''早晨的井沿最好看。鸟排成一排站在井沿上，等第一桶水上来，抢着喝井绳上滴下来的水珠。我故意放慢摇水的速度，它们就歪着头看我，像在催我快点。''','''The well rim at dawn is the best view. Birds line up on it, waiting for the first bucket so they can drink the drops off the rope. When I slow the winch they tilt their heads at me, urging me on.'''),
(0,"village_lantern","灯笼下的蛾与鸟","菜贩",'''晚上菜摊挂灯笼招客人，也招蛾子。蛾子一多，鸟就来了，在灯影里上下翻飞，一只接一只地吃。我灯挂到几时，它们就吃到几时，比我的摊还敬业。''','''I hang a lantern at my stall for customers, and it draws moths. Where moths gather, birds come, swooping through the lamplight to eat them, one after another. They work longer hours than my stall.'''),
(1,"interesting_seagull","海鸟领航","老水手",'''海鸥跟着船飞，老船员说那是好运，其实它们是想捡船员扔的食物。别信什么「海鸥会救落水的人」，它们只会围着你叫，等你扑腾累了，好叼走你帽子上的羽毛。''','''Seagulls follow ships, and old sailors call it luck — really they're after the food the crew throws. Don't believe the tale that gulls rescue drowning sailors. They circle you, calling, waiting for you to tire so they can snatch the feather off your hat.'''),
(1,"interesting_macaw","会说人话的鹦鹉","丛林探险家",'''鹦鹉会学人说话，可它不明白意思。上次我教它几句骂骂冽冽的话，结果它见人就喊，我差点被当成怪人赶出村子。它们记性还特别好，你教的东西它记一辈子。''','''Parrots copy your words but don't know what they mean. I taught one a few rude phrases once and it greeted everyone with them; I was nearly chased out of the village as a madman. And they remember — teach them something and they keep it forever.'''),
(1,"interesting_nightheron","黄昏的猎手","守夜人",'''夜鹭白天缩在树上一动不动，像块木头。一到黄昏就活了，贴着水面飞，抓鱼快得看不清。我守夜时经常看见它们，那眼睛在暗处反光，吓我一跳。''','''A night heron stands motionless in a tree all day like a log. At dusk it comes alive, gliding over the water, grabbing fish faster than the eye can follow. On night watch I see them often; their eyes shine in the dark and gave me a start.'''),
(1,"interesting_mineshaft","矿洞里的旅鸟","矿工老李",'''矿洞里居然有鸟！它们在废弃的矿道上搭窝，靠吃蝙蝠和虫子过活。有几次我听到洞里叽叽喳喳，还以为是闹鬼。后来发现是鸟，心里踏实多了——总比鬼好。''','''Birds in a mineshaft! They nest on the abandoned rails and live off bats and bugs. I heard chirping down in the dark a few times and thought it was a ghost. Turned out to be birds. Much better than a ghost.'''),
(1,"interesting_tit","一团毛球","制图师",'''冬天你会在树枝上看到一串毛球——那是长尾山雀挤在一起睡觉，十几只抱成一团互相取暖。我数过，最多的一团有十七只。挤在最外面的那只总是被挤醒，再换到中间去。''','''In winter you see a clump of fluff on a branch — long-tailed tits huddled together to keep warm, a dozen or more in one ball. I counted seventeen once. The ones on the outside keep waking up and pushing into the middle.'''),
(1,"interesting_pigeon","信鸽的家","驿站的邮差",'''鸽子能从几百里外飞回来，不是因为聪明，是它们认得回家的路。我家那只送信回来，先绕村子转三圈，好像在显摆「我到家了」。其实它是饿了，等着投喂。''','''Pigeons can find their way home from hundreds of miles away — not from cleverness but from memory. Mine circles the village three times when it returns, as if showing off 'I'm home!'. Really it's just hungry and waiting to be fed.'''),
(1,"interesting_diet","鸟都吃什么","猎户",'''我打猎时顺带记过鸟吃什么：麻雀吃草籽，燕子吃飞虫，海鸥什么都吃，乌鸦也什么都吃。想知道一只鸟爱吃啥，看它的嘴形就八九不离十——尖的吃虫，粗的吃种。''','''While hunting I kept notes on what birds eat: sparrows eat grass seeds, swallows eat flies, seagulls eat anything, crows eat anything. Want to guess a bird's diet? Look at its beak — pointed means bugs, thick means seeds.'''),
(1,"interesting_desert","沙漠绿洲","商队向导",'''沙漠里看着什么都没有，可绿洲一出现，鸟就不知从哪冒出来了。它们喝水的样子凶得很，几只鸟抢一个水坑，打得羽毛乱飞。我在旁边看得斤斤有味，向导说我是个怪人。''','''The desert looks empty, but the moment an oasis appears, birds come out of nowhere. They drink ferociously, several fighting over one puddle, feathers flying. I watched with great interst. My guide thinks I'm strange.'''),
(1,"interesting_shipwreck","沉船上的海鸥","幸存的水手",'''船沉了之后，海鸥占了我的桅杆。我漂在木板上，它们在我头顶开会，像是在讨论怎么处理我这个多余的东西。最后一只海鸥落下来，叼走了我最后一块干粮。生死关头，我笑了。''','''After my ship sank, the seagulls claimed my mast. I drifted on a plank while they held a meeting overhead, as if deciding what to do with the extra cargo. One gull landed and took my last biscuit. At death's door, I laughed.'''),
(1,"interesting_steal","被抢走的三明治","遇难的商人",'''我在海边吃午饭，一只海鸥俯冲下来，整块三明治就没了。不是叼走，是抢——它连我的手指都咬了一下。后来我听说海鸥连村民手里的食物都敢抢。别在海边吃东西，这是用教训换来的。''','''I was eating lunch by the sea when a seagull dived and the whole sandwich was gone. Not snatched — robbed. It even bit my finger. Later I heard gulls steal from villagers' hands too. Don't eat by the sea. That lesson cost me a sandwich.'''),
(1,"interesting_trader_route","商队的歌","商队向导",'''商队走沙漠，头顶总有几只鸟跟着，不叫不闹，就跟着。老向导说，那是路神的使者，跟着它们不会迷路。我没见过路神，但跟着鸟走，确实没迷过路。''','''Crossing the desert, a few birds trail the caravan, quiet and constant. The old guide says they are the road god's messengers; follow them and you never lose your way. I've never met the road god, but following the birds, I've never been lost.'''),
(1,"interesting_navigation","星星与鸟","老水手",'''老辈水手在海上找方向，不只看星星，还看鸟。白天有海鸟飞来的方向，多半是陆地；黄昏它们往哪边飞，哪里就有岸。星星在夜里指路，鸟在白天指路。海上的日子，全靠它们。''','''Old sailors read direction not only from the stars but from birds. Where seabirds fly from by day is usually land; where they fly at dusk is shore. Stars guide the night, birds the day. At sea, we lean on both.'''),
(1,"interesting_parrot_mimic","学舌的边界","丛林探险家",'''丛林里有一种鹦鹉，能把一句话一字不差地学出来，连语气都对。我教它「你好」，它回我「你好」；我教它骂人，它骂得比我还利索。它们学人话，却永远不知道自己在说什么。像一部会飞的留声机。''','''Some jungle parrots copy a whole sentence exactly, even the tone. I taught one "hello"; it said "hello". I taught it a curse; it swore better than me. They mimic speech without ever understanding it — a flying gramophone.'''),
(1,"interesting_tit_ball_fall","雪团坠落","制图师",'''冬天我画地图，休息时看见树上一团东西掉下来，在雪地里滚了几圈，散开——是十几只长尾山雀。它们本来挤成球取暖，被风摇掉了。散开后叽叽喳喳，又爬回树上重新抱团。''','''While mapping in winter I saw a clump drop from a tree and roll in the snow — a dozen long-tailed tits. They'd huddled into a ball for warmth and the wind shook them loose. Scattered, they chattered, climbed back, and re-balled.'''),
(1,"interesting_pigeon_homing_trick","认路的花招","驿站的邮差",'''都说鸽子认路是靠天上的磁力，我看它们靠的是路。我们驿站的信鸽，放出去先贴着大道飞，拐弯的地方认识路标。它们不是天生认路，是天生记路。跟人一样，走熟了自然认得。''','''They say pigeons navigate by magnetism. I say they navigate by roads. Our courier pigeons fly along the main roads and know the landmarks at each turn. Not born knowing — born remembering. Like people, they learn by walking.'''),
(1,"interesting_hunter_quiet","猎人要学会安静","猎户",'''好猎手的第一课不是射箭，是安静。树上有鸟，你嗓门一大，整片林子的动物都知道了。我进林子先看鸟：鸟啄食，说明这里安全；鸟抬头乱飞，说明有危险，我得跟着警觉。''','''A hunter's first lesson isn't archery, it's silence. Raise your voice near a bird and the whole forest knows. I read the forest by its birds: pecking means safe; sudden flight means danger — I take my cue from them.'''),
(1,"interesting_mine_bird_dark","黑暗中的鸟","矿工老李",'''矿洞深处的鸟，我起初以为是蝙蝠。后来点灯一看，是鸟，眼睛大大的，不怕光。它们在黑暗中靠叫声认路，一声接一声，像在跟洞壁对话。我听得多了，竟觉得那声音不那么可怕了。''','''At first I took the mine's deep birds for bats. Lamp on, they were birds — big-eyed, unafraid of light. They navigate the dark by calls, one echoing the next, like talking to the walls. Hear it long enough and it stops being frightening.'''),
(1,"interesting_watchfire","守夜人的火堆","守夜人",'''夜里守城楼，点了火堆，就有鸟绕着火飞。是虫子往火里扑，鸟追着虫。火光照着它们的身影，一圈一圈。有时候我会想，它们到底是来取暖，还是来加餐，也许都有。''','''On night watch I light a brazier and birds circle it. Moths dive the flame; birds chase the moths. Firelight paints their loops against the dark. Whether they come for warmth or supper — probably both.'''),
(1,"interesting_fisher_knowledge","会钓鱼的鹭","渔夫老周",'''鹭吃鱼，我也吃鱼，我们算半个同行。它站在浅水里一动不动，鱼游过来，它一嘴下去，稳、准、狠。我看它的姿势，学了两手，还真管用。同行是冤家，但老师也是它。''','''Herons eat fish and so do I — fellow professionals. It stands still in the shallows and strikes sure and fast. I copied its stance into my fishing. Rivals, yes, but also my teacher.'''),
(1,"interesting_cartographer_height","高度与迁徙","制图师",'''我画地形图，把候鸟的迁徙路线也标了上去。它们走河谷、越山脊，跟我标的最省力的商路几乎重合。原来不是我懂地理，是鸟先懂。地图上的路，鸟比我画得早。''','''I map terrain and have started marking migration routes too. They follow river valleys and cross ridgelines — nearly the same as my most efficient trade routes. I didn't learn geography; the birds knew it first. The roads on my maps, birds drew earlier.'''),
(1,"interesting_messenger_speed","谁更快","驿站的邮差",'''有人吹牛说他的马快，我不服，拿信鸽跟他比。鸽子从村东飞到村西，马跑一半，信先到了。他认了输，把最好的马草分了一半给鸽子。从此驿站的鸽子，伙食比马好。''','''Someone bragged his horse was faster. I raced him with a carrier pigeon. It flew the village east-to-west while the horse ran half way. He conceded and split his best hay with the pigeon. Our pigeons now eat better than the horses.'''),
(1,"interesting_alchemist","炼金术士的鸟","药剂师",'''药房的老客人是位炼金术士，他坚信鸟能找到矿脉，说什么「啄石之鸟，其下有矿」。我当他是疯话。直到他按一只鸟常啄的山崖挖下去，真挖出了铜。从此他信鸟，我信他。''','''An old customer, an alchemist, swore birds can find ore veins — "where birds peck stone, ore lies beneath." I took it for madness until he dug where one bird always pecked and found copper. Now I believe him.'''),
(1,"interesting_astronomer","仰望的鸟","天文学家",'''我研究星象，却常被白天的鸟分心。它们清晨朝东飞，黄昏朝西飞，整整齐齐。后来我才明白，它们也在看太阳，靠太阳认方向。天上的人看着星，天上的鸟看着太阳，各看各的。''','''I study the stars yet often lose focus on the daytime birds. They fly east at dawn, west at dusk, orderly. It dawned on me: they read the sun for direction. Stargazers watch stars; birds watch the sun. Each minding its own sky.'''),
(1,"interesting_herbalist","药草与羽毛","药草师",'''老方子里有一味「鸟羽」，说是治风寒。我采药二十年，从不用它——羽毛入药，多半是图个念想。不过有一味真的有用：用鸟粪灰敷伤口，止血比草药还快。这个方子我记在药典边角上。''','''Old remedies call for "bird feather" for colds. Twenty years gathering herbs, I never use it — feathers are more faith than medicine. But one recipe is real: bird-dropping ash stops bleeding faster than herbs. I keep it in the margin of my pharmacopoeia.'''),
(1,"interesting_traveller_tales","旅人的歌","流浪歌手",'''我编了一首歌，唱旅人跟着鸟找路的事，走到哪唱到哪。有次在酒馆唱完，一个老水手红着眼眶说，他年轻时真的跟着海鸟找回过岸。歌是真的，我才敢一直唱下去。''','''I wrote a song about travelers finding their way by following birds, and sing it wherever I roam. Once, after a tavern set, an old sailor said with wet eyes that he really had found shore by following seabirds once. The song is true; that's why I keep singing it.'''),
(1,"interesting_swamp","沼泽的鹭","采药人",'''沼泽里采药，总能看见鹭。它们站在烂泥里，从早站到晚，比我还沉得住气。有一回我陷进淤泥，是远处一只鹭先叫起来，惊动了同伴，也惊来了路过的人把我拉出来。它算救了我一命。''','''Gathering herbs in the swamp I always see herons, standing in the muck from dawn to dusk, more patient than me. Once I sank into the mud; a far heron's call raised the alarm, drew passers-by, and they pulled me out. That heron saved my life.'''),
(1,"interesting_coast","崖边的巢","灯塔看守人",'''灯塔边的悬崖上，海鸟的窝密密匝匝，一只挨一只。夜晚灯一亮，它们就醒了，绕着灯塔飞，翅膀扇得灯影忽明忽暗。我在塔顶守了半辈子，它们的叫声就是我的作息。''','''On the cliffs by the lighthouse, seabird nests crowd side by side. When the lamp flares at night they wake and wheel around it, shadowing the beam. Half a life at the top, their cries are my clock.'''),
(1,"interesting_storm_warning","风暴的预报","老水手",'''风暴要来，海鸥先知道。平常它们跟着船抢食，要变天的时候，全往内陆飞，一只不剩。老船员看见海鸥上岸，二话不说收帆找港。海鸥比天气哨兵准，也比它们话少。''','''Gulls know a storm before it comes. Normally they shadow the ship for scraps; before a blow, all of them fly inland, not one left. Old sailors see gulls going ashore and stow sail without a word. Gulls beat the weather-glass and talk less.'''),
(1,"interesting_crow_count","数乌鸦","守夜人",'''我们这儿有个说法：一只乌鸦报忧，两只乌鸦报喜，三只乌鸦来客。我守夜闲着没事数乌鸦，从没数出过准数——它们飞来飞去，你怎么数都数不对。后来我明白了，这规矩是逗人玩的。''','''We have a saying: one crow spells sorrow, two spell joy, three spell a guest. On watch I count crows for fun and never get the same number twice — they keep flying about. Then I understood: the saying is a joke on us.'''),
(1,"interesting_nest_materials","巢的材料","木匠阿福",'''春天我的工坊丢东西：细绳、羊毛、铁皮条，还有一把铜钉。顺着找，全在一棵树上——有只鸟拿我的材料盖了座大宅子，连铜钉都叼走了。我不生气，我那工坊可盖不出那么漂亮的窝。''','''In spring my workshop loses things: string, wool, strips of tin, a handful of copper nails. I traced them to a tree — a bird built a mansion out of my supplies, nails included. I'm not angry; my workshop couldn't build a nest that fine.'''),
(1,"interesting_mirror","镜子里的自己","老奶奶",'''我家窗上挂了一面旧镜子，有只麻雀天天来，对着镜子又扑又啄，闹了半天。我起先以为它疯了，后来明白它是把镜子里的自己当成了对手，天天来打一架。今天它终于飞走了，估计是想通了。''','''I hung an old mirror by my window. A sparrow comes daily to attack its own reflection, pecking and flapping. I thought it mad, then understood: it mistakes its reflection for a rival and comes to fight. Today it flew off — it must have figured it out.'''),
(1,"interesting_diet_change","换季的胃口","猎户",'''鸟的胃口会随季节变。春天爱吃虫，因为要给小鸟喂肉；秋天猛吃果子种子，囤过冬的膘；冬天专挑谷物，好对付天寒。看一只鸟在吃什么，就能猜出现在的季节，比翻日历准。''','''Birds change diet with the seasons. Spring they eat insects, to feed their chicks meat; autumn they gorge on fruit and seeds, storing fat; winter they pick grains to survive the cold. Read what a bird eats and you know the season better than any calendar.'''),
(1,"interesting_migration_line","人字形","商队向导",'''大雁南飞排成「人」字形，最前面那只领飞最吃力，过一阵就换一只上去领。我带着商队走沙漠，学着它们轮流开路，队伍果然走得轻松些。路是人走出来的，也是鸟教会我走的。''','''Geese fly south in a V; the leader works hardest and they take turns at the front. Guiding caravans through the desert, I copied them, rotating who breaks trail, and the march went easier. Roads are walked by people, but birds taught me how.'''),
(1,"interesting_forest_song","森林的歌","丛林探险家",'''丛林里天亮那一刻，声音最响。不是风声水声，是鸟，成百上千只一起开口，像整片林子醒过来伸了个懒腰。我走了半辈子林子，每个早晨还是会被这声音震住，愣在原地听一会儿。''','''The loudest moment in the jungle is the instant of dawn — not wind or water, but birds, hundreds of them starting at once, like the whole forest waking with a stretch. Half a life in the woods and that sound still stops me in my tracks.'''),
(1,"interesting_tide","潮汐与海鸥","灯塔看守人",'''海鸥跟着潮水走。涨潮时它们贴着浪尖找被卷上来的小鱼，退潮时落在露出的滩涂上啄食。我守着灯塔看了一辈子，它们的饭点就是潮汐的时刻表。你问它们几点开饭，抬头看海就行。''','''Gulls keep time with the tide. They hunt the wave-tops as it comes in, and the exposed flats as it goes out. A lifetime at the lighthouse tells me their mealtimes are the tide table. Ask a gull when dinner is — look at the sea.'''),
(1,"interesting_kingfisher_dream","青蓝的影子","药草师",'''有人说河边有一种青蓝色的鸟，像一道光贴着水面飞，快得看不清。我在河边守了三天，只瞥见一个影子，蓝得不像真的。采药的人都说那是传说。可传说总得有人先看见，才传得下去。''','''They say a blue-green bird haunts the river, a flash of light over the water, too fast to see. I watched three days and caught only a shadow, blue beyond belief. Herbalists call it legend. But a legend must first be seen by someone, or there'd be nothing to tell.'''),
(2,"rare_portal","穿越门户的鸟","卫道士",'''我看见一只麻雀钻进下界传送门，又飞回来。它出来的时候羽毛焦了，眼神不太对。我追了它半座山，它居然绕着一座遗迹转圈。鸟不会这么做。那不是鸟，是别的东西穿着鸟皮。''','''I saw a sparrow fly into a nether portal and come back out. Its feathers were singed and its eyes were wrong. I chased it across half a mountain and it kept circling an old ruin. Birds don't do that. That was not a bird wearing feathers.'''),
(2,"rare_gold_feather","金色的羽","资深观鸟者",'''三十年观鸟，我只见过一次纯金色的鸟。它停在清晨的树梢上，阳光一照，浑身像熔化的金子。我屏住呼吸看了半分钟，它转头飞走了，再也没见过。老伴说我在编故事，可我手上有一根掉落的金羽，收在盒子里，谁也不给看。''','''Thirty years of watching birds and I saw a pure golden one exactly once. It perched on a treetop in the morning sun, shining like molten gold. I held my breath for half a minute; then it turned and flew off, never seen again. My wife says I made it up. But I keep a fallen golden feather in a box I show no one.'''),
(2,"rare_poop_weapon","粪便是武器","卫道士",'''我们林地府邸的守卫都知道，别站在鸟的正下方。它们能精准地把白色炸弹扔进你的头盔缝里。我试过反击，扔石头，它们飞得比石头快。最后我妥协了，戴了一顶更大的帽子。''','''We mansion guards all know: never stand directly under a bird. They can drop a white bomb precisely into the gap of your helmet. I tried throwing rocks back; they fly faster than rocks. In the end I gave up and wore a bigger hat.'''),
(2,"rare_lava","熔岩边的倒影","被诅咒的旅人",'''有人说在下界看见过鸟的影子，贴着熔岩飞，翅膀不煽也能滑行。我以为是幻觉，直到有一天我也开始看见。它停在我的影子里，比我还安静。别往下界带活物，也别带走那里的羽毛。''','''Some say there are bird shadows in the nether that glide over the lava without flapping. I thought it was my imaganation, until I started seeing it too. It rests in my shadow, quieter than I am. Don't bring living things into the nether, and don't bring its feathers out.'''),
(2,"rare_black_shadow","黑鸟的影子","守夜人",'''全黑的鸟比想象中更难看见。我见过一只通体漆黑的乌鸦，混进夜色里，只剩一双眼睛。老人们说黑鸟不吉利，我看他们是怕黑。它明明就是只乌鸦，吃相还特别斯文。''','''An all-black bird is harder to see than you'd think. I saw a pitch-black crow melt into the night until only its eyes showed. The elders say black birds are ill omens. I think they're afraid of the dark. It was just a crow — and a well-mannered eater.'''),
(2,"rare_grudge","会记仇的鸟","老猎人",'''别惹乌鸦。十年前我吓跑过一只乌鸦的幼鸟，这十年它每年春天都来我屋顶拉屎，还带它的子孙来。我搬了家，第二年新家的屋顶上，站着同一只乌鸦。它们认脸。''','''Never offend a crow. Ten years ago I startled a crow's chick; for ten years it has come to my roof every spring to relieve itself, and brought its family. I moved house. The same crow was on the new roof the next year. They remember faces.'''),
(2,"rare_migration","迁徙的路线","资深观鸟者",'''候鸟迁徙的路线不是直的，是沿着河流和山脊走的。它们认得地标，比人用的地图还准。有人说鸟靠太阳和星星认路，可我觉得，它们就是天生知道该往哪飞，像心里装了指南针。''','''Migrating birds don't fly in a straight line — they follow rivers and ridgelines, reading landmarks better than any map. Some say they steer by the sun and stars. I think they simply know where to go, as if a compass were built into them.'''),
(2,"rare_piglin","羽毛换黄金","猪灵",'''金羽毛，金羽毛！旅行的人说有金羽毛的鸟，大大的鸟！我们用黄金换羽毛，亏了，亏大了。可是金羽毛好看。给不给都行，反正黄金本来就是我们的。''','''Gold feathers, gold feathers! Travellers speak of a bird with gold feathers, a big bird! We trade gold for feathers. Bad trade, bad! But gold feathers pretty. Give or not give, gold is ours anyway.'''),
(2,"rare_soldier_watch","军中的号角","退伍士兵",'''军中待了二十年，耳朵练出来了。营地的鸟叫分好几种：平常的啄食叫，交班的起落叫，还有警戒的报警叫。野外的鸟一乱，我就知道有动静，比放哨的兄弟还早一步。我教的新兵，先学听鸟，再学听号。''','''Twenty years in the army trained my ears. Birdsong has kinds: the pecking idle, the shift-change flight, and the alarm. When wild birds panic I know something stirs, before the sentry. New recruits learn to read birds before they learn the bugle.'''),
(2,"rare_mercenary","佣兵的酬劳","佣兵",'''有个雇主付不起金币，拿一根羽毛抵账，说什么「金羽」。我当他是耍赖，把羽毛别在帽子上当笑话戴。结果路过的老商人看见了，出价三倍要买。我没卖。现在我信了：有的羽毛，比黄金值钱。''','''A client couldn't pay in gold and offered a feather, claiming it was "golden." I wore it in my cap as a joke. A passing merchant saw it and offered triple. I didn't sell. Now I believe him: some feathers are worth more than gold.'''),
(2,"rare_brute","蛮兵的战利品","猪灵蛮兵",'''我守着金，谁抢金，我砸谁。有一次抢金的人带来一根金羽毛，亮得晃眼。我抢过来，金子都不要了，只要它。它比金子好看。从此我守的不只是金，还有这根羽毛。谁也别想拿走。''','''I guard gold; whoever steals gold, I smash. Once a raider brought a golden feather, dazzling. I took it and forgot the gold. It outshines gold. Now I guard not just gold but this feather. No one takes it.'''),
(2,"rare_guard_captain","巡逻的鹰眼","守卫队长",'''巡逻二十年，我学会看鸟。城门口的鸟稳稳当当，城里就没大事；它们忽然飞得又高又乱，城里必有事发生。手下笑我信鸟不信人。我跟他们说：人会撒谎，鸟不会。后来他们自己也看鸟了。''','''Twenty years on patrol taught me to read birds. When the gate birds are calm, the city is calm; when they fly high and frantic, something's wrong inside. My men mocked me for trusting birds over people. I told them: people lie, birds don't. They watch birds now too.'''),
(2,"rare_border","边境的渡鸦","边境巡逻兵",'''边境的渡鸦又大又黑，专吃战场上剩下的东西。它们总在打仗前聚集，像闻到了什么。老兵说，渡鸦来得多的年头，边境必不太平。我巡逻时数渡鸦，数得越多，心里越沉。今天又来了几只。''','''Border ravens are huge and black, feeding on what battle leaves behind. They gather before wars, as if they smell it coming. Old soldiers say the years the ravens flock are years the border bleeds. I count ravens on patrol. The count is rising. More came today.'''),
(2,"rare_veteran_falcon","驯鹰","老猎人",'''我年轻时驯过一只鹰，喂它、磨它的性子、教它认我。出猎时它替我上天，找猎物的本事比我强十倍。它不算是我的伙计，我们更像是合伙的。后来它老了，我放它走了。鹰这种东西，你驯得服它的身体，驯不服它的心。''','''In my youth I kept a hawk — fed it, tempered it, taught it to know me. Hunting, it took to the sky and found game ten times better than I. Not my servant; my partner. When it aged I let it go. You can train a hawk's body, never its heart.'''),
(2,"rare_herbalist_secret","羽毛入药","炼金术士",'''我这辈子找一种东西：一根会发光的羽毛。古籍上说，用它入药，能治最顽固的病。我找了几十年，翻遍了药书，问遍了采药人，只拿到半页传说。可传说写得很清楚：那羽毛，比命还难找。''','''All my life I have sought one thing: a feather that glows. Old books say it cures the stubbornest illness. Decades of searching, every herb book, every herbalist — I have only half a page of legend. But the legend is clear: that feather is harder to find than a life.'''),
(2,"rare_astronomer_comet","彗星与鸟","天文学家",'''那年天现彗星，连着一个月，村子里的鸟都不见了，连麻雀都找不到一只。老人说，鸟看见了天要变，提前跑了。我不知道鸟是不是真的看见彗星，但那天之后，我学会了看天，也学会了看鸟。''','''The year a comet appeared, for a month the village birds vanished — not a sparrow anywhere. Elders said the birds saw the sky changing and fled. I can't say whether birds see comets, but since that year I watch the sky, and I watch the birds.'''),
(2,"rare_naturalist","博物学家的标本","博物学家",'''我把见过的鸟都记在册子上：羽毛的颜色、叫的声音、飞的姿势，一样一样记。有四种我只见过一面，想再找却怎么也找不到，像躲着我。册子越写越厚，可我知道，真正的珍品，是那些我写不进去的。''','''I catalogue every bird I see: plumage, call, flight, each in its column. Four species I've met only once and can never find again, as if they hide from me. The ledger grows fat, but I know the true treasures are the ones I cannot write down.'''),
(2,"rare_geomancer","地脉","地理学家",'''有人说鸟飞的方向跟地下的矿脉有关，我半信半疑地记了十年：每张地图上，把同一群鸟的路线画下来，发现它们确实绕着某些山脊走。我顺着画出的线去查，那几条线底下，真藏着矿。鸟看得见地底，人看不见。''','''I half-believed that birds fly along the ore veins below, so I mapped ten years of their routes. The lines loop certain ridgelines — and where the lines ran, ore was found. Birds see beneath the earth; we cannot.'''),
(2,"rare_librarian_forbidden","禁书区","图书馆长",'''图书馆最里头有一间禁书区，上着锁，钥匙在我这里。书堆里有一本没有作者的书，讲一种「月下的鸟」。我照书上的记号找过，记号指向的地方，什么都没有。可那本书的页脚，夹着一根发白的羽毛。''','''The library's innermost room is forbidden, locked, key with me. Among the books is an authorless one about a "moonlight bird." I followed its marks once; they led nowhere. But between its pages lay a feather, gone white.'''),
(2,"rare_scroll","卷轴上的羽毛","史官",'''整理古卷时，我在一卷几百年前的羊皮卷里翻出一根羽毛，压得平平整整。卷上记着一位君王豢养「月鸟」的事，说那鸟只在满月啼鸣，一鸣则国泰民安。我不知道真假，但把那根羽毛，原样压回了卷里。''','''Sorting ancient scrolls I found a feather pressed flat in a parchment centuries old. It records a king who kept a "moon bird," said to sing only at the full moon, its song promising peace. I cannot say if it's true. I pressed the feather back where it was.'''),
(2,"rare_volcano","火山口的鸟","登山者",'''火山口的热气往上冒，托着鸟在山顶盘旋，翅膀都不用扇。我爬了半辈子山，头一回见鸟这么省力。山下的人说那是火神的鸟，我不懂神，但我知道，风把谁托起来，谁就能少飞一会儿。''','''The volcano's heat rises and lifts birds in endless circles above the crater, wings idle. Half a life of climbing, first time I saw birds get a free ride. The valley folk call them fire-god's birds. I know nothing of gods, but I know: who the wind lifts can rest awhile.'''),
(2,"rare_dungeon","地牢的伴生","洞穴探险家",'''地牢深处住着一种白眼睛的鸟，吃银鱼和灰尘里的虫。它们不怕人，跟着火把走，像向导又像看客。探险队里有人说这种鸟不吉利，我倒觉得，有它们的地方，至少说明这里还没被彻底放弃。''','''Deep dungeons hold pale-eyed birds that eat silverfish and dust mites. Unafraid, they follow your torch like guides or spectators. Some in the crew call them ill omens. I think where they live, a place isn't fully abandoned yet.'''),
(2,"rare_treasure","藏宝图上的爪印","寻宝者",'''我得了半张藏宝图，图角有一串爪印，不像人画的路，倒像鸟踩出来的。我顺着爪印的方向找，在一片老林子里，真挖出了前人埋的箱子。箱子里没有金银，只有一窝鸟蛋。我合上箱子，原样埋了回去。''','''Half a treasure map came to me, its corner marked by a trail of claw prints, more bird than human. Following the prints into an old wood I dug up a chest — not gold, but a bird's nest of eggs. I closed it and buried it as I found it.'''),
(2,"rare_omen","三只乌鸦","老预言家",'''有人问我，三只乌鸦齐飞是什么兆头。我说：是乌鸦，三只，结伴出门找食罢了。他们嫌我答得不玄乎，非说那是灾祸的前兆。也罢，你们若信乌鸦能预知，就先去看看它到底在吃什么。答案都在地上。''','''People ask what it bodes when three crows fly together. I answer: it's crows, three of them, heading out to forage. They find me disappointingly un-ominous and insist it's a portent. Fine — if you believe crows foresee, first look at what they're eating. The answer is on the ground.'''),
(2,"rare_ravager","劫掠兽与鸟","卫道士",'''我们林地府邸的劫掠兽，走到哪，身后就跟着一圈鸟。它们等着劫掠兽刨土翻出虫子，捡现成的。劫掠兽凶，对鸟却从不发火，大概是习惯了这群跟班。连兽都有跟班的，我们卫道士倒没有。''','''Wherever our ravagers go, a ring of birds follows, waiting for the beast to root up insects. The ravager, fierce to all else, never minds them — used to its retinue. Even a beast has followers; we vindicators have none.'''),
(2,"rare_piglin_trade","下界的集市","猪灵",'''下界有集市，什么都换。有人拿羽毛来换金子，我一开始觉得他傻。后来看见一根火一样红的羽毛，我一整袋金子都掏了出去。红羽毛好看，红羽毛比金贵。集市散了，我还攥着它，舍不得花。''','''The nether has markets where anything trades. Someone offered feathers for gold; I thought him a fool. Then I saw a feather red as fire and gave a whole bag of gold for it. Red feathers are prettier, red feathers outprice gold. Market over, I still clutch it, can't bear to spend it.'''),
(2,"rare_soldier_drill","列队","退伍士兵",'''军中有列队操练，练了几十年。后来我才发现，天上的鸟也在列队，一只领飞，其余跟着，队形不散。我教新兵排队，就拿鸟打比方：别抢、别掉队、轮到你领就领。新兵听得懂，因为天上天天在示范。''','''The army drills formations; I drilled them for years. Then I noticed the sky does it too — one bird leads, the rest follow, the line holds. I teach recruits formation with birds as the example: don't rush, don't lag, lead when it's your turn. They get it, because the sky demonstrates daily.'''),
(2,"rare_blood_moon","血月","守卫队长",'''血月那夜，全城的鸟都在叫，叫得人头皮发麻。老兵们关紧门窗，说血月夜里鸟的叫声不是报信，是报丧。天亮时城里丢了两户人家，谁也不知道去了哪。从那以后，我听见鸟夜里惊叫，一定起身查哨。''','''On the night of the blood moon every bird in the city screamed until it crawled your scalp. Veterans barred the doors, saying a bird's scream on a blood moon isn't warning but mourning. By dawn two households were simply gone. Since then, if birds cry at night, I go check the watch myself.'''),
(3,"secret_whisper","远古的低语","佚名",'''远古城市的地底回荡着低语，像一群看不见的鸟在互相呼唤。我在那里捡到一根羽毛，凉得像骨头。谁也不知道那些声音是什么，但我知道——它们听着所有走近的人。''','''Deep beneath the ancient city, whispers echo like invisible birds calling to one another. I found a feather there, cold as bone. No one knows what the sounds are. I know they are listening to all who come near.'''),
(3,"secret_end","世界的边缘","末地学者",'''有人把末地当作终点，我却在这里寻找鸟的痕迹。传说有一种带着星光的鸟，翅膀划过天空时会留下微光，比末地本身还古老。我守了很久，还没见到。但我相信它存在，就像相信风。''','''People treat the End as a dead end; I came here looking for birds. Legend speaks of a star-winged bird whose passing leaves a trail of light across the sky, older than the End itself. I have watched long and seen nothing yet. But I believe it exists, the way you believe in wind.'''),
(3,"secret_forgotten","失传的观鸟法","图书馆长",'''古代图书馆里有一本没有书名的观鸟手册，字迹潦草得像鸟爪划的。上面说：观鸟的最高境界，是让鸟以为你是棵树。我试了一整天，站到腿麻，真有一只麻雀落在我肩上。它啄了啄我的耳朵，发现我不是树，飞走了。''','''In the old library there is an unnamed bird handbook, the handwriting like marks of bird claws. It says: the highest art of bird watching is to make the birds take you for a tree. I stood a whole day until my legs ached — and a sparrow really did land on my shoulder. It pecked my ear, realised I was not a tree, and flew away.'''),
(3,"secret_first_bird","第一只鸟","佚名",'''传说世界刚造好的时候，只有天和海。第一只鸟从海里飞出来，抖落的水珠变成了雨，羽毛变成了云。它一直飞，飞了几万年，累了，就落在一棵树上，那棵树从此有了名字。这个故事是不是真的不重要。重要的是，你抬头看天的时候，还会不会想起它。''','''They say when the world was new there was only sky and sea. The first bird flew out of the sea; the water it shook off became rain, and its feathers became clouds. It flew for ages, grew tired, and settled on a tree, which has had a name ever since. Whether it is true matters little. What matters is whether you still look up.'''),
(3,"secret_ancient_library","沉没的图书馆","图书馆长",'''有人告诉我在深海下面有一座沉没的图书馆，墙上的壁画画满了鸟。我半信半疑地找了许多年，最后在一张老地图的角落找到它的标记。我没敢下水去，但我把那个标记抄了下来。如果有谁比我勇敢，请替我看一眼那些壁画。''','''Someone told me of a drowned library beneath the deep sea, its murals painted with birds. Half-doubting, I searched for years and found its mark in a map's corner. I never dared dive. But I copied the mark down. If anyone braver than me reads this, look at those murals for me.'''),
(3,"secret_astrologer","星图上的鸟","占星师",'''我画星图几十年，从没见过一颗会动的星。直到有一天，我在两片星云之间看见一只「鸟」——不是星星排成的形状，是会飞的星。它飞过的地方，星光暗下去一小会儿。我揉了揉眼，它还在。我画进了星图，没人信我。''','''I've charted stars for decades and never seen one move. Then one night between two nebulae I saw a "bird" — not a shape made of stars, but a star that flies. Where it passed, the starlight dimmed a moment. I rubbed my eyes. It remained. I drew it into my chart. No one believes me.'''),
(3,"secret_watcher","观察者","佚名",'''这本笔记不是写给读者看的，是写给我的。我一直在记录鸟，却总觉得有一双眼睛在记录我。鸟停在我窗台的时候，眼睛不是看虫，是看我。我不知道它是什么，但我知道，它在等我说出最后一句话。''','''This note is not for readers; it is for me. I record birds, yet always feel a pair of eyes recording me. When a bird perches on my sill, it does not watch for bugs — it watches me. I do not know what it is. But I know it is waiting for me to speak the last word.'''),
(3,"secret_moon_bird","月之鸟","老预言家",'''有个传说我守了一辈子：满月之夜，会有一只鸟站在最高的屋顶上，对着月亮叫。它的羽毛在月光下会慢慢变白。有人说那是吉兆，有人说那是诅咒。我守了许多个月圆，还没见过它。但我见过月光下变白的影子，一次。''','''A legend I've kept a lifetime: on the full moon a bird stands on the highest roof and cries at the moon, its feathers slowly turning white in the moonlight. Some call it blessing, some curse. I have watched many full moons and never seen it. But once, I saw a shadow turning white in the moonlight.'''),
(3,"secret_farlander","远行者的信","末地学者",'''一位远行者从世界边缘寄来一封信，信里夹着一根发光的羽毛。他说，在世界最远的那座岛上，有一只鸟，翅膀不是羽毛做的，是光做的。它绕着岛飞了一圈，天就亮了一次。我不知道该不该信，但那根羽毛，至今还在发光。''','''A farlander sent me a letter from the edge of the world, a glowing feather folded inside. He wrote that on the farthest island lives a bird whose wings are not feathers but light; it circled the island once and the day rose. I don't know whether to believe him. The feather is still glowing.'''),
(3,"secret_underworld","深处的回声","佚名",'''远古城市的地底，除了低语，还有鸟叫。不是回声，是真正的鸟叫，从石缝里传出来。我带了一盏灯下去，走了很久，在尽头看见一窝鸟，羽毛灰白，眼睛是闭着的。它们不需要光。我在黑暗里坐了很久，然后原路退了回去。''','''Beneath the ancient city, beyond the whispers, there is birdsong — not echo, true song, seeping from cracks in stone. I took a lamp down, walked long, and at the end found a nest of birds, grey-white, eyes closed. They need no light. I sat in the dark a while, then retreated the way I came.'''),
(3,"secret_song","消失的歌","史官",'''宫廷里有一首失传的歌，乐师们找了几百年也找不回曲谱，只记得最后一句：飞吧，别回头。前些日子我在城外听到一只鸟，把这一句唱得一字不差。我追上它，它飞进林子不见了。也许歌没有消失，只是换了谁来唱。''','''The court lost a song centuries ago; musicians searched forever for its score, remembering only the last line: fly, and do not look back. The other day outside the city a bird sang that line exactly. I followed; it vanished into the wood. Perhaps the song never died — only changed singers.'''),
(3,"secret_keep","禁忌图书馆的看守","看守者",'''我守着一根羽毛，比这个图书馆的年纪还老。它装在一个铅盒里，没人敢打开。上任看守交给我时说：别让它见天光，见了天光，就要飞。我守了三十年，铅盒没动过。但今夜，我在盒子里听到了一声心跳。''','''I guard a feather older than this library, sealed in a lead box no one dares open. My predecessor handed it over with one warning: keep it from the light, for in the light it will fly. Thirty years the box has not moved. But tonight, I heard a heartbeat inside it.'''),
(3,"secret_census","第一百只鸟","佚名",'''有人让我数清这个国家有多少只鸟。我数了十年，每一次数完，结果都不一样。第十一年，我数出了第一百只鸟——然后我发现，那只鸟在数我。我们隔着田野对望了很久。我合上本子，从此不数了。它替我数。''','''I was asked to count every bird in this land. Ten years of counting, and each total differed. In the eleventh year I counted the hundredth bird — and saw it counting me. We regarded each other across the field for a long time. I closed my ledger and never counted again. It counts for me now.'''),
(3,"secret_eggs","蛋的秘密","图书馆长",'''古籍里有一个说法：世界上的第一颗蛋，包着世界上所有的颜色。鸟把它孵出来，颜色就散了出去——白色的给了白鸟，金色的给了金鸟，彩虹的，只给了那一只。古籍在最后一页写着：别把蛋打碎，颜色会跑回天上去。''','''An old book claims the first egg in the world held every color there is. When a bird hatched it, the colors scattered — white went to the white birds, gold to the golden, and the rainbow went to only one. The final page warns: do not break the egg, or the colors will return to the sky.'''),
(3,"secret_dreamer","梦游者的话","梦游者",'''我每晚都会梦游，醒来时总在村外的高坡上。家里人说我每晚都往同一个方向走。我记不清梦里的路，只记得一只鸟在梦里领着我飞。有一次我醒来，手里攥着一根灰白的羽毛。不是我的。也不是这个世界的。''','''I walk in my sleep each night; my family finds me at the high slope beyond the village, always walking the same direction. I can't recall the dream roads, only that a bird led me flying through them. Once I woke clutching a grey-white feather. It was not mine. It is not from this world.'''),
(3,"secret_shadow_king","影子王","佚名",'''所有的鸟都在一个影子的统领之下。它没有名字，没有颜色，只在所有鸟的影子重叠的时候出现。有人说那影子是传说。可我见过一次：太阳正中，千万只鸟的影子合成一个，站在我面前。它点了点头，像在确认什么，然后散成千万只。''','''All birds answer to a shadow. It has no name, no color, and appears only when every bird's shadow overlaps. Some call it legend. But once, at high noon, the shadows of ten thousand birds merged into one that stood before me. It nodded, as if confirming something, and scattered back into ten thousand.'''),
(3,"secret_rainbow_secret","彩虹的秘密","占星师",'''我一生都在找彩虹之鸟。占星书上说，它只在雨后、在云隙的镜子前现身。我以为那是比喻，直到有一天，我在一片积水的云影里，看见了流动的颜色。我追了三步，它飞走了。但我记住了它的位置：水，就是它的镜子。''','''All my life I have sought the rainbow bird. The star books say it appears only after rain, before a mirror in a gap of cloud. I took it for metaphor, until one day, in the reflection of a puddle holding a cloud, I saw moving colors. I chased three steps; it flew. But I kept its secret: water is its mirror.'''),
(3,"secret_clock","无钟的时辰","史官",'''宫廷的钟坏过三年，没人修好。但那三年里，御花园的鸟每天准时啼叫，臣子们听着鸟叫上朝，分毫不差。后来钟修好了，鸟的叫声反而没人听了。我把这件事写进史书，主编删掉了，说「不合史法」。可它真的发生过。''','''The palace clock was broken for three years, beyond repair. Yet the garden birds sang on time every day, and the court convened by them, exactly. When the clock was fixed, no one listened to the birds anymore. I wrote this into the chronicle; the editor cut it as "unseemly." But it truly happened.'''),
(3,"secret_silence","寂静之日","末地学者",'''一年里有一天，全世界的鸟都会同时沉默，连笼中的鸟也不叫。古籍说那是世界在换气。我为了证实，在野外守了一整年，终于等到那一天：早上鸟还叫着，正午突然全静下来，整整一刻钟，没有一声鸟叫。然后，又像什么也没发生。''','''One day each year every bird on earth falls silent at once — even caged birds. Old books say it is the world drawing breath. To test it I camped in the wild for a full year and caught the day: birdsong in the morning, then at noon total silence, a full quarter hour without a single call. Then, as if nothing had happened.'''),
(3,"secret_inherit","传承","佚名",'''读到这里，这份笔记就是你的了。我写下它，不是为名，是为有一天有人翻开它，知道这个世界的鸟不全是书里写的那样。它们有自己的传说，自己的秘密。现在，轮到你替它们记着了。别写错名字，别记错颜色。''','''Reading this far, the note is yours. I wrote it not for fame but so that someday someone opens it and knows the birds of this world are more than the books claim. They keep their own legends, their own secrets. Now it is your turn to remember for them. Do not misspell their names. Do not mistake their colors.'''),
(5,"secret_dev_programmer","敲门三下","蛋炒饭",'''窗口常有鸟来。有一只特别讲道理：它想进来，就啄三下窗玻璃，等我开窗。我不开，它就不走，坐在窗台上，一直看着我。有一次我故意不开，它叫了整整一上午。最后我认输了。现在它每天都来，啄三下，等我开窗。我不懂鸟语，但我觉得它是在骂我懒。''','''Birds come to my window all the time. One of them is very polite: when it wants in, it pecks the glass three times and waits for me to open the window. If I don't, it won't leave — it sits on the sill and just watches me. Once I refused on purpose and it called for an entire morning. I gave in. Now it comes every day, pecks three times, and waits. I don't speak bird, but I'm pretty sure it's calling me lazy.'''),
(5,"secret_dev_keeper","学咳嗽","伊洛哥斯拉",'''我家有只鸟，别的都好，就一个毛病：爱学我咳嗽。我感冒那阵咳了三天，病好了，它倒把咳嗽学会了，见我就咳，学得一模一样。客人来家里，听见鸟咳嗽都吓一跳，以为屋里藏着个人。我跟它商量，让它别咳了。它停下来，看了我一眼，然后咳得更大声了。''','''I have a bird at home, perfectly well-behaved except for one habit: it copies my cough. I had a cold and coughed for three days; once I got better, the bird had learned it and coughed at me, pitch-perfect. Guests come over, hear a bird cough, and nearly jump — they think there's someone hiding in the house. I tried to reason with it, asked it to stop. It stopped, looked at me, then coughed even louder.'''),
(5,"secret_dev_animator","歪头大赛","多雨",'''有只鸟特别爱歪头看人。你站着不动，它歪头；你歪头，它也歪头；你再歪，它能把头歪到快躺平。我跟它比了十分钟，最后是我先受不了了。它赢了我，得意地叫了一声，飞走了。第二天它又来了，还是那套。我觉得它上瘾了。''','''There's a bird that loves tilting its head at you. You stand still, it tilts; you tilt, it tilts back; you tilt harder, and it can get its head almost horizontal. We held a contest for ten minutes, and I gave up first. It won, chirped smugly, and flew off. The next day it came back and did the whole thing again. I think it's addicted.'''),
(5,"secret_dev_sound","破音","老三",'''我用树叶吹口哨能吹出各种调子。家门口有只鸟，我吹什么它回什么，吹得比我还准。我想逗逗它，故意吹了个破音。它愣了一下，然后回了我一个更破的破音，还带拐弯的。我笑出声来。它又叫了一声，特别像在笑。''','''I can whistle all sorts of tunes through a leaf. There's a bird by my door that answers every note I play, and it's more accurate than I am. To tease it, I cracked a note on purpose. It paused, then cracked one right back — a wobblier crack. I laughed out loud. It chirped again, and it really sounded like laughing.'''),
(5,"secret_dev_modeler","斜着飞","千年村庄",'''小时候我把家里的木鸟玩具拆了，想看看里面藏着什么。拆开一看，里面空空荡荡，就是一块整木头。我七扭八歪地装回去，翅膀一边高一边低。我妈回来一看，说：哟，这鸟学会斜着飞了。从那以后我明白了一个道理——东西拆开再装回去，多半会歪。但我还是爱拆。''','''When I was little I took apart my wooden bird toy to see what was hidden inside. Turned out nothing — it was one solid block of wood. I put it back together all lopsided, one wing higher than the other. When my mother came home she said: oh look, that bird learned to fly crooked. From then on I knew one thing for sure — take anything apart and put it back, and it comes out crooked. But I still love taking things apart.'''),
(4,"leucistic","关于白鸟的传说","佚名",'''传说在极北的森林里，住着一只通体雪白的鸟。它的羽毛白得像初雪，落在树影里几乎看不见它。有人说是雪把它染白的，也有人说它生来就是这般雪白。想找到它，你得在雪后的清晨出发。''','''Legend tells of a bird pure white as snow living in the far northern forests. Its feathers look like fresh snow, nearly invisible among the shadows. Some say the snow bleached it; others say it was born that white. To find it, set out on a morning after snowfall.'''),
(4,"melanistic","关于黑鸟的传说","佚名",'''老人们说，深山里有一只黑得像炭的鸟，连眼睛都隐没在黑暗里。它只在云遮住月亮的时候出现，像一片会飞的阴影。见过它的人都说，那不是鸟，是夜晚本身。''','''The elders say a bird black as coal lives deep in the mountains, its eyes lost in the dark. It appears only when clouds cover the moon, like a shadow that flies. Those who saw it say it was not a bird but the night itself.'''),
(4,"golden","关于金鸟的传说","佚名",'''有旅行者写信说，他在某个村庄的粮仓顶上看见一只金色的鸟，羽毛在夕阳下泛着柔和的金光，不像任何寻常的鸟。他追了一路，那鸟飞过山脊便消失了。如果你也见过金色的鸟，请别急着告诉别人——也许它就在你附近。''','''A traveller wrote that he saw a golden bird on a village granary roof, its feathers gleaming softly in the sunset, unlike any ordinary bird. He chased it all the way until it vanished beyond the ridge. If you too see a golden bird, don't tell just anyone — it may be right nearby.'''),
(4,"puregold","关于纯金之鸟的传说","佚名",'''最珍贵的传说，属于一只「纯金之鸟」。它比金色的鸟更加耀眼，羽毛像被熔化的黄金浇过，却没有一丝杂色。据说它在最偏远的角落出没，一生也未必能被看见一次。若你真遇见了它，那便是命运。''','''The rarest legend belongs to a 'pure-gold bird'. More dazzling than the golden one, its feathers look poured from molten gold without a single flaw. It is said to haunt the farthest corners, perhaps glimpsed once in a lifetime. If you ever meet it, that is fate.'''),
(4,"rainbow","关于彩虹之鸟的传说","佚名",'''有笔记记载，某个雨后的傍晚，有人看见一只鸟飞过云隙，身上的羽毛流动着彩虹般的光，仿佛有无数颜色在它身上奔跑。那不是染色，也不是光线的戏法——那鸟自己就会发光。至今没有人再见过它。若你遇见了彩虹之鸟，请一定把它画下来。''','''A note records that one rainy evening someone saw a bird fly through a gap in the clouds, its feathers flowing with rainbow light, as if countless colours were running across it. It was no dye, no trick of light — the bird itself glowed. No one has seen it since. If you meet the rainbow bird, be sure to paint it.'''),
]

def note_lines(lang_index):
    """lang_index 0 -> zh (index 4 in tuple), 1 -> en (index 5)."""
    lines = []
    for row in D:
        key, zh, en = row[1], row[4], row[5]
        val = zh if lang_index == 0 else en
        lines.append('  "%s": %s,' % ("note.guaniao.body." + key, json.dumps(val, ensure_ascii=False)))
    return "\n".join(lines) + "\n"

def rewrite_lang(path, lang_index):
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    start = text.index('  "note.guaniao.body.village_bath"')
    end = text.index('\n', text.index('  "note.guaniao.body.rainbow"'))
    new_text = text[:start] + note_lines(lang_index) + text[end + 1:]
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(new_text)
    print("rewrote", os.path.basename(path), "->", new_text.count("note.guaniao.body."), "body keys")

def java_note_arrays():
    pools = [[] for _ in range(4)]
    for row in D:
        pool, key, title, author = row[0], row[1], row[2], row[3]
        if pool >= 4:
            continue
        pools[pool].append('new NoteTemplate("%s", "%s", "%s")' % (title, author, key))
    blocks = []
    for pool in range(4):
        inner = ",\n".join("                    " + p for p in pools[pool])
        blocks.append("            {\n%s\n            }" % inner)
    return ",\n".join(blocks)

def java_dev_notes():
    rows = [row for row in D if row[0] == 5]
    lines = ["            new NoteTemplate(\"%s\", \"%s\", \"%s\")" % (row[2], row[3], row[1]) for row in rows]
    return ",\n".join(lines)

def java_mutation_titles():
    titles = [row[2] for row in D if row[0] == 4]
    return ", ".join('"%s"' % t for t in titles)

JAVA_TEMPLATE = '''package EdDYON.guaniao.content.note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * Generates the lootable "bird note" written books that appear in structure chests.
 *
 * <p>Every note is written by its own author — a farmer, a blacksmith's kid, a
 * shepherd boy, a vindicator, a piglin brute, an astrologer, an anonymous keeper —
 * so the tone, the topics (baths, food, habits, nests, droppings, omens, rumours)
 * and even the spelling vary wildly. A village chest usually holds plain gossip; an
 * ancient-city chest usually holds something closer to a secret. Rarely, a note
 * tells a legend about the special mutations (leucistic/melanistic/golden/
 * pure-gold/rainbow). A few secret notes are easter eggs written by the mod's
 * own developers, drawn from the secret pool at the same odds as any other
 * secret note.
 */
public final class BirdNoteContent {
    /** NBT marker so "read the note" gameplay knows this written book is a mod note. */
    public static final String NOTE_TAG = "GuaniaoNote";

    /** Chest difficulty tiers. */
    private static final int VILLAGE = 0;
    private static final int MEDIUM = 1;
    private static final int NETHER = 2;
    private static final int ANCIENT = 3;

    /** Secrecy level of the rarest pool, where the dev easter-egg notes live. */
    private static final int SECRET = 3;

    /** Chance that an eligible chest contains a note at all, per tier. */
    private static final double[] NOTE_CHANCE = {0.20, 0.25, 0.30, 0.35};

    /**
     * Weighted secrecy levels (ordinary/interesting/rare/secret) per chest tier.
     * A village chest mostly gives ordinary notes but can rarely reach a secret one.
     */
    private static final double[][] TIER_WEIGHTS = {
            {0.795, 0.16, 0.04, 0.005},
            {0.40, 0.42, 0.15, 0.03},
            {0.15, 0.40, 0.35, 0.10},
            {0.03, 0.20, 0.50, 0.27},
    };

    /** Chance a note of a given secrecy level is about a special bird. Overall lands near ~1/10. */
    private static final double[] SPECIAL_BY_SECRET = {0.02, 0.12, 0.35, 0.75};

    /** Which mutation a legend describes, weighted per chest tier (rarer mutations in harder places). */
    private static final double[][] MUTATION_WEIGHTS = {
            {35, 40, 20, 4, 1},
            {25, 30, 30, 10, 5},
            {15, 20, 30, 20, 15},
            {10, 15, 25, 25, 25},
    };

    private static final String[] MUTATION_KEYS = {"leucistic", "melanistic", "golden", "puregold", "rainbow"};
    private static final String[] MUTATION_TITLES = {__MUTATION_TITLES__};
    private static final String MYSTERY_AUTHOR = "佚名";

    /** One note: its literal title/author and the translatable body key. */
    private record NoteTemplate(String title, String author, String bodyKey) {
    }

    /** Pool of notes for each secrecy level (ordinary/interesting/rare/secret). */
    private static final NoteTemplate[][] NOTES = {
__NOTES__
    };

    /**
     * Easter-egg notes written by the mod's own developers. They live in the
     * secret pool and roll at the same odds as any other secret note.
     */
    private static final NoteTemplate[] DEV_NOTES = {
__DEV_NOTES__
    };

    private static final Map<String, NoteTemplate> NOTES_BY_KEY = new HashMap<>();

    static {
        for (NoteTemplate[] pool : NOTES) {
            for (NoteTemplate note : pool) {
                NOTES_BY_KEY.put(note.bodyKey, note);
            }
        }
        for (NoteTemplate note : DEV_NOTES) {
            NOTES_BY_KEY.put(note.bodyKey, note);
        }
    }

    private static final Set<String> MEDIUM_TABLES = Set.of(
            "desert_pyramid", "jungle_temple", "shipwreck_treasure", "shipwreck_supply",
            "shipwreck_map", "pillager_outpost", "igloo_chest", "underwater_ruin_big",
            "underwater_ruin_small", "buried_treasure", "abandoned_mineshaft", "simple_dungeon",
            "woodland_mansion", "stronghold_corridor", "stronghold_crossing", "stronghold_library");
    private static final Set<String> NETHER_TABLES = Set.of(
            "nether_bridge", "bastion_treasure", "bastion_other", "bastion_bridge",
            "bastion_hoglin_stable", "ruined_portal", "end_city_treasure");

    private BirdNoteContent() {
    }

    /** Returns a note book for this loot context, or {@link ItemStack#EMPTY} when no note spawns. */
    public static ItemStack roll(LootContext context) {
        int tier = tierIndex(context.getQueriedLootTableId());
        if (tier < 0) {
            return ItemStack.EMPTY;
        }
        RandomSource random = context.getRandom();
        if (random.nextDouble() >= NOTE_CHANCE[tier]) {
            return ItemStack.EMPTY;
        }
        int secret = rollIndex(random, TIER_WEIGHTS[tier]);
        if (random.nextDouble() < SPECIAL_BY_SECRET[secret]) {
            int mutation = rollIndex(random, MUTATION_WEIGHTS[tier]);
            return makeBook(MUTATION_TITLES[mutation], MYSTERY_AUTHOR, "note.guaniao.body." + MUTATION_KEYS[mutation]);
        }
        NoteTemplate note = pickNote(random, secret);
        return makeBook(note.title, note.author, "note.guaniao.body." + note.bodyKey);
    }

    private static NoteTemplate pickNote(RandomSource random, int secret) {
        if (secret != SECRET) {
            return NOTES[secret][random.nextInt(NOTES[secret].length)];
        }
        // Dev easter eggs roll at the same odds as ordinary secret notes.
        int normalCount = NOTES[SECRET].length;
        int roll = random.nextInt(normalCount + DEV_NOTES.length);
        if (roll < normalCount) {
            return NOTES[SECRET][roll];
        }
        return DEV_NOTES[roll - normalCount];
    }

    /** Builds the note book for a body key ("leucistic", "village_poop", ...), matching its title and author. */
    public static ItemStack noteFor(String bodyKey) {
        for (int i = 0; i < MUTATION_KEYS.length; i++) {
            if (bodyKey.equals(MUTATION_KEYS[i])) {
                return makeBook(MUTATION_TITLES[i], MYSTERY_AUTHOR, "note.guaniao.body." + bodyKey);
            }
        }
        NoteTemplate note = NOTES_BY_KEY.get(bodyKey);
        if (note == null) {
            return makeBook("佚名的笔记", MYSTERY_AUTHOR, "note.guaniao.body." + bodyKey);
        }
        return makeBook(note.title, note.author, "note.guaniao.body." + bodyKey);
    }

    /** Returns every possible note book: the 128 villagers' notes, the 5 dev easter eggs and the 5 mutation legends. */
    public static List<ItemStack> allNotes() {
        List<ItemStack> notes = new ArrayList<>();
        for (NoteTemplate[] pool : NOTES) {
            for (NoteTemplate note : pool) {
                notes.add(noteFor(note.bodyKey));
            }
        }
        for (NoteTemplate note : DEV_NOTES) {
            notes.add(noteFor(note.bodyKey));
        }
        for (String mutationKey : MUTATION_KEYS) {
            notes.add(noteFor(mutationKey));
        }
        return notes;
    }

    /** Returns one representative book from each secrecy level for the creative tab. */
    public static List<ItemStack> creativeTabNotes() {
        List<ItemStack> notes = new ArrayList<>(NOTES.length);
        for (NoteTemplate[] pool : NOTES) {
            if (pool.length > 0) {
                notes.add(noteFor(pool[0].bodyKey));
            }
        }
        return notes;
    }

    private static ItemStack makeBook(String title, String author, String bodyKey) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag marker = new CompoundTag();
        marker.putByte(NOTE_TAG, (byte) 1);
        marker.putString("author", author);
        EdDYON.guaniao.util.ItemData.write(book, marker);
        book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
                new net.minecraft.world.item.component.WrittenBookContent(
                        net.minecraft.server.network.Filterable.passThrough(title), author, 0,
                        List.of(net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.translatable(bodyKey))), true));
        return book;
    }

    private static int rollIndex(RandomSource random, double[] weights) {
        double total = 0.0;
        for (double weight : weights) {
            total += weight;
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0.0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    private static int tierIndex(ResourceLocation table) {
        if (!"minecraft".equals(table.getNamespace())) {
            return -1;
        }
        String path = table.getPath();
        if (!path.startsWith("chests/")) {
            return -1;
        }
        String key = path.substring("chests/".length());
        if (key.startsWith("village/") || key.equals("spawn_bonus_chest") || key.equals("desert_well")) {
            return VILLAGE;
        }
        if (key.equals("ancient_city")) {
            return ANCIENT;
        }
        if (MEDIUM_TABLES.contains(key)) {
            return MEDIUM;
        }
        if (NETHER_TABLES.contains(key)) {
            return NETHER;
        }
        return -1;
    }
}
'''

def write_java():
    java = JAVA_TEMPLATE
    java = java.replace("__NOTES__", java_note_arrays())
    java = java.replace("__DEV_NOTES__", java_dev_notes())
    java = java.replace("__MUTATION_TITLES__", java_mutation_titles())
    with open(JAVA, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(java)
    print("wrote BirdNoteContent.java with", D.__len__(), "notes (",
          sum(1 for r in D if r[0] < 4), "regular +",
          sum(1 for r in D if r[0] == 5), "dev +",
          sum(1 for r in D if r[0] == 4), "mutation)")

if __name__ == "__main__":
    rewrite_lang(ZH, 0)
    rewrite_lang(EN, 1)
    write_java()
