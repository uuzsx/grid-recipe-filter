package io.github.gridrecipefilter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.ModConfigSpec;
@Mod(GridRecipeFilter.MOD_ID)
public final class GridRecipeFilter {
 public static final String MOD_ID="grid_recipe_filter";
 public static final ModConfigSpec.BooleanValue ENABLED, ALL_CRAFTING_RECIPES;
 public static final ModConfigSpec CONFIG;
 static {
  var b=new ModConfigSpec.Builder();
  ENABLED=b.translation("grid_recipe_filter.configuration.enabled").define("enabled",true);
  ALL_CRAFTING_RECIPES=b.translation("grid_recipe_filter.configuration.allCraftingRecipes").define("allCraftingRecipes",true);
  CONFIG=b.build();
 }
 public GridRecipeFilter(IEventBus bus,ModContainer container) {}
 public static void toggle(){ ENABLED.set(!ENABLED.get()); CONFIG.save(); }
}
