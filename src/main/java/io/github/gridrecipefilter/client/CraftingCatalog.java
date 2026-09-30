package io.github.gridrecipefilter.client;

import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.mixin.ClientPacketListenerAccess;
import io.github.gridrecipefilter.mixin.ClientRecipeBookAccess;
import io.github.gridrecipefilter.network.CraftingCatalogPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;

/** A separate display catalog: the player's actual known recipes are never modified. */
public final class CraftingCatalog {
    private static final List<RecipeDisplayEntry> pending = new ArrayList<>();
    private static Map<RecipeDisplayId, RecipeDisplayEntry> entries = Map.of();
    private static ClientRecipeBook displayBook;
    private static boolean receiving;
    private static boolean needsRefresh;
    private static boolean observedMode;
    private CraftingCatalog() {}

    public static void receive(CraftingCatalogPayload payload) {
        if (payload.first()) {
            pending.clear();
            receiving = true;
        }
        if (!receiving) return;
        pending.addAll(payload.entries());
        if (!payload.last()) return;
        ClientRecipeBook book = new ClientRecipeBook();
        Map<RecipeDisplayId, RecipeDisplayEntry> complete = new HashMap<>();
        for (RecipeDisplayEntry entry : pending) {
            complete.put(entry.id(), entry);
            book.add(entry);
        }
        book.rebuildCollections();
        entries = Map.copyOf(complete);
        displayBook = book;
        pending.clear();
        receiving = false;
        needsRefresh = true;
        refresh();
    }

    public static void clear() {
        pending.clear();
        receiving = false;
        entries = Map.of();
        displayBook = null;
        needsRefresh = false;
    }

    public static boolean available() { return displayBook != null; }
    public static boolean enabled() { return available() && GridRecipeFilter.ALL_CRAFTING_RECIPES.get(); }
    public static RecipeDisplayEntry entry(RecipeDisplayId id) { return entries.get(id); }

    public static boolean isPlayerBook(ClientRecipeBook book) {
        var player = Minecraft.getInstance().player;
        return enabled() && player != null && player.getRecipeBook() == book;
    }

    public static boolean locked(RecipeDisplayId id) {
        var player = Minecraft.getInstance().player;
        return player != null && entries.containsKey(id)
                && !((ClientRecipeBookAccess) player.getRecipeBook()).gridRecipeFilter$known().containsKey(id);
    }

    public static boolean crafting(RecipeDisplay display) {
        return display instanceof ShapedCraftingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay;
    }

    public static List<RecipeCollection> collections(List<RecipeCollection> original) {
        List<RecipeCollection> result = new ArrayList<>(displayBook.getCollections());
        for (RecipeCollection collection : original) {
            var other = collection.getRecipes().stream().filter(entry -> !crafting(entry.display())).toList();
            if (other.size() == collection.getRecipes().size()) result.add(collection);
            else if (!other.isEmpty()) result.add(new RecipeCollection(other));
        }
        return List.copyOf(result);
    }

    /** Null means this category belongs to another workstation and should retain vanilla behavior. */
    public static List<RecipeCollection> category(ExtendedRecipeBookCategory category) {
        if (category == SearchRecipeBookCategory.CRAFTING) return displayBook.getCollections();
        var collections = displayBook.getCollection(category);
        if (!collections.isEmpty() || category == RecipeBookCategories.CRAFTING_BUILDING_BLOCKS
                || category == RecipeBookCategories.CRAFTING_EQUIPMENT || category == RecipeBookCategories.CRAFTING_MISC
                || category == RecipeBookCategories.CRAFTING_REDSTONE) return collections;
        return null;
    }

    public static void refresh() {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && mc.getConnection() != null) {
            needsRefresh = false;
            ((ClientPacketListenerAccess) mc.getConnection()).gridRecipeFilter$refreshBook(mc.player.getRecipeBook());
        } else if (available()) {
            needsRefresh = true;
        }
    }

    public static void tick() {
        boolean mode = enabled();
        if (mode != observedMode) {
            observedMode = mode;
            needsRefresh = true;
        }
        if (needsRefresh) refresh();
    }
}
