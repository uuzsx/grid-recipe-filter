package io.github.gridrecipefilter.client;
import io.github.gridrecipefilter.GridRecipeFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.item.crafting.RecipeHolder;
public final class CraftingCatalog {
 private static boolean observed;
 private CraftingCatalog(){}
 // 1.21.1 already synchronizes the full RecipeManager through the vanilla protocol.
 public static boolean enabled(){return GridRecipeFilter.ALL_CRAFTING_RECIPES.get();}
 public static boolean locked(RecipeHolder<?> r){var p=Minecraft.getInstance().player;return p!=null&&!p.getRecipeBook().contains(r);}
 public static void refresh(){
  var mc=Minecraft.getInstance();if(mc.player==null)return;
  var book=mc.player.getRecipeBook();
  book.getCollections().forEach(c->c.updateKnownRecipes(book));
  if(mc.screen instanceof RecipeUpdateListener listener)listener.recipesUpdated();
 }
 public static void tick(){boolean mode=enabled();if(mode!=observed){observed=mode;refresh();}}
}
