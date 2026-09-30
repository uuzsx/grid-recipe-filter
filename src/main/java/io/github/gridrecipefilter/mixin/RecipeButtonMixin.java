package io.github.gridrecipefilter.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RecipeButton.class)
abstract class RecipeButtonMixin {
    @Shadow public abstract RecipeDisplayId getCurrentRecipe();

    @Redirect(method = "extractWidgetRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection;hasCraftable()Z"))
    private boolean gridRecipeFilter$currentRecipeBackground(RecipeCollection collection) {
        // A grouped button can cycle between locked and unlocked variants; color the displayed variant.
        return collection.isCraftable(getCurrentRecipe());
    }
}
