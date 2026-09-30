package io.github.gridrecipefilter.client;

import io.github.gridrecipefilter.IngredientMatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

public final class GridFilterState {
    private final Map<RecipeDisplay, List<Set<Item>>> alternatives = new IdentityHashMap<>();
    private final Map<RecipeDisplay, Boolean> results = new IdentityHashMap<>();
    private Set<Item> materials = Set.of();
    private Level level;
    private ContextMap context;

    public static Set<Item> snapshot(AbstractCraftingMenu menu) {
        Set<Item> items = new HashSet<>();
        for (Slot slot : menu.getInputGridSlots()) {
            if (!slot.getItem().isEmpty()) items.add(slot.getItem().getItem());
        }
        return Set.copyOf(items);
    }

    public void capture(AbstractCraftingMenu menu, Level currentLevel) {
        if (level != currentLevel) {
            invalidate();
            level = currentLevel;
            context = currentLevel == null ? null : SlotDisplayContext.fromLevel(currentLevel);
        }
        Set<Item> current = snapshot(menu);
        if (!materials.equals(current)) {
            materials = current;
            results.clear();
        }
    }

    public void invalidate() {
        alternatives.clear();
        results.clear();
        level = null;
        context = null;
    }

    public boolean matches(RecipeDisplay display) {
        if (materials.isEmpty() || context == null) return true;
        return results.computeIfAbsent(display, recipe -> {
            List<SlotDisplay> ingredients = switch (recipe) {
                case ShapedCraftingRecipeDisplay shaped -> shaped.ingredients();
                case ShapelessCraftingRecipeDisplay shapeless -> shapeless.ingredients();
                default -> null;
            };
            // Leave non-crafting displays to the vanilla selector.
            if (ingredients == null) return true;
            List<Set<Item>> choices = alternatives.computeIfAbsent(recipe, ignored -> resolve(ingredients));
            return IngredientMatcher.matches(materials, choices);
        });
    }

    private List<Set<Item>> resolve(List<SlotDisplay> ingredients) {
        List<Set<Item>> resolved = new ArrayList<>();
        for (SlotDisplay ingredient : ingredients) {
            if (ingredient instanceof SlotDisplay.Empty) continue;
            Set<Item> items = new HashSet<>();
            for (ItemStack stack : ingredient.resolveForStacks(context)) {
                if (!stack.isEmpty()) items.add(stack.getItem());
            }
            resolved.add(Set.copyOf(items));
        }
        return List.copyOf(resolved);
    }
}
