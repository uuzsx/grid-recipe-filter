package io.github.gridrecipefilter.mixin;

import io.github.gridrecipefilter.client.CraftingCatalog;
import java.util.List;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientRecipeBook.class)
abstract class ClientRecipeBookMixin {
    @Inject(method = "getCollections", at = @At("RETURN"), cancellable = true)
    private void gridRecipeFilter$collections(CallbackInfoReturnable<List<RecipeCollection>> cir) {
        if (CraftingCatalog.isPlayerBook((ClientRecipeBook) (Object) this)) {
            cir.setReturnValue(CraftingCatalog.collections(cir.getReturnValue()));
        }
    }

    @Inject(method = "getCollection", at = @At("HEAD"), cancellable = true)
    private void gridRecipeFilter$category(ExtendedRecipeBookCategory category, CallbackInfoReturnable<List<RecipeCollection>> cir) {
        if (CraftingCatalog.isPlayerBook((ClientRecipeBook) (Object) this)) {
            var collections = CraftingCatalog.category(category);
            if (collections != null) cir.setReturnValue(collections);
        }
    }
}
