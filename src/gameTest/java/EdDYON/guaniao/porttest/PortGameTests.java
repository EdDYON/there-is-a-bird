package EdDYON.guaniao.porttest;

import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bath.*;
import EdDYON.guaniao.content.camera.*;
import EdDYON.guaniao.content.enchantment.GuaniaoEnchantments;
import EdDYON.guaniao.registry.GuaniaoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod("guaniao_port_tests")
@GameTestHolder("guaniao_port_tests")
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void allBirdsSpawnTickAndKeepSavedTraits(GameTestHelper helper) {
        var level = helper.getLevel();
        List<Mob> birds = new ArrayList<>();
        for (BirdSpecies species : BirdSpecies.values()) {
            Entity created = species.entityType().create(level);
            helper.assertTrue(created instanceof Mob, "Missing bird: " + species.id());
            Mob bird = (Mob)created;
            bird.moveTo(helper.absolutePos(new BlockPos(2 + birds.size() % 4 * 3, 2, 2 + birds.size() / 4 * 3)), 0, 0);
            bird.finalizeSpawn(level, level.getCurrentDifficultyAt(bird.blockPosition()), MobSpawnType.COMMAND, null);
            CompoundTag saved = new CompoundTag();
            bird.saveWithoutId(saved);
            Mob copy = (Mob)species.entityType().create(level);
            copy.load(saved);
            CompoundTag restored = new CompoundTag();
            copy.saveWithoutId(restored);
            for (String key : List.of("BirdModelScale", "BirdMutation", "PigeonVariant", "SkinVariant", "Owner", "TrustByPlayer")) {
                if (saved.contains(key)) helper.assertTrue(saved.get(key).equals(restored.get(key)), species.id() + " lost " + key);
            }
            level.addFreshEntity(bird);
            birds.add(bird);
        }
        helper.assertTrue(birds.size() == 16, "All sixteen species must be present");
        helper.runAfterDelay(80, () -> {
            for (Mob bird : birds) {
                helper.assertTrue(bird.tickCount >= 60, "Bird did not tick: " + bird.getType());
                helper.assertTrue(Double.isFinite(bird.getX()) && Double.isFinite(bird.getY()) && Double.isFinite(bird.getZ()), "Invalid bird position");
                bird.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void itemComponentsAndBlockEntitiesRoundTrip(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (var item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("guaniao")) continue;
            ItemStack stack = new ItemStack(item);
            ItemStack restored = ItemStack.parse(registries, stack.save(registries)).orElseThrow();
            helper.assertTrue(ItemStack.isSameItemSameComponents(stack, restored), "Item round trip failed: " + item);
        }
        CompoundTag legacyItem = new CompoundTag();
        legacyItem.putString("id", "guaniao:film");
        legacyItem.putByte("Count", (byte)7);
        legacyItem.putByte("Slot", (byte)5);
        CompoundTag legacyData = new CompoundTag();
        legacyData.putString("PhotoId", "0123456789abcdef0123456789abcdef");
        legacyData.putLong("GameTime", 42L);
        CompoundTag display = new CompoundTag();
        display.putString("Name", "{\"text\":\"legacy photo\"}");
        legacyData.put("display", display);
        legacyItem.put("tag", legacyData);
        ItemStack legacy = EdDYON.guaniao.util.ItemData.load(registries, legacyItem);
        helper.assertTrue(legacy.getCount() == 7, "Legacy nested item count lost");
        helper.assertTrue(legacy.getHoverName().getString().equals("legacy photo"), "Legacy custom name lost");
        helper.assertTrue(PhotographData.gameTime(legacy) == 42L, "Legacy mod data did not migrate to custom_data");
        helper.assertTrue(EdDYON.guaniao.util.ItemData.upgradeLegacyStack(legacyItem).getByte("Slot") == 5, "Legacy nest slot lost");
        helper.assertTrue(legacyItem.contains("Count") && legacyItem.getCompound("tag").contains("display"), "Legacy data fix mutated the source tag");
        ItemStack camera = new ItemStack(GuaniaoItems.NIKON_D750.get());
        CameraState state = CameraState.defaults().withFocalLength(85).withFocusDistance(12).withFilter(CameraFilter.FILM_GRAIN);
        CameraSettingsData.setState(camera, state);
        ItemStack cameraCopy = ItemStack.parse(registries, camera.save(registries)).orElseThrow();
        helper.assertTrue(CameraSettingsData.state(cameraCopy).equals(state), "Camera settings did not survive component serialization");
        CameraSettingsData.setFilter(cameraCopy, CameraFilter.BLACK_AND_WHITE);
        helper.assertTrue(CameraSettingsData.state(cameraCopy).focalLength() == 85, "Changing a filter discarded lens settings");
        helper.assertTrue(CameraSettingsData.filter(camera) == CameraFilter.FILM_GRAIN, "A copied item mutated the original");
        ItemStack photo = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
        UUID author = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        PhotographData.writeReference(photo, "0123456789abcdef0123456789abcdef", "test", author, 345L, 512, 512, "a".repeat(64));
        ItemStack photoCopy = ItemStack.parse(registries, photo.save(registries)).orElseThrow();
        helper.assertTrue(PhotographData.hasImage(photoCopy), "Photo reference lost");
        helper.assertTrue(author.equals(PhotographData.photographerId(photoCopy)), "Photo author lost");
        helper.assertTrue(PhotographData.gameTime(photoCopy) == 345L, "Photo capture time lost");
        helper.assertTrue(!PhotographData.hasLegacyPixels(photoCopy), "Referenced photos must not copy image pixels into items");
        for (var block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("guaniao") || !(block instanceof EntityBlock entityBlock)) continue;
            var blockState = block.defaultBlockState();
            BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, blockState);
            if (entity instanceof BirdBathBlockEntity bath) bath.setContent(BirdBathContentType.WATER, 3);
            CompoundTag saved = entity.saveWithFullMetadata(registries);
            BlockEntity restored = BlockEntity.loadStatic(BlockPos.ZERO, blockState, saved, registries);
            helper.assertTrue(restored != null && entity.getType() == restored.getType(), "Block entity round trip failed: " + block);
            if (restored instanceof BirdBathBlockEntity bath) helper.assertTrue(bath.hasUsableWater() && bath.getContentLevel() == 3, "Bath contents lost");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesAdvancementsTagsAndEnchantmentsLoaded(GameTestHelper helper) {
        var level = helper.getLevel();
        long recipes = level.getRecipeManager().getRecipes().stream().filter(recipe -> recipe.id().getNamespace().equals("guaniao")).count();
        helper.assertTrue(recipes == 20, "Expected 20 recipes, got " + recipes);
        long advancements = level.getServer().getAdvancements().getAllAdvancements().stream().filter(a -> a.id().getNamespace().equals("guaniao")).count();
        helper.assertTrue(advancements == 37, "Expected 37 advancements, got " + advancements);
        ItemStack fan = new ItemStack(GuaniaoItems.WIND_FEATHER_FAN.get());
        for (var key : List.of(GuaniaoEnchantments.BURIAL_PLUME, GuaniaoEnchantments.RIVEN_PLUME, GuaniaoEnchantments.HUNTING_RETURN)) {
            var holder = GuaniaoEnchantments.holder(level.registryAccess(), key);
            helper.assertTrue(holder.value().isSupportedItem(fan), "Fan enchantment lost supported item tag: " + key.location());
            helper.assertTrue(holder.is(net.minecraft.tags.EnchantmentTags.TREASURE), "Fan enchantment lost treasure status");
            helper.assertTrue(!holder.is(net.minecraft.tags.EnchantmentTags.TRADEABLE)
                    && !holder.is(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE)
                    && !holder.is(net.minecraft.tags.EnchantmentTags.ON_RANDOM_LOOT), "Fan enchantment became generally obtainable");
            for (var otherKey : List.of(GuaniaoEnchantments.BURIAL_PLUME, GuaniaoEnchantments.RIVEN_PLUME, GuaniaoEnchantments.HUNTING_RETURN)) {
                helper.assertTrue(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(holder,
                        GuaniaoEnchantments.holder(level.registryAccess(), otherKey)), "Fan enchantments must remain mutually exclusive");
            }
            fan.enchant(holder, 1);
            helper.assertTrue(GuaniaoEnchantments.level(fan, key) == 1, "Fan enchantment component lookup failed");
        }
        var record = new ItemStack(GuaniaoItems.MUSIC_DISC_UWU_FUNK.get());
        helper.assertTrue(record.has(DataComponents.JUKEBOX_PLAYABLE), "Music disc is not playable");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(
                net.minecraft.resources.ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath("guaniao", "uwu_funk"))).isPresent(), "Disc song missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wallPhotosKeepFacingRotationAndReference(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = helper.absolutePos(new BlockPos(4, 3, 4));
        for (var facing : net.minecraft.core.Direction.values()) {
            ItemStack photo = new ItemStack(GuaniaoItems.PHOTOGRAPH.get());
            PhotographData.writeReference(photo, "0123456789abcdef0123456789abcdef", "test",
                    UUID.fromString("12345678-1234-1234-1234-123456789abc"), 123L, 512, 512, "a".repeat(64));
            var entity = new PhotographEntity(level, position, facing, photo);
            entity.setRotation(3);
            var bounds = entity.getBoundingBox();
            helper.assertTrue(bounds.getXsize() == (facing.getAxis() == net.minecraft.core.Direction.Axis.X ? 0.0625D : 0.75D)
                    && bounds.getYsize() == (facing.getAxis() == net.minecraft.core.Direction.Axis.Y ? 0.0625D : 0.75D)
                    && bounds.getZsize() == (facing.getAxis() == net.minecraft.core.Direction.Axis.Z ? 0.0625D : 0.75D), "Photo bounds changed: " + facing);
            CompoundTag saved = new CompoundTag();
            entity.saveWithoutId(saved);
            var restored = EdDYON.guaniao.registry.GuaniaoEntityTypes.PHOTOGRAPH.get().create(level);
            restored.load(saved);
            helper.assertTrue(restored.getDirection() == facing && restored.getRotation() == 3, "Photo orientation lost: " + facing);
            helper.assertTrue(restored.getBoundingBox().equals(bounds), "Photo bounds lost after reload: " + facing);
            helper.assertTrue(ItemStack.isSameItemSameComponents(photo, restored.getItem()), "Photo reference lost after reload");
        }
        helper.succeed();
    }
}
