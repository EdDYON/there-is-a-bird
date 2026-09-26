package EdDYON.guaniao.porttest;

import EdDYON.guaniao.content.advancement.BirdAdvancements;
import EdDYON.guaniao.content.dropping.*;
import EdDYON.guaniao.util.ItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("guaniao_port_tests")
@PrefixGameTestTemplate(false)
public final class GameplayParityTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everyDroppingThrowsAndLeavesAnImpactSplat(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var shots = new ArrayList<BirdDroppingProjectileEntity>();
        var targets = new ArrayList<BlockPos>();
        int index = 0;
        for (var variant : BirdDroppingVariant.values()) {
            var target = h.absolutePos(new BlockPos(2 + index++ * 3, 2, 3));
            level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
            player.setPos(Vec3.atCenterOf(target).add(0, 3, 0));
            player.setXRot(90);
            var stack = new ItemStack(variant.item(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.getItem().use(level, player, InteractionHand.MAIN_HAND);
            h.assertTrue(player.isUsingItem(), "Charging did not begin: " + variant);
            stack.getItem().releaseUsing(stack, level, player, stack.getUseDuration(player) - 20);
            player.stopUsingItem();
            h.assertTrue(stack.getCount() == 1, "Throw did not consume one item: " + variant);
            var shot = level.getEntitiesOfClass(BirdDroppingProjectileEntity.class,
                    new AABB(target).inflate(6)).stream().filter(e -> !shots.contains(e)).findFirst().orElseThrow();
            h.assertTrue(shot.getVariant() == variant && shot.getItem().is(variant.item()), "Projectile variant lost");
            var saved = new CompoundTag();
            shot.saveWithoutId(saved);
            var copy = shot.getType().create(level);
            copy.load(saved);
            h.assertTrue(((BirdDroppingProjectileEntity)copy).getVariant() == variant, "Saved variant lost");
            shots.add(shot);
            targets.add(target);
        }
        h.runAfterDelay(12, () -> {
            for (int i = 0; i < shots.size(); i++) {
                h.assertTrue(shots.get(i).isRemoved(), "Projectile failed to hit the floor");
                var splats = level.getEntitiesOfClass(BirdDroppingSplatEntity.class, new AABB(targets.get(i)).inflate(1));
                h.assertTrue(splats.size() == 1, "Expected one visible splat after impact, got " + splats.size());
            }
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void dispenserDroppingsPulseAllFourPlateTypes(GameTestHelper h) {
        var level = h.getLevel();
        Block[] types = {Blocks.OAK_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE,
                Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE};
        var positions = new ArrayList<BlockPos>();
        for (int i = 0; i < types.length; i++) {
            var plate = h.absolutePos(new BlockPos(2 + i * 3, 2, 4));
            positions.add(plate);
            level.setBlockAndUpdate(plate.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(plate, types[i].defaultBlockState());
            var dispenser = plate.above(3);
            level.setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, net.minecraft.core.Direction.DOWN));
            var stack = new ItemStack(BirdDroppingVariant.values()[i].item(), 2);
            DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem()).dispense(new BlockSource(level, dispenser,
                    level.getBlockState(dispenser), (DispenserBlockEntity)level.getBlockEntity(dispenser)), stack);
            h.assertTrue(stack.getCount() == 1, "Dispenser did not consume exactly one dropping");
        }
        h.runAfterDelay(10, () -> {
            for (var pos : positions) {
                h.assertTrue(level.getBestNeighborSignal(pos.below()) > 0, "Impact did not power plate: " + level.getBlockState(pos)
                        + " shots=" + level.getEntitiesOfClass(BirdDroppingProjectileEntity.class, new AABB(pos).inflate(6)).stream().map(e -> e.position() + " age=" + e.tickCount).toList());
            }
        });
        h.runAfterDelay(50, () -> {
            for (var pos : positions) {
                h.assertTrue(level.getBestNeighborSignal(pos.below()) == 0, "Plate did not turn off: " + level.getBlockState(pos));
            }
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void foodCraftingAndActualFloorImpactsBothMakePrankFood(GameTestHelper h) {
        var level = h.getLevel();
        var targets = new ArrayList<BlockPos>();
        int index = 0;
        for (var variant : BirdDroppingVariant.values()) {
            for (var food : List.of(Items.BREAD, Items.POTION)) {
                var input = CraftingInput.of(2, 1, List.of(new ItemStack(food), new ItemStack(variant.item())));
                var recipe = new BirdDroppingFoodRecipe(CraftingBookCategory.MISC);
                h.assertTrue(recipe.matches(input, level) && PrankFoodUtil.isPrankFood(recipe.assemble(input, level.registryAccess())), "Crafting failed");
                var target = h.absolutePos(new BlockPos(2 + index % 4 * 3, 2, 2 + index / 4 * 5));
                index++;
                targets.add(target);
                level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
                var item = new ItemEntity(level, target.getX() + 0.5, target.getY() + 1.02, target.getZ() + 0.5, new ItemStack(food, 3));
                item.setDeltaMovement(Vec3.ZERO);
                item.setNoGravity(true);
                item.setNeverPickUp();
                level.addFreshEntity(item);
                shootDown(h, target, variant);
            }
        }
        h.runAfterDelay(12, () -> {
            for (var target : targets) {
                var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(1.4));
                int clean = 0, dirty = 0;
                for (var item : items) {
                    if (PrankFoodUtil.isPrankFood(item.getItem())) dirty += item.getItem().getCount();
                    else clean += item.getItem().getCount();
                }
                h.assertTrue(clean == 2 && dirty == 1, "Impact must dirty exactly one food: clean=" + clean + ", dirty=" + dirty);
            }
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void thrownDroppingPollutesCake(GameTestHelper h) {
        var target = h.absolutePos(new BlockPos(5, 2, 5));
        h.getLevel().setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(target, Blocks.CAKE.defaultBlockState());
        shootDown(h, target, BirdDroppingVariant.FOUR);
        h.runAfterDelay(12, () -> {
            h.assertTrue(PollutedCakeData.get(h.getLevel()).isPolluted(target), "Actual impact did not pollute cake");
            h.succeed();
        });
    }

    private static void shootDown(GameTestHelper h, BlockPos target, BirdDroppingVariant variant) {
        var shot = new BirdDroppingProjectileEntity(h.getLevel(), h.makeMockPlayer(GameType.SURVIVAL), variant);
        shot.setPos(Vec3.atCenterOf(target).add(0, 3, 0));
        shot.setDeltaMovement(0, -0.8, 0);
        h.getLevel().addFreshEntity(shot);
    }

    @GameTest(template = "empty", batch = "natural_dropping", timeoutTicks = 100)
    public static void naturalAndLaxativeDroppingsReachTheGround(GameTestHelper h) {
        var level = h.getLevel();
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "NearbyPlayer");
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
        var target = h.absolutePos(new BlockPos(5, 2, 5));
        player.setPos(Vec3.atCenterOf(target).add(5, 1, 0));
        level.addNewPlayer(player);
        // Separate landing pads: the second projectile can otherwise hit the
        // first pickable decal, which is unrelated to testing these two entry points.
        for (int x = -1; x <= 5; x++) for (int z = -1; z <= 1; z++) {
            level.setBlockAndUpdate(target.offset(x, 0, z), Blocks.STONE.defaultBlockState());
        }
        var bird = (net.minecraft.world.entity.Mob)EdDYON.guaniao.config.BirdSpecies.SEAGULL.entityType().create(level);
        bird.setPos(Vec3.atCenterOf(target).add(0, 4, 0));
        bird.setNoAi(true);
        bird.setNoGravity(true);
        level.addFreshEntity(bird);
        bird.getPersistentData().putInt("GuaniaoDroppingCooldown", 0);
        bird.tickCount = 400 + Math.floorMod(-bird.getId(), 20);
        try {
            EdDYON.guaniao.event.BirdDroppingEvents.tickBird(bird);
            h.assertTrue(level.getEntitiesOfClass(BirdDroppingProjectileEntity.class, new AABB(target).inflate(6)).stream()
                    .anyMatch(e -> e.isNaturalDropping() && !e.isLaxativeDropping() && bird.getUUID().equals(e.getSourceBirdUuid())), "Natural tick did not spawn a dropping");
            bird.setPos(bird.position().add(4, 0, 0));
            h.assertTrue(EdDYON.guaniao.event.BirdDroppingEvents.spawnLaxativeDropping(level, bird), "Laxative entry point failed");
            h.assertTrue(level.getEntitiesOfClass(BirdDroppingProjectileEntity.class, new AABB(target).inflate(6)).stream()
                    .anyMatch(BirdDroppingProjectileEntity::isLaxativeDropping), "Laxative marker missing");
        } finally {
            player.discard();
            channel.finishAndReleaseAll();
        }
        h.runAfterDelay(20, () -> {
            var splats = level.getEntitiesOfClass(BirdDroppingSplatEntity.class, new AABB(target).inflate(6));
            var projectiles = level.getEntitiesOfClass(BirdDroppingProjectileEntity.class, new AABB(target).inflate(6));
            h.assertTrue(splats.size() == 2,
                    "Natural and laxative drops must both produce ground splats: splats="
                            + splats.stream().map(s -> s.position().toString()).toList()
                            + ", projectiles=" + projectiles.stream().map(p -> p.position().toString()).toList());
            h.assertTrue(splats.stream().allMatch(s -> s.getY() > target.getY() + 1
                            && s.getY() < target.getY() + 1.1),
                    "Droppings must land on the fixture floor, not attach to an airborne entity");
            bird.discard();
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void actualLivingHitSlowsAndAttachesSplat(GameTestHelper h) {
        var level = h.getLevel();
        var target = h.absolutePos(new BlockPos(5, 2, 5));
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        sheep.setPos(Vec3.atBottomCenterOf(target));
        sheep.setNoAi(true);
        level.addFreshEntity(sheep);
        shootDown(h, target, BirdDroppingVariant.TWO);
        h.runAfterDelay(10, () -> {
            h.assertTrue(sheep.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Hit did not slow living target");
            h.assertTrue(level.getEntitiesOfClass(BirdDroppingSplatEntity.class, sheep.getBoundingBox().inflate(1)).size() == 1,
                    "Hit did not attach a splat");
            sheep.discard();
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ordinaryBooksAndFiveUpgradedDevNotesUseRealReadEvent(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ParityTest"), false);
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player, cookie);
        try {
            var ordinary = new ItemStack(Items.WRITTEN_BOOK);
            ordinary.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("ordinary"), "player", 0,
                    List.of(Filterable.passThrough(Component.literal("hello"))), true));
            player.setItemInHand(InteractionHand.MAIN_HAND, ordinary);
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
            h.assertTrue(!BirdAdvancements.isDone(player, BirdAdvancements.BIRD_NOTE), "Ordinary book counted as a bird note");
            for (String author : List.of("蛋炒饭", "伊洛哥斯拉", "多雨", "老三", "千年村庄")) {
                var legacy = new CompoundTag();
                legacy.putString("id", "minecraft:written_book");
                legacy.putByte("Count", (byte)1);
                var data = new CompoundTag();
                data.putByte("GuaniaoNote", (byte)1);
                data.putString("title", "legacy note");
                data.putString("author", author);
                var pages = new ListTag();
                pages.add(StringTag.valueOf("{\"text\":\"test\"}"));
                data.put("pages", pages);
                legacy.put("tag", data);
                var upgraded = ItemData.load(h.getLevel().registryAccess(), legacy);
                h.assertTrue(upgraded.get(DataComponents.WRITTEN_BOOK_CONTENT).author().equals(author), "Author did not migrate");
                player.setItemInHand(InteractionHand.MAIN_HAND, upgraded);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
            }
            h.assertTrue(BirdAdvancements.isDone(player, BirdAdvancements.BIRD_NOTE), "Read advancement missing");
            h.assertTrue(BirdAdvancements.isDone(player, BirdAdvancements.DEV_NOTES_ALL), "Five legacy dev notes did not award collection advancement");
        } finally {
            channel.finishAndReleaseAll();
        }
        h.succeed();
    }
}
