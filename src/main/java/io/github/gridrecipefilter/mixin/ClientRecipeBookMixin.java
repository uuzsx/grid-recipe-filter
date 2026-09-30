package io.github.gridrecipefilter.mixin;
import io.github.gridrecipefilter.client.CraftingCatalog;
import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientRecipeBook.class)
abstract class ClientRecipeBookMixin {
 @Shadow public abstract List<RecipeCollection> getCollections();
 @Inject(method="getCollection",at=@At("HEAD"),cancellable=true)
 private void gridRecipeFilter$allCrafting(RecipeBookCategories category,CallbackInfoReturnable<List<RecipeCollection>> cir){
  if(category==RecipeBookCategories.CRAFTING_SEARCH&&CraftingCatalog.enabled())
   cir.setReturnValue(getCollections().stream().filter(c->c.getRecipes().stream().anyMatch(r->r.value() instanceof CraftingRecipe)).toList());
 }
}
