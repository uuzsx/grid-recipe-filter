package io.github.gridrecipefilter.smoke;

import io.github.gridrecipefilter.GridRecipeFilter;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real registered mod materials and data-pack recipes, present only in development tests. */
@Mod(GridRecipeFilter.MOD_ID)
public final class SmokeFixtures {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GridRecipeFilter.MOD_ID);
    public static final DeferredItem<Item> INGOT = ITEMS.registerSimpleItem("smoke_ingot", new Item.Properties());
    public static final DeferredItem<Item> ALLOY = ITEMS.registerSimpleItem("smoke_alloy", new Item.Properties());
    public static final DeferredItem<Item> PLATE = ITEMS.registerSimpleItem("smoke_plate", new Item.Properties());

    public SmokeFixtures(IEventBus bus) { ITEMS.register(bus); }
}
