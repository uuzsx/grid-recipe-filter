package io.github.gridrecipefilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Matches material types, independent of grid positions, components and stack sizes. */
public final class IngredientMatcher {
    private IngredientMatcher() {}

    public static <T> boolean matches(Collection<T> gridMaterials, List<? extends Set<T>> ingredients) {
        List<T> materials = new ArrayList<>(new LinkedHashSet<>(gridMaterials));
        if (materials.size() > ingredients.size()) return false;
        int[] assignedMaterial = new int[ingredients.size()];
        Arrays.fill(assignedMaterial, -1);
        for (int material = 0; material < materials.size(); material++) {
            if (!assign(material, materials, ingredients, assignedMaterial, new boolean[ingredients.size()])) {
                return false;
            }
        }
        return true;
    }

    private static <T> boolean assign(int material, List<T> materials, List<? extends Set<T>> ingredients,
                                      int[] assignedMaterial, boolean[] visited) {
        for (int slot = 0; slot < ingredients.size(); slot++) {
            if (visited[slot] || !ingredients.get(slot).contains(materials.get(material))) continue;
            visited[slot] = true;
            // Reassign earlier matches so overlapping tags do not make the result order-dependent.
            if (assignedMaterial[slot] == -1
                    || assign(assignedMaterial[slot], materials, ingredients, assignedMaterial, visited)) {
                assignedMaterial[slot] = material;
                return true;
            }
        }
        return false;
    }
}
