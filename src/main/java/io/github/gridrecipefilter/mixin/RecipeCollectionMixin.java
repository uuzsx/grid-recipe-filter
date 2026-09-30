package io.github.gridrecipefilter.mixin;

import io.github.gridrecipefilter.client.CraftingCatalog;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeCollection.class)
abstract class RecipeCollectionMixin {
    @Shadow @Final private Set<RecipeDisplayId> craftable;

    @Inject(method = "selectRecipes", at = @At("TAIL"))
    private void gridRecipeFilter$lockedRecipesAreRed(StackedItemContents contents, Predicate<RecipeDisplay> selector, CallbackInfo ci) {
        if (CraftingCatalog.enabled()) craftable.removeIf(CraftingCatalog::locked);
    }
}
