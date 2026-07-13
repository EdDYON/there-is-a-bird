package EdDYON.guaniao.content.nest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.world.item.ItemStack;

/**
 * Randomly packs independently rolled 1x1, 2x2, and 3x3 footprints into the
 * complete vanilla double-chest grid. The saved layout seed keeps the result stable.
 */
public final class CrowNestLootLayout {
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_ROWS = 6;

    private CrowNestLootLayout() {
    }

    public static List<Placement> arrange(List<ItemStack> stacks, long layoutSeed, int[] footprints) {
        List<Request> requests = new ArrayList<>();
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (!stacks.get(slot).isEmpty()) {
                int footprint = slot < footprints.length ? footprints[slot] : 1;
                requests.add(new Request(slot, Math.max(1, Math.min(3, footprint))));
            }
        }

        Random random = new Random(layoutSeed);
        Collections.shuffle(requests, random);
        requests.sort(Comparator.comparingInt(Request::area).reversed());
        boolean[][] occupied = new boolean[GRID_ROWS][GRID_COLUMNS];
        List<Placement> placements = new ArrayList<>(requests.size());
        if (!placeRequests(0, requests, occupied, placements, random)) {
            throw new IllegalStateException("Bird nest loot footprints could not be packed into the double-chest grid");
        }
        placements.sort(Comparator.comparingInt(Placement::storageSlot));
        return placements;
    }

    /** Recreates a previously packed layout without moving any remaining find. */
    public static List<Placement> fromStored(List<ItemStack> stacks, int[] footprints, int[] columns, int[] rows) {
        List<Placement> placements = new ArrayList<>();
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (stacks.get(slot).isEmpty()) {
                continue;
            }
            int footprint = slot < footprints.length ? footprints[slot] : 1;
            int column = slot < columns.length ? columns[slot] : -1;
            int row = slot < rows.length ? rows[slot] : -1;
            if (column < 0 || row < 0) {
                continue;
            }
            placements.add(new Placement(slot, column, row, Math.max(1, Math.min(3, footprint)),
                    Math.max(1, Math.min(3, footprint))));
        }
        return placements;
    }

    private static boolean placeRequests(int requestIndex, List<Request> requests, boolean[][] occupied,
                                         List<Placement> placements, Random random) {
        if (requestIndex >= requests.size()) {
            return true;
        }
        Request request = requests.get(requestIndex);
        List<Placement> candidates = new ArrayList<>();
        for (int row = 0; row <= GRID_ROWS - request.size(); row++) {
            for (int column = 0; column <= GRID_COLUMNS - request.size(); column++) {
                if (fits(column, row, request.size(), occupied)) {
                    candidates.add(new Placement(request.storageSlot(), column, row, request.size(), request.size()));
                }
            }
        }
        Collections.shuffle(candidates, random);
        for (Placement candidate : candidates) {
            markOccupied(candidate, occupied, true);
            placements.add(candidate);
            if (placeRequests(requestIndex + 1, requests, occupied, placements, random)) {
                return true;
            }
            placements.remove(placements.size() - 1);
            markOccupied(candidate, occupied, false);
        }
        return false;
    }

    private static boolean fits(int column, int row, int size, boolean[][] occupied) {
        for (int y = row; y < row + size; y++) {
            for (int x = column; x < column + size; x++) {
                if (occupied[y][x]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void markOccupied(Placement placement, boolean[][] occupied, boolean occupiedValue) {
        for (int y = placement.row(); y < placement.row() + placement.height(); y++) {
            for (int x = placement.column(); x < placement.column() + placement.width(); x++) {
                occupied[y][x] = occupiedValue;
            }
        }
    }

    public record Placement(int storageSlot, int column, int row, int width, int height) {
    }

    private record Request(int storageSlot, int size) {
        private int area() {
            return this.size * this.size;
        }
    }
}
