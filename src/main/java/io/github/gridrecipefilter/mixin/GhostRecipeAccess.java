package io.github.gridrecipefilter.mixin;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(GhostRecipe.class)
public interface GhostRecipeAccess {
 @Accessor("time") float gridRecipeFilter$time();
 @Accessor("time") void gridRecipeFilter$time(float time);
}
