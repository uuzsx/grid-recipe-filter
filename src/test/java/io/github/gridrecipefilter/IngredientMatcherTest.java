package io.github.gridrecipefilter;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngredientMatcherTest {
    private static final List<Set<String>> TORCH = List.of(Set.of("coal", "charcoal"), Set.of("stick"));
    private static final List<Set<String>> PICKAXE = List.of(Set.of("iron"), Set.of("iron"), Set.of("iron"),
            Set.of("stick"), Set.of("stick"));

    @Test void emptyGridShowsRecipesEvenWithNoIngredients() {
        assertTrue(IngredientMatcher.matches(List.of(), TORCH));
        assertTrue(IngredientMatcher.matches(List.of(), List.of()));
    }

    @Test void partialMaterialsDoNotRequireACompleteRecipe() {
        assertTrue(IngredientMatcher.matches(List.of("stick"), TORCH));
        assertTrue(IngredientMatcher.matches(List.of("stick"), PICKAXE));
        assertTrue(IngredientMatcher.matches(List.of("iron"), PICKAXE));
    }

    @Test void everyProvidedMaterialMustBeAnIngredient() {
        assertTrue(IngredientMatcher.matches(List.of("coal", "stick"), TORCH));
        assertFalse(IngredientMatcher.matches(List.of("coal", "stick"), PICKAXE));
        assertFalse(IngredientMatcher.matches(List.of("dirt"), TORCH));
    }

    @Test void repeatedMaterialTypesAndTheirOrderAreIgnored() {
        assertTrue(IngredientMatcher.matches(List.of("stick", "stick", "stick", "coal"), TORCH));
        assertTrue(IngredientMatcher.matches(List.of("coal", "stick"), TORCH));
        assertTrue(IngredientMatcher.matches(List.of("iron", "stick", "iron"), PICKAXE));
    }

    @Test void alternativesCannotShareOneRecipeSlotForTwoDifferentMaterials() {
        assertTrue(IngredientMatcher.matches(List.of("charcoal", "stick"), TORCH));
        assertFalse(IngredientMatcher.matches(List.of("coal", "charcoal"), TORCH));
        assertFalse(IngredientMatcher.matches(List.of("coal", "charcoal", "stick"), TORCH));
    }

    @Test void overlappingTagsCanReassignAnEarlierMatch() {
        List<Set<String>> ingredients = List.of(Set.of("oak", "birch"), Set.of("oak"));
        assertTrue(IngredientMatcher.matches(List.of("oak", "birch"), ingredients));
        assertTrue(IngredientMatcher.matches(List.of("birch", "oak"), ingredients));
    }

    @Test void emptyIngredientSlotsCannotAcceptMaterials() {
        assertFalse(IngredientMatcher.matches(List.of("stick"), List.of(Set.of())));
        assertTrue(IngredientMatcher.matches(List.of("stick"), List.of(Set.of(), Set.of("stick"))));
    }

    @Test void moreMaterialTypesThanRecipeSlotsFails() {
        assertFalse(IngredientMatcher.matches(List.of("oak", "birch", "spruce"),
                List.of(Set.of("oak", "birch", "spruce"), Set.of("oak", "birch", "spruce"))));
    }
}
