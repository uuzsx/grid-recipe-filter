package io.github.gridrecipefilter.mixin;

import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.client.GridFilterAccess;
import io.github.gridrecipefilter.client.GridFilterState;
import io.github.gridrecipefilter.client.CraftingCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin<T extends RecipeBookMenu> implements GridFilterAccess {
    @Shadow @Final protected T menu;
    @Shadow protected Minecraft minecraft;
    @Shadow private EditBox searchBox;
    @Shadow @Final private GhostSlots ghostSlots;
    @Shadow public abstract boolean isVisible();
    @Shadow public abstract void fillGhostRecipe(RecipeDisplay display);
    @Shadow private int getXOrigin() { throw new AssertionError(); }
    @Shadow private int getYOrigin() { throw new AssertionError(); }
    @Shadow private boolean isFiltering() { throw new AssertionError(); }
    @Shadow private void updateStackedContents() { throw new AssertionError(); }
    @Shadow private void updateTabs(boolean filtering) { throw new AssertionError(); }
    @Shadow private void updateCollections(boolean resetPage, boolean filtering) { throw new AssertionError(); }

    @Unique private final GridFilterState gridRecipeFilter$state = new GridFilterState();
    @Unique private Button gridRecipeFilter$button;
    @Unique private Set<Item> gridRecipeFilter$observedMaterials = Set.of();
    @Unique private boolean gridRecipeFilter$observedEnabled;
    @Unique private boolean gridRecipeFilter$observedAllRecipes;

    @Inject(method = "selectMatchingRecipes()V", at = @At("HEAD"))
    private void gridRecipeFilter$capture(CallbackInfo ci) {
        if (menu instanceof AbstractCraftingMenu crafting) {
            gridRecipeFilter$state.capture(crafting, minecraft.level);
        }
    }

    @Override
    public boolean gridRecipeFilter$matches(RecipeDisplay display) {
        return !GridRecipeFilter.ENABLED.get() || gridRecipeFilter$state.matches(display);
    }

    @Inject(method = "recipesUpdated", at = @At("HEAD"))
    private void gridRecipeFilter$clearCache(CallbackInfo ci) {
        gridRecipeFilter$state.invalidate();
    }

    @Inject(method = "initVisuals", at = @At("TAIL"))
    private void gridRecipeFilter$addButton(CallbackInfo ci) {
        if (!(menu instanceof AbstractCraftingMenu crafting)) return;
        searchBox.setWidth(59);
        gridRecipeFilter$button = Button.builder(Component.empty(), button -> gridRecipeFilter$toggle())
                .bounds(getXOrigin() + 88, getYOrigin() + 12, 18, 16)
                .createNarration(ignored -> Component.translatable("grid_recipe_filter.narration",
                        Component.translatable(GridRecipeFilter.ENABLED.get()
                                ? "grid_recipe_filter.on" : "grid_recipe_filter.off")))
                .build();
        gridRecipeFilter$observedMaterials = GridFilterState.snapshot(crafting);
        gridRecipeFilter$observedEnabled = GridRecipeFilter.ENABLED.get();
        gridRecipeFilter$observedAllRecipes = GridRecipeFilter.ALL_CRAFTING_RECIPES.get();
        gridRecipeFilter$updateButton();
    }

    @Unique
    private void gridRecipeFilter$updateButton() {
        if (gridRecipeFilter$button == null) return;
        boolean enabled = GridRecipeFilter.ENABLED.get();
        gridRecipeFilter$button.setMessage(Component.translatable("grid_recipe_filter.button")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        gridRecipeFilter$button.setTooltip(Tooltip.create(Component.translatable(
                enabled ? "grid_recipe_filter.tooltip.enabled" : "grid_recipe_filter.tooltip.disabled")
                .append("\n").append(Component.translatable(CraftingCatalog.enabled()
                        ? "grid_recipe_filter.catalog.full" : "grid_recipe_filter.catalog.known"))));
    }

    @Unique
    private void gridRecipeFilter$refresh() {
        updateStackedContents();
        updateTabs(isFiltering());
        updateCollections(true, isFiltering());
        gridRecipeFilter$updateButton();
    }

    @Unique
    private void gridRecipeFilter$toggle() {
        GridRecipeFilter.toggle();
        searchBox.setFocused(false);
        gridRecipeFilter$observedEnabled = GridRecipeFilter.ENABLED.get();
        gridRecipeFilter$refresh();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void gridRecipeFilter$trackGrid(CallbackInfo ci) {
        if (!isVisible() || !(menu instanceof AbstractCraftingMenu crafting)) return;
        Set<Item> current = GridFilterState.snapshot(crafting);
        boolean enabled = GridRecipeFilter.ENABLED.get();
        boolean allRecipes = GridRecipeFilter.ALL_CRAFTING_RECIPES.get();
        if (allRecipes != gridRecipeFilter$observedAllRecipes) {
            gridRecipeFilter$observedAllRecipes = allRecipes;
            CraftingCatalog.refresh();
            gridRecipeFilter$refresh();
        }
        if (!current.equals(gridRecipeFilter$observedMaterials) || enabled != gridRecipeFilter$observedEnabled) {
            gridRecipeFilter$observedMaterials = current;
            gridRecipeFilter$observedEnabled = enabled;
            gridRecipeFilter$refresh();
        }
    }

    @Inject(method = "tryPlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void gridRecipeFilter$preview(RecipeCollection collection, RecipeDisplayId id, boolean useMaxItems,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!CraftingCatalog.enabled() || !(menu instanceof AbstractCraftingMenu crafting)) return;
        var entry = CraftingCatalog.entry(id);
        if (entry == null) return;
        boolean fits = switch (entry.display()) {
            case ShapedCraftingRecipeDisplay shaped -> shaped.width() <= crafting.getGridWidth()
                    && shaped.height() <= crafting.getGridHeight();
            case ShapelessCraftingRecipeDisplay shapeless -> shapeless.ingredients().size()
                    <= crafting.getGridWidth() * crafting.getGridHeight();
            default -> true;
        };
        if (!fits || CraftingCatalog.locked(id)) {
            if (fits) {
                fillGhostRecipe(entry.display());
            } else {
                // A 3x3 recipe cannot be laid out in four slots. Keep its output visible and explain the size.
                ghostSlots.clear();
                ghostSlots.setResult(crafting.getResultSlot(), SlotDisplayContext.fromLevel(minecraft.level), entry.display().result());
                minecraft.gui.setOverlayMessage(Component.translatable("grid_recipe_filter.requires_workbench"), false);
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void gridRecipeFilter$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (isVisible() && gridRecipeFilter$button != null) {
            gridRecipeFilter$button.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void gridRecipeFilter$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (isVisible() && gridRecipeFilter$button != null && !minecraft.player.isSpectator()
                && gridRecipeFilter$button.mouseClicked(event, doubleClick)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void gridRecipeFilter$shortcut(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (isVisible() && gridRecipeFilter$button != null && !minecraft.player.isSpectator()
                && event.key() == GLFW.GLFW_KEY_G && event.hasControlDown()) {
            gridRecipeFilter$toggle();
            cir.setReturnValue(true);
        }
    }

    @ModifyArg(method = "updateNarration", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;findNarratableWidget(Ljava/util/List;Lnet/minecraft/client/gui/narration/NarratableEntry;)Lnet/minecraft/client/gui/screens/Screen$NarratableSearchResult;"), index = 0)
    private List<NarratableEntry> gridRecipeFilter$narration(List<NarratableEntry> entries) {
        if (gridRecipeFilter$button == null) return entries;
        List<NarratableEntry> result = new ArrayList<>(entries);
        result.add(gridRecipeFilter$button);
        return result;
    }
}
