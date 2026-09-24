package EdDYON.guaniao.content.bird;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;

/**
 * A spawn egg whose item model already contains its final colors.
 *
 * <p>Forge registers an item-color handler for every {@link ForgeSpawnEggItem}
 * and normally multiplies layer 0 by the egg's background color. These eggs use
 * complete painted icons, so that multiplication would make the packaged icon
 * much darker than the source texture.</p>
 */
public final class FullColorSpawnEggItem extends ForgeSpawnEggItem {
    public FullColorSpawnEggItem(
            Supplier<? extends EntityType<? extends Mob>> entityType,
            int backgroundColor,
            int highlightColor,
            Item.Properties properties
    ) {
        super(entityType, backgroundColor, highlightColor, properties);
    }

    @Override
    public int getColor(int tintIndex) {
        return 0xFFFFFFFF;
    }
}
