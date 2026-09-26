package EdDYON.guaniao.porttest;

import EdDYON.guaniao.command.BirdStressTestManager;
import EdDYON.guaniao.content.bird.mutation.BirdMutation;
import EdDYON.guaniao.event.BirdMutationEvents;
import EdDYON.guaniao.registry.GuaniaoEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Regressions for the remaining differences from the Forge 3.5.0 source. */
@GameTestHolder("guaniao_port_tests")
@PrefixGameTestTemplate(false)
public final class PortAlignmentGameTests {
    @GameTest(template = "empty", batch = "port_alignment", timeoutTicks = 100)
    public static void stressReportMeasuresActualServerTicks(GameTestHelper h) {
        var level = h.getLevel();
        // No synthetic birds are needed to check the real Pre/Post event chain.
        h.assertTrue(BirdStressTestManager.start(level,
                Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 3, 2))), null, 0, 10, 8),
                "Stress session unexpectedly active");
        h.runAfterDelay(25, () -> {
            var report = BirdStressTestManager.stop(level.getServer(), true);
            h.assertTrue(report != null && report.samples() >= 20, "Missing server tick samples");
            h.assertTrue(Double.isFinite(report.averageMs()) && report.averageMs() >= 0
                    && report.averageMs() < 5000, "Tick timing used an unset or stale start timestamp");
            h.assertTrue(report.p95Ms() >= 0 && report.p95Ms() <= report.maxMs()
                    && report.maxMs() < 5000, "Invalid tick percentiles");
            h.assertTrue(report.estimatedTps() > 0 && report.estimatedTps() <= 20, "Invalid estimated TPS");
            System.out.println("ALIGN|stress|samples=" + report.samples()
                    + "|averageMs=" + report.averageMs() + "|tps=" + report.estimatedTps());
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "port_alignment")
    public static void goldenBirdDropsUseVanillaAndNeoForgeLooting(GameTestHelper h) {
        var level = h.getLevel();
        var killer = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = killer.getMainHandItem();
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        var looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        var queries = new AtomicInteger();
        Consumer<GetEnchantmentLevelEvent> modifier = event -> {
            if (event.getStack() == sword && event.isTargetting(Enchantments.LOOTING)) {
                queries.incrementAndGet();
                event.getEnchantments().set(looting, 2);
            }
        };
        boolean listening = false;
        try {
            killer.setItemSlot(EquipmentSlot.MAINHAND, sword);
            checkGoldDrop(h, killer, BirdMutation.NONE, 0);
            checkGoldDrop(h, killer, BirdMutation.GOLDEN, 1);
            checkGoldDrop(h, killer, BirdMutation.GOLDEN_PURE, 3);
            sword.enchant(looting, 2); // Original probability is 0.5 * level, hence certain at level two.
            checkGoldDrop(h, killer, BirdMutation.GOLDEN, 2);
            checkGoldDrop(h, killer, BirdMutation.GOLDEN_PURE, 4);
            sword.remove(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
            NeoForge.EVENT_BUS.addListener(modifier);
            listening = true;
            checkGoldDrop(h, killer, BirdMutation.GOLDEN, 2);
            checkGoldDrop(h, killer, BirdMutation.GOLDEN_PURE, 4);
            h.assertTrue(queries.get() >= 2, "NeoForge enchantment-level overrides were bypassed");
            System.out.println("ALIGN|golden_looting|base=1,3|looting_II=2,4|event_override=2,4|queries=" + queries.get());
        } finally {
            if (listening) NeoForge.EVENT_BUS.unregister(modifier);
            killer.setItemSlot(EquipmentSlot.MAINHAND, previous);
        }
        h.succeed();
    }

    private static void checkGoldDrop(GameTestHelper h, net.minecraft.server.level.ServerPlayer killer,
                                      BirdMutation mutation, int expected) {
        var level = h.getLevel();
        var bird = GuaniaoEntityTypes.NIGHT_HERON.get().create(level);
        h.assertTrue(bird != null, "Missing night heron");
        bird.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 3, 4))));
        bird.setBirdMutation(mutation);
        var captured = new ArrayList<ItemEntity>();
        bird.captureDrops(captured);
        try {
            BirdMutationEvents.onLivingDrops(new LivingDropsEvent(bird,
                    level.damageSources().playerAttack(killer), new ArrayList<>(), true));
            int count = captured.stream().filter(item -> item.getItem().is(Items.GOLD_INGOT))
                    .mapToInt(item -> item.getItem().getCount()).sum();
            h.assertTrue(count == expected, mutation + " gold count: expected " + expected + ", got " + count);
        } finally {
            bird.captureDrops(null);
            bird.discard();
        }
    }
}
