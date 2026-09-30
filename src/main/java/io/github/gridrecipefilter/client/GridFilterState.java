package io.github.gridrecipefilter.client;
import io.github.gridrecipefilter.IngredientMatcher;
import java.util.*;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
public final class GridFilterState {
 private final Map<Recipe<?>,List<Set<Item>>> choices=new IdentityHashMap<>();
 private final Map<Recipe<?>,Boolean> results=new IdentityHashMap<>();
 private Set<Item> materials=Set.of();
 public static boolean crafting(RecipeBookMenu<?,?> menu){return menu.getRecipeBookType()==RecipeBookType.CRAFTING;}
 public static Set<Item> snapshot(RecipeBookMenu<?,?> menu){
  Set<Item> found=new HashSet<>();
  int first=menu.getResultSlotIndex()+1;
  for(int i=first;i<first+menu.getGridWidth()*menu.getGridHeight();i++){
   var stack=menu.slots.get(i).getItem();if(!stack.isEmpty())found.add(stack.getItem());
  }
  return Set.copyOf(found);
 }
 public void capture(RecipeBookMenu<?,?> menu){var next=snapshot(menu);if(!next.equals(materials)){materials=next;results.clear();}}
 public void invalidate(){choices.clear();results.clear();}
 public boolean matches(Recipe<?> recipe){
  if(materials.isEmpty())return true;
  return results.computeIfAbsent(recipe,r->IngredientMatcher.matches(materials,choices.computeIfAbsent(r,ignored->{
   List<Set<Item>> out=new ArrayList<>();
   for(var ingredient:r.getIngredients()){
    if(ingredient.isEmpty())continue;
    Set<Item> items=new HashSet<>();for(var stack:ingredient.getItems())if(!stack.isEmpty())items.add(stack.getItem());
    out.add(Set.copyOf(items));
   }
   return List.copyOf(out);
  })));
 }
}
