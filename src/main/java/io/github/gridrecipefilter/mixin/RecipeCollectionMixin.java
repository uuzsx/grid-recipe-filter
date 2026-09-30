package io.github.gridrecipefilter.mixin;
import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.client.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RecipeCollection.class)
abstract class RecipeCollectionMixin {
 @Shadow @Final private List<RecipeHolder<?>> recipes;
 @Shadow @Final private Set<RecipeHolder<?>> known, fitsDimensions, craftable;
 @Unique private final GridFilterState gridRecipeFilter$state=new GridFilterState();
 @Inject(method="updateKnownRecipes",at=@At("TAIL"))
 private void gridRecipeFilter$known(RecipeBook book,CallbackInfo ci){
  known.clear();for(var r:recipes)if(book.contains(r)||(CraftingCatalog.enabled()&&r.value() instanceof CraftingRecipe))known.add(r);
 }
 @Inject(method="canCraft",at=@At("TAIL"))
 private void gridRecipeFilter$select(StackedContents contents,int width,int height,RecipeBook book,CallbackInfo ci){
  var player=Minecraft.getInstance().player;
  if(player==null||!(player.containerMenu instanceof RecipeBookMenu<?,?> menu)||!GridFilterState.crafting(menu))return;
  gridRecipeFilter$state.capture(menu);
  // Resolve the current tag-backed ingredients afresh after collection replacement on data reload.
  known.clear();fitsDimensions.clear();craftable.clear();
  for(var r:recipes){
   boolean unlocked=book.contains(r);
   boolean eligible=unlocked||(CraftingCatalog.enabled()&&r.value() instanceof CraftingRecipe);
   if(eligible)known.add(r);
   boolean fits=r.value().canCraftInDimensions(width,height)||(CraftingCatalog.enabled()&&r.value().canCraftInDimensions(3,3));
   boolean matches=!GridRecipeFilter.ENABLED.get()||gridRecipeFilter$state.matches(r.value());
   if(eligible&&fits&&matches){
    fitsDimensions.add(r);
    if(unlocked&&r.value().canCraftInDimensions(width,height)&&contents.canCraft(r.value(),null))craftable.add(r);
   }
  }
 }
}
