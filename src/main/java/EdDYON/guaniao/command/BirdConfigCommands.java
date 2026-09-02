package EdDYON.guaniao.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.config.BirdConfigData;
import EdDYON.guaniao.config.BirdConfigManager;
import EdDYON.guaniao.config.BirdSpecies;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.content.bird.mutation.BirdMutationHolder;
import EdDYON.guaniao.content.bird.scale.ScalableBirdModel;
import EdDYON.guaniao.content.note.BirdNoteContent;
import EdDYON.guaniao.network.GuaniaoNetwork;
import EdDYON.guaniao.network.OpenBirdConfigPacket;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class BirdConfigCommands {
    private BirdConfigCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniaoconfig")
                .requires(BirdConfigCommands::canEdit)
                .executes(context -> open(context.getSource())));
        event.getDispatcher().register(Commands.literal("birdconfig")
                .requires(BirdConfigCommands::canEdit)
                .executes(context -> open(context.getSource())));
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("seagullRaid")
                        .executes(context -> seagullRaid(context.getSource())))
                .then(Commands.literal("spawnMutation")
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests(BirdConfigCommands::suggestSpecies)
                                .then(Commands.argument("mutation", StringArgumentType.word())
                                        .suggests(BirdConfigCommands::suggestMutation)
                                        .executes(context -> spawnMutation(context.getSource(),
                                                StringArgumentType.getString(context, "species"),
                                                StringArgumentType.getString(context, "mutation"))))))
                .then(Commands.literal("spawnAllMutations")
                        .executes(context -> spawnAllMutations(context.getSource(), null))
                        .then(Commands.argument("species", StringArgumentType.word())
                                .suggests(BirdConfigCommands::suggestSpecies)
                                .executes(context -> spawnAllMutations(context.getSource(),
                                        StringArgumentType.getString(context, "species")))))
                .then(Commands.literal("birdNotes")
                        .executes(context -> birdNotes(context.getSource())))
                .then(Commands.literal("birdSizeComparison")
                        .executes(context -> birdSizeComparison(context.getSource()))));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BirdConfigManager.loadForServer(source.getServer());
        GuaniaoNetwork.sendToPlayer(new OpenBirdConfigPacket(BirdConfigManager.snapshot()), player);
        return 1;
    }

    private static int seagullRaid(CommandSourceStack source) {
        BirdConfigData config = BirdConfigManager.snapshot();
        config.global.seagullPlayerCooldownTicks = 0;
        config.global.maxConcurrentSeagullTargetsPerPlayer = 8;
        config.global.aprilFoolsMode = true;
        if (BirdConfigManager.replaceAndSave(config, source.getServer())) {
            source.sendSuccess(() -> Component.translatable("Seagull raid mode activated! Seagulls are now extremely aggressive."), true);
            return 1;
        }
        source.sendFailure(Component.literal("Failed to apply seagull raid config."));
        return 0;
    }

    private static final String[] SPECIES_CHOICES = {
            "nightheron", "sparrow", "longtailedtit", "cockatiel", "macaw",
            "budgerigar", "spotteddove", "pigeon", "crow", "seagull"
    };
    private static final String[] MUTATION_CHOICES = {"leucistic", "melanistic", "golden", "puregold", "rainbow", "random"};
    private static final BirdMutation[] ALL_MUTATIONS = {
            BirdMutation.LEUCISTIC, BirdMutation.MELANISTIC, BirdMutation.GOLDEN, BirdMutation.GOLDEN_PURE, BirdMutation.RAINBOW
    };
    private static final String SIZE_COMPARISON_TAG = "guaniao.bird_size_comparison";
    private static final BirdSpecies[] SIZE_COMPARISON_ORDER = {
            BirdSpecies.BUDGERIGAR, BirdSpecies.LONG_TAILED_TIT, BirdSpecies.SPARROW, BirdSpecies.COCKATIEL,
            BirdSpecies.KIWI, BirdSpecies.SPOTTED_DOVE, BirdSpecies.PIGEON, BirdSpecies.MACAW,
            BirdSpecies.MYNA, BirdSpecies.CROW, BirdSpecies.NIGHT_HERON, BirdSpecies.SEAGULL
    };

    // Lazily built: this class is a @Mod.EventBusSubscriber, so Forge loads it during mod
    // construction, before DeferredRegister entries exist. Calling RegistryObject.get() in a
    // static initializer would NPE ("Registry Object not present"); the map is built on first
    // command use instead, when registries are fully populated.
    private static Map<String, EntityType<?>> speciesMap;

    private static Map<String, EntityType<?>> species() {
        Map<String, EntityType<?>> map = speciesMap;
        if (map == null) {
            map = new HashMap<>();
            map.put("nightheron", GuaniaoEntityTypes.NIGHT_HERON.get());
            map.put("sparrow", GuaniaoEntityTypes.SPARROW.get());
            map.put("longtailedtit", GuaniaoEntityTypes.LONG_TAILED_TIT.get());
            map.put("cockatiel", GuaniaoEntityTypes.COCKATIEL.get());
            map.put("macaw", GuaniaoEntityTypes.MACAW.get());
            map.put("budgerigar", GuaniaoEntityTypes.BUDGERIGAR.get());
            map.put("spotteddove", GuaniaoEntityTypes.SPOTTED_DOVE.get());
            map.put("pigeon", GuaniaoEntityTypes.PIGEON.get());
            map.put("crow", GuaniaoEntityTypes.CROW.get());
            map.put("seagull", GuaniaoEntityTypes.SEAGULL.get());
            speciesMap = map;
        }
        return map;
    }

    private static CompletableFuture<Suggestions> suggestSpecies(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (String choice : SPECIES_CHOICES) {
            builder.suggest(choice);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMutation(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (String choice : MUTATION_CHOICES) {
            builder.suggest(choice);
        }
        return builder.buildFuture();
    }

    private static EntityType<?> resolveSpecies(String name) {
        return species().get(name.toLowerCase(Locale.ROOT));
    }

    private static BirdMutation resolveMutation(String name, RandomSource random) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "leucistic", "albino", "white" -> BirdMutation.LEUCISTIC;
            case "melanistic", "black", "dark" -> BirdMutation.MELANISTIC;
            // "golden" matches the natural spread: most gold birds are matte gold, a rarer few
            // are pure gold.
            case "golden", "gold" -> random.nextInt(100) < 70 ? BirdMutation.GOLDEN : BirdMutation.GOLDEN_PURE;
            case "puregold", "pure" -> BirdMutation.GOLDEN_PURE;
            case "rainbow", "iridescent", "shimmer" -> BirdMutation.RAINBOW;
            case "random", "any" -> ALL_MUTATIONS[random.nextInt(ALL_MUTATIONS.length)];
            default -> null;
        };
    }

    private static int spawnMutation(CommandSourceStack source, String speciesName, String mutationName) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        EntityType<?> type = resolveSpecies(speciesName);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown bird species: " + speciesName));
            return 0;
        }
        BirdMutation mutation = resolveMutation(mutationName, player.getRandom());
        if (mutation == null) {
            source.sendFailure(Component.literal("Unknown mutation: " + mutationName
                    + " (leucistic / melanistic / golden / puregold / rainbow / random)"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        if (!spawnMutated(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), type, mutation)) {
            source.sendFailure(Component.literal("That entity type does not support mutations."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned a " + mutationName.toLowerCase(Locale.ROOT) + " " + speciesName + "."), true);
        return 1;
    }

    private static int spawnAllMutations(CommandSourceStack source, String speciesName) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        String[] speciesList;
        if (speciesName == null) {
            speciesList = SPECIES_CHOICES;
        } else {
            String key = speciesName.toLowerCase(Locale.ROOT);
            if (!species().containsKey(key)) {
                source.sendFailure(Component.literal("Unknown bird species: " + speciesName));
                return 0;
            }
            speciesList = new String[]{key};
        }
        ServerLevel level = player.serverLevel();
        int spawned = 0;
        for (int s = 0; s < speciesList.length; s++) {
            EntityType<?> type = species().get(speciesList[s]);
            for (int m = 0; m < ALL_MUTATIONS.length; m++) {
                double x = player.getX() + (s - (speciesList.length - 1) / 2.0) * 2.5;
                double z = player.getZ() + (m - (ALL_MUTATIONS.length - 1) / 2.0) * 2.5;
                if (spawnMutated(level, x, player.getY(), z, 0.0F, type, ALL_MUTATIONS[m])) {
                    spawned++;
                }
            }
        }
        int count = spawned;
        source.sendSuccess(() -> Component.literal("Spawned " + count + " mutated birds."), true);
        return 1;
    }

    private static boolean spawnMutated(ServerLevel level, double x, double y, double z, float yRot,
                                        EntityType<?> type, BirdMutation mutation) {
        Entity entity = type.create(level);
        if (!(entity instanceof BirdMutationHolder holder) || !(entity instanceof Mob mob)) {
            return false;
        }
        mob.moveTo(x, y, z, yRot, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null, null);
        holder.setBirdMutation(mutation);
        // Freeze the summoned bird so it stays perfectly still for inspection: no AI goals run,
        // and no gravity so it never sinks or falls even if the ground is uneven.
        mob.setNoAi(true);
        mob.setNoGravity(true);
        level.addFreshEntity(mob);
        return true;
    }

    private static int birdSizeComparison(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 base = player.position().add(forward.scale(7.0D));

        int spawned = 0;
        for (int index = 0; index < SIZE_COMPARISON_ORDER.length; index++) {
            BirdSpecies species = SIZE_COMPARISON_ORDER[index];
            int column = index % 4;
            int row = index / 4;
            Vec3 groupCenter = base
                    .add(right.scale((column - 1.5D) * 4.0D))
                    .add(forward.scale(row * 3.25D));

            if (spawnSizeComparisonBird(level, player, species, groupCenter.subtract(right.scale(0.8D)), true)) {
                spawned++;
            }
            if (spawnSizeComparisonBird(level, player, species, groupCenter.add(right.scale(0.8D)), false)) {
                spawned++;
            }
        }
        return spawned;
    }

    private static boolean spawnSizeComparisonBird(ServerLevel level, ServerPlayer player, BirdSpecies species,
                                                     Vec3 position, boolean minimum) {
        EntityType<?> type = species.entityType();
        if (type == null) {
            return false;
        }
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob) || !(entity instanceof ScalableBirdModel scalable)) {
            return false;
        }

        Vec3 toPlayer = player.position().subtract(position);
        float yaw = (float) Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90.0F;
        mob.moveTo(position.x, player.getY(), position.z, yaw, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null, null);

        float comparisonScale = minimum
                ? scalable.modelScaleProfile().minIndividualScale()
                : scalable.modelScaleProfile().maxIndividualScale();
        scalable.setIndividualModelScale(comparisonScale);

        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setSilent(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.addTag(SIZE_COMPARISON_TAG);
        mob.setCustomName(Component.translatable(species.translationKey())
                .append(Component.literal(minimum ? " · MIN" : " · MAX")));
        mob.setCustomNameVisible(true);
        level.addFreshEntity(mob);
        return true;
    }

    private static int birdNotes(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        List<ItemStack> notes = BirdNoteContent.allNotes();
        // A single chest holds 27 slots, so the full collection needs a row of chests.
        int chestCount = (notes.size() + 26) / 27;
        BlockPos base = findChestSpot(level, player);
        if (base == null) {
            source.sendFailure(Component.literal("No open space in front of you to place the chests."));
            return 0;
        }
        Direction facing = player.getDirection().getOpposite();
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        right = right.lengthSqr() < 1.0E-8 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        int stepX = (int) Math.round(right.x);
        int stepZ = (int) Math.round(right.z);
        for (int c = 0; c < chestCount; c++) {
            BlockPos pos = base.offset(stepX * c, 0, stepZ * c);
            if (!canPlaceChest(level, pos)) {
                source.sendFailure(Component.literal("Not enough open space beside you for all " + chestCount + " chests."));
                return 0;
            }
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                int from = c * 27;
                int to = Math.min(from + 27, notes.size());
                for (int i = from; i < to; i++) {
                    chest.setItem(i - from, notes.get(i));
                }
                chest.setChanged();
            }
        }
        int count = chestCount;
        int noteCount = notes.size();
        source.sendSuccess(() -> Component.literal("Spawned " + count + " chests holding all " + noteCount
                + " bird notes at " + base.toShortString() + "."), true);
        return 1;
    }

    private static boolean canPlaceChest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.canBeReplaced();
    }

    /** Finds a reachable spot in front of the player where a chest can be placed. */
    private static BlockPos findChestSpot(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        for (int dist = 2; dist <= 6; dist++) {
            BlockPos pos = BlockPos.containing(eye.x + look.x * dist, eye.y + look.y * dist, eye.z + look.z * dist);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced()) {
                continue;
            }
            BlockState above = level.getBlockState(pos.above());
            if (above.isAir() || above.canBeReplaced()) {
                return pos;
            }
        }
        return null;
    }

    public static boolean canEdit(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        return source.getEntity() instanceof ServerPlayer player
                && source.getServer().isSingleplayerOwner(player.getGameProfile());
    }
}
