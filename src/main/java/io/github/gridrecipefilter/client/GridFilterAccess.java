package io.github.gridrecipefilter.client;

import net.minecraft.world.item.crafting.display.RecipeDisplay;

public interface GridFilterAccess {
    boolean gridRecipeFilter$matches(RecipeDisplay display);
}
