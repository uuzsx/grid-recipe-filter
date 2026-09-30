package io.github.gridrecipefilter;

import io.github.gridrecipefilter.network.CraftingCatalogPayload;
import io.github.gridrecipefilter.network.CraftingCatalogSync;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(GridRecipeFilter.MOD_ID)
public final class GridRecipeFilter {
    public static final String MOD_ID = "grid_recipe_filter";
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ALL_CRAFTING_RECIPES;
    public static final ModConfigSpec CONFIG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Filter crafting recipes by material types currently in the input grid.")
                .translation("grid_recipe_filter.configuration.enabled").define("enabled", true);
        ALL_CRAFTING_RECIPES = builder.comment("Include locked crafting recipes when the server provides the full catalog.")
                .translation("grid_recipe_filter.configuration.allCraftingRecipes").define("allCraftingRecipes", true);
        CONFIG = builder.build();
    }

    public GridRecipeFilter(IEventBus modBus, ModContainer container) {
        modBus.addListener(GridRecipeFilter::registerPayloads);
        NeoForge.EVENT_BUS.addListener(CraftingCatalogSync::sync);
        NeoForge.EVENT_BUS.addListener(CraftingCatalogSync::flush);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2").optional().playToClient(CraftingCatalogPayload.TYPE, CraftingCatalogPayload.STREAM_CODEC);
    }

    public static void toggle() {
        ENABLED.set(!ENABLED.get());
        CONFIG.save();
    }
}
