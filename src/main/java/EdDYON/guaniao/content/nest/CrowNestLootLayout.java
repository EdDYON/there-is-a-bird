package EdDYON.guaniao.content.nest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Packs the nest's six hidden treasure stacks into a compact rummage grid. */
public final class CrowNestLootLayout {
    public static final int GRID_COLUMNS = 6;
    public static final int GRID_ROWS = 4;

    private CrowNestLootLayout() {
    }

    public static List<Placement> arrange(List<ItemStack> stacks) {
        boolean[][] occupied = new boolean[GRID_ROWS][GRID_COLUMNS];
        List<Placement> placements = new ArrayList<>();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CrowNestLootProfile profile = CrowNestLootProfile.forStack(stack);
            Placement placement = findPlacement(slot, profile, occupied);
            if (placement != null) {
                markOccupied(placement, occupied);
                placements.add(placement);
            }
        }
        return placements;
    }

    private static Placement findPlacement(int slot, CrowNestLootProfile profile, boolean[][] occupied) {
        for (int row = 0; row <= GRID_ROWS - profile.height(); row++) {
            for (int column = 0; column <= GRID_COLUMNS - profile.width(); column++) {
                if (fits(column, row, profile, occupied)) {
                    return new Placement(slot, column, row, profile);
                }
            }
        }
        return null;
    }

    private static boolean fits(int column, int row, CrowNestLootProfile profile, boolean[][] occupied) {
        for (int y = row; y < row + profile.height(); y++) {
            for (int x = column; x < column + profile.width(); x++) {
                if (occupied[y][x]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void markOccupied(Placement placement, boolean[][] occupied) {
        for (int y = placement.row(); y < placement.row() + placement.profile().height(); y++) {
            for (int x = placement.column(); x < placement.column() + placement.profile().width(); x++) {
                occupied[y][x] = true;
            }
        }
    }

    public record Placement(int storageSlot, int column, int row, CrowNestLootProfile profile) {
    }
}
