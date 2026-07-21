package EdDYON.guaniao.command;

import EdDYON.guaniao.GuaniaoMod;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoRecord;
import EdDYON.guaniao.content.camera.PhotoIndexSavedData.PhotoStatus;
import EdDYON.guaniao.content.camera.PhotoIoService;
import EdDYON.guaniao.content.camera.PhotoMaintenance;
import EdDYON.guaniao.content.camera.PhotoRepository;
import EdDYON.guaniao.network.PhotoUploadManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GuaniaoMod.MOD_ID)
public final class PhotoAdminCommands {
    private PhotoAdminCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guaniao")
                .requires(BirdConfigCommands::canEdit)
                .then(Commands.literal("photo")
                        .then(Commands.literal("stats")
                                .executes(context -> stats(context.getSource(), null))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(context -> stats(
                                                context.getSource(),
                                                GameProfileArgument.getGameProfiles(context, "player").stream().findFirst().orElse(null)))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(context -> list(
                                                context.getSource(),
                                                GameProfileArgument.getGameProfiles(context, "player").stream().findFirst().orElse(null)))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("photo_id", StringArgumentType.word())
                                        .executes(context -> moveToTrash(context.getSource(), StringArgumentType.getString(context, "photo_id")))))
                        .then(Commands.literal("restore")
                                .then(Commands.argument("photo_id", StringArgumentType.word())
                                        .executes(context -> restore(context.getSource(), StringArgumentType.getString(context, "photo_id")))))
                        .then(Commands.literal("verify")
                                .executes(context -> maintenance(context.getSource(), true)))
                        .then(Commands.literal("prune")
                                .then(Commands.literal("dry_run")
                                        .executes(context -> maintenance(context.getSource(), true)))
                                .then(Commands.literal("confirm")
                                        .executes(context -> maintenance(context.getSource(), false))))));
    }

    private static int stats(CommandSourceStack source, GameProfile profile) {
        UUID owner = profile == null ? null : profile.getId();
        PhotoIndexSavedData.Usage usage = PhotoIndexSavedData.get(source.getServer()).usage(owner);
        source.sendSuccess(() -> Component.translatable(
                "command.guaniao.photo.stats",
                usage.worldCount(), formatBytes(usage.worldBytes()), usage.activeCount(),
                usage.trashCount(), usage.missingCount(),
                PhotoUploadManager.activeUploads(), PhotoUploadManager.activeDownloads(), PhotoIoService.queuedTasks()
        ), false);
        if (profile != null) {
            source.sendSuccess(() -> Component.translatable(
                    "command.guaniao.photo.player_stats",
                    profile.getName(), usage.playerCount(), formatBytes(usage.playerBytes())
            ), false);
        }
        return usage.worldCount();
    }

    private static int list(CommandSourceStack source, GameProfile profile) {
        if (profile == null) {
            return 0;
        }
        List<PhotoRecord> records = PhotoIndexSavedData.get(source.getServer()).ownedBy(profile.getId()).stream()
                .sorted(Comparator.comparingLong(PhotoRecord::createdAt).reversed())
                .limit(10)
                .toList();
        source.sendSuccess(() -> Component.translatable("command.guaniao.photo.list_header", profile.getName(), records.size()), false);
        for (PhotoRecord record : records) {
            source.sendSuccess(() -> Component.literal(record.id() + "  " + record.status().name().toLowerCase()
                    + "  " + formatBytes(record.bytes())), false);
        }
        return records.size();
    }

    private static int moveToTrash(CommandSourceStack source, String photoId) {
        if (PhotoMaintenance.isRunning()) {
            source.sendFailure(Component.translatable("command.guaniao.photo.busy"));
            return 0;
        }
        MinecraftServer server = source.getServer();
        PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
        PhotoRecord record = index.get(photoId);
        if (record == null || record.status() != PhotoStatus.ACTIVE) {
            source.sendFailure(Component.translatable("command.guaniao.photo.not_found", photoId));
            return 0;
        }
        long now = System.currentTimeMillis();
        index.moveToTrash(photoId, now);
        boolean accepted = PhotoIoService.submit(
                server,
                () -> {
                    PhotoRepository.moveToTrash(server, photoId);
                    return photoId;
                },
                ignored -> source.sendSuccess(() -> Component.translatable("command.guaniao.photo.deleted", photoId), true),
                throwable -> {
                    index.restore(photoId);
                    source.sendFailure(Component.translatable("command.guaniao.photo.io_failed", photoId));
                }
        );
        if (!accepted) {
            index.restore(photoId);
            source.sendFailure(Component.translatable("command.guaniao.photo.busy"));
            return 0;
        }
        return 1;
    }

    private static int restore(CommandSourceStack source, String photoId) {
        if (PhotoMaintenance.isRunning()) {
            source.sendFailure(Component.translatable("command.guaniao.photo.busy"));
            return 0;
        }
        MinecraftServer server = source.getServer();
        PhotoIndexSavedData index = PhotoIndexSavedData.get(server);
        PhotoRecord record = index.get(photoId);
        if (record == null || record.status() != PhotoStatus.TRASH) {
            source.sendFailure(Component.translatable("command.guaniao.photo.not_in_trash", photoId));
            return 0;
        }
        index.restore(photoId);
        boolean accepted = PhotoIoService.submit(
                server,
                () -> {
                    PhotoRepository.restoreFromTrash(server, photoId);
                    return photoId;
                },
                ignored -> source.sendSuccess(() -> Component.translatable("command.guaniao.photo.restored", photoId), true),
                throwable -> {
                    index.moveToTrash(photoId, System.currentTimeMillis());
                    source.sendFailure(Component.translatable("command.guaniao.photo.io_failed", photoId));
                }
        );
        if (!accepted) {
            index.moveToTrash(photoId, System.currentTimeMillis());
            source.sendFailure(Component.translatable("command.guaniao.photo.busy"));
            return 0;
        }
        return 1;
    }

    private static int maintenance(CommandSourceStack source, boolean dryRun) {
        boolean accepted = PhotoMaintenance.schedule(source.getServer(), dryRun, result -> {
            if (!result.success()) {
                source.sendFailure(Component.translatable("command.guaniao.photo.maintenance_failed", result.error()));
                return;
            }
            source.sendSuccess(() -> Component.translatable(
                    dryRun ? "command.guaniao.photo.verify_result" : "command.guaniao.photo.prune_result",
                    result.storedFiles(), result.missing().size(), result.orphans().size(), result.deletedTrash().size()
            ), true);
        });
        if (!accepted) {
            source.sendFailure(Component.translatable("command.guaniao.photo.busy"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.guaniao.photo.maintenance_started"), false);
        return 1;
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(java.util.Locale.ROOT, "%.2f MiB", bytes / (1024.0D * 1024.0D));
        }
        return String.format(java.util.Locale.ROOT, "%.1f KiB", bytes / 1024.0D);
    }
}
