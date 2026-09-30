package io.github.gridrecipefilter.mixin;
import java.util.List;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(RecipeButton.class)
abstract class RecipeButtonMixin {
 @Shadow private float time;
 @Shadow private List<RecipeHolder<?>> getOrderedRecipes(){throw new AssertionError();}
 @Redirect(method="renderWidget",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection;hasCraftable()Z"))
 private boolean gridRecipeFilter$currentVariant(RecipeCollection collection){
  var list=getOrderedRecipes();return !list.isEmpty()&&collection.isCraftable(list.get(Mth.floor(time/30.0F)%list.size()));
 }
}
