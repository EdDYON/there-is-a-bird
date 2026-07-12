package EdDYON.guaniao.client.bath;

import EdDYON.guaniao.content.bath.BirdBathContentType;
import EdDYON.guaniao.content.bath.BirdBathCleanliness;
import java.util.Set;
import software.bernie.geckolib.cache.object.GeoBone;

final class BirdBathBoneVisibility {
    private static final Set<String> CONTENT_BONES = Set.of(
            "water_up", "water_middle", "water_down",
            "ice_up", "ice_middle", "ice_down",
            "fish_up", "fish_middle", "fish_down",
            "meat_up", "meat_middle", "meat_down",
            "bread_up", "bread_middle", "bread_down",
            "spoiled_up", "spoiled_middle", "spoiled_down"
    );
    private static final Set<String> DIRT_BONES = Set.of(
            "dirty_spots_light",
            "dirty_spots_medium",
            "dirty_spots_heavy",
            "spoil_spots",
            "flies"
    );

    private BirdBathBoneVisibility() {
    }

    static void apply(BirdBathContentType type, int level, BirdBathCleanliness cleanliness, BirdBathContentType spoiledContentType, GeoBone bone) {
        String boneName = bone.getName();
        if (CONTENT_BONES.contains(boneName)) {
            boolean visible = isContentBoneVisible(type, level, spoiledContentType, boneName);
            bone.setHidden(!visible);
            bone.setChildrenHidden(!visible);
            return;
        }
        if (!DIRT_BONES.contains(boneName)) {
            return;
        }
        boolean visible = isDirtBoneVisible(type, level, cleanliness, boneName);
        bone.setHidden(!visible);
        bone.setChildrenHidden(!visible);
    }

    static boolean isContentBoneVisible(BirdBathContentType type, int level, BirdBathContentType spoiledContentType, String boneName) {
        if (type == null || type.isEmpty() || level <= 0) {
            return false;
        }
        String levelName = switch (level) {
            case 1 -> "down";
            case 2 -> "middle";
            case 3 -> "up";
            default -> "";
        };
        if (levelName.isEmpty()) {
            return false;
        }
        if (type == BirdBathContentType.SPOILED) {
            BirdBathContentType visualType = spoiledContentType != null && spoiledContentType.isFood() ? spoiledContentType : BirdBathContentType.FISH;
            return boneName.equals(visualType.serializedName() + "_" + levelName) || boneName.equals("spoiled_" + levelName);
        }
        return switch (type) {
            case WATER -> boneName.equals("water_" + levelName);
            case FROZEN_WATER -> boneName.equals("water_" + levelName) || boneName.equals("ice_" + levelName);
            case FISH -> boneName.equals("fish_" + levelName);
            case MEAT -> boneName.equals("meat_" + levelName);
            case BREAD -> boneName.equals("bread_" + levelName);
            case EMPTY, SPOILED -> false;
        };
    }

    static float[] tintFor(BirdBathContentType type, BirdBathContentType visualType, BirdBathCleanliness cleanliness) {
        if (type == BirdBathContentType.SPOILED) {
            return switch (visualType == null ? BirdBathContentType.FISH : visualType) {
                case FISH -> new float[]{0.46F, 0.56F, 0.40F};
                case MEAT -> new float[]{0.48F, 0.34F, 0.25F};
                case BREAD -> new float[]{0.50F, 0.44F, 0.25F};
                default -> new float[]{0.46F, 0.50F, 0.34F};
            };
        }
        if (type == BirdBathContentType.WATER) {
            BirdBathCleanliness normalized = cleanliness == null ? BirdBathCleanliness.CLEAN : cleanliness;
            return switch (normalized) {
                case CLEAN -> new float[]{1.0F, 1.0F, 1.0F};
                case USED -> new float[]{0.75F, 0.82F, 0.78F};
                case DIRTY -> new float[]{0.56F, 0.54F, 0.43F};
                case FILTHY -> new float[]{0.38F, 0.48F, 0.28F};
            };
        }
        if (type == BirdBathContentType.FROZEN_WATER) {
            return new float[]{0.74F, 0.88F, 1.0F};
        }
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    static boolean isDirtBone(String boneName) {
        return DIRT_BONES.contains(boneName);
    }

    static float[] dirtTintFor(BirdBathContentType type, BirdBathCleanliness cleanliness, String boneName) {
        if (type == BirdBathContentType.SPOILED) {
            if (boneName.equals("flies")) {
                return new float[]{0.08F, 0.08F, 0.07F};
            }
            if (boneName.equals("spoil_spots")) {
                return new float[]{0.36F, 0.48F, 0.20F};
            }
        }
        BirdBathCleanliness normalized = cleanliness == null ? BirdBathCleanliness.CLEAN : cleanliness;
        return switch (normalized) {
            case CLEAN -> new float[]{1.0F, 1.0F, 1.0F};
            case USED -> new float[]{0.62F, 0.58F, 0.48F};
            case DIRTY -> new float[]{0.42F, 0.46F, 0.28F};
            case FILTHY -> new float[]{0.24F, 0.32F, 0.16F};
        };
    }

    private static boolean isDirtBoneVisible(BirdBathContentType type, int level, BirdBathCleanliness cleanliness, String boneName) {
        if (level < 3) {
            return false;
        }
        BirdBathCleanliness normalizedCleanliness = cleanliness == null ? BirdBathCleanliness.CLEAN : cleanliness;
        if (type == BirdBathContentType.SPOILED && (boneName.equals("spoil_spots") || boneName.equals("flies"))) {
            return true;
        }
        return switch (normalizedCleanliness) {
            case CLEAN -> false;
            case USED -> boneName.equals("dirty_spots_light");
            case DIRTY -> boneName.equals("dirty_spots_medium");
            case FILTHY -> boneName.equals("dirty_spots_heavy");
        };
    }

    static String visibleBoneFor(BirdBathContentType type, int level) {
        if (type == null || type.isEmpty() || level <= 0 || type == BirdBathContentType.SPOILED) {
            return "";
        }
        String levelName = switch (level) {
            case 1 -> "down";
            case 2 -> "middle";
            case 3 -> "up";
            default -> "";
        };
        if (levelName.isEmpty()) {
            return "";
        }
        if (type == BirdBathContentType.FROZEN_WATER) {
            return "water_" + levelName;
        }
        return type.serializedName() + "_" + levelName;
    }
}
