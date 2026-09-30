package io.github.gridrecipefilter.mixin;

import io.github.gridrecipefilter.client.GridFilterAccess;
import io.github.gridrecipefilter.client.CraftingCatalog;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingRecipeBookComponent.class)
abstract class CraftingRecipeBookComponentMixin {
    @Inject(method = "canDisplay", at = @At("RETURN"), cancellable = true)
    private void gridRecipeFilter$select(RecipeDisplay display, CallbackInfoReturnable<Boolean> cir) {
        boolean previewFits = CraftingCatalog.enabled() && switch (display) {
            case ShapedCraftingRecipeDisplay shaped -> shaped.width() <= 3 && shaped.height() <= 3;
            case ShapelessCraftingRecipeDisplay shapeless -> shapeless.ingredients().size() <= 9;
            default -> false;
        };
        cir.setReturnValue((cir.getReturnValueZ() || previewFits) && ((GridFilterAccess) this).gridRecipeFilter$matches(display));
    }
}
