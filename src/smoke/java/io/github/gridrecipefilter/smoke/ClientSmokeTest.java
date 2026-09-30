package io.github.gridrecipefilter.smoke;

import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.client.GridFilterAccess;
import io.github.gridrecipefilter.client.CraftingCatalog;
import io.github.gridrecipefilter.mixin.ClientRecipeBookAccess;
import io.github.gridrecipefilter.network.CraftingCatalogPayload;
import io.github.gridrecipefilter.network.CraftingCatalogSync;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Runs in a disposable world; never included in the normal release build. */
@EventBusSubscriber(modid = GridRecipeFilter.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static final List<String> checks = new ArrayList<>();
    private static int stage;
    private static int delay;
    private static long started = System.nanoTime();
    private static RecipeBookComponent<?> component;
    private static AbstractCraftingMenu menu;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == 99) return;
            if ((System.nanoTime() - started) / 1_000_000_000 > 180) throw new AssertionError("Integration run timed out at stage " + stage);
            if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                stage = 1;
                mc.options.pauseOnLostFocus = false;
                mc.options.languageCode = "zh_cn";
                mc.createWorldOpenFlows().createFreshLevel("grid-filter-smoke-" + System.currentTimeMillis(),
                        new LevelSettings("Grid Filter Smoke", GameType.SURVIVAL,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
            } else if (stage == 1 && mc.player != null && mc.level != null && mc.getSingleplayerServer() != null
                    && CraftingCatalog.available()) {
                testModCatalog(mc);
                stage = 6;
                delay = 12;
            } else if (stage == 6 && --delay <= 0) {
                Screenshot.grab(mc.gameDirectory, "mod-locked-workbench.png", mc.getMainRenderTarget(), 1, ignored -> {});
                mc.screen.onClose();
                stage = 2;
                mc.getSingleplayerServer().execute(() -> {
                    var server = mc.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "recipe give @s *");
                });
            } else if (stage == 2 && mc.player.getRecipeBook().getCollections().size() > 20) {
                stage = 3;
                openAndTest(mc, false);
                delay = 12;
            } else if (stage == 3 && --delay <= 0) {
                Screenshot.grab(mc.gameDirectory, "inventory-filter.png", mc.getMainRenderTarget(), 1, ignored -> {});
                stage = 4;
                openAndTest(mc, true);
                delay = 12;
            } else if (stage == 4 && --delay <= 0) {
                Screenshot.grab(mc.gameDirectory, "crafting-filter.png", mc.getMainRenderTarget(), 1, ignored -> {});
                stage = 5;
                delay = 20;
            } else if (stage == 5 && --delay <= 0) {
                finish(mc, null);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(mc, failure);
        }
    }

    private static void openAndTest(Minecraft mc, boolean table) throws Exception {
        GridRecipeFilter.ENABLED.set(true);
        GridRecipeFilter.ALL_CRAFTING_RECIPES.set(false);
        CraftingCatalog.refresh();
        mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, true);
        Screen screen;
        if (table) {
            menu = new CraftingMenu(1, mc.player.getInventory());
            mc.player.containerMenu = menu;
            screen = new CraftingScreen((CraftingMenu) menu, mc.player.getInventory(), Component.literal("Crafting test"));
        } else {
            menu = mc.player.inventoryMenu;
            screen = new InventoryScreen(mc.player);
        }
        mc.setScreen(screen);
        Field field = AbstractRecipeBookScreen.class.getDeclaredField("recipeBookComponent");
        field.setAccessible(true);
        component = (RecipeBookComponent<?>) field.get(screen);
        check(component.isVisible(), "Recipe book opens for " + (table ? "3x3" : "2x2"));
        Method selector = component.getClass().getDeclaredMethod("canDisplay", RecipeDisplay.class);
        selector.setAccessible(true);

        RecipeDisplay torch = new ShapelessCraftingRecipeDisplay(List.of(
                new SlotDisplay.Composite(List.of(item(Items.COAL), item(Items.CHARCOAL))), item(Items.STICK)),
                item(Items.TORCH), item(Items.CRAFTING_TABLE));
        RecipeDisplay pickaxe = new ShapedCraftingRecipeDisplay(3, 3, List.of(
                item(Items.IRON_INGOT), item(Items.IRON_INGOT), item(Items.IRON_INGOT),
                SlotDisplay.Empty.INSTANCE, item(Items.STICK), SlotDisplay.Empty.INSTANCE,
                SlotDisplay.Empty.INSTANCE, item(Items.STICK), SlotDisplay.Empty.INSTANCE),
                item(Items.IRON_PICKAXE), item(Items.CRAFTING_TABLE));

        setGrid(Items.STICK);
        check(selected(selector, torch), "Partial stick matches torch");
        check(selected(selector, pickaxe) == table, "Vanilla grid-size limits preserved");
        setGrid(Items.COAL, Items.STICK);
        check(selected(selector, torch), "Coal and stick match torch");
        check(!((GridFilterAccess) component).gridRecipeFilter$matches(pickaxe), "Coal excludes pickaxe");
        setGrid(Items.COAL, Items.CHARCOAL);
        check(!selected(selector, torch), "Two alternatives cannot share one ingredient slot");
        setGrid(Items.TORCH);
        check(!selected(selector, torch), "Output is not treated as an ingredient");
        setGrid(Items.CRAFTING_TABLE);
        check(!selected(selector, torch), "Crafting station is not treated as an ingredient");

        RecipeDisplay milk = new ShapelessCraftingRecipeDisplay(List.of(
                new SlotDisplay.WithRemainder(item(Items.MILK_BUCKET), item(Items.BUCKET))),
                item(Items.CAKE), item(Items.CRAFTING_TABLE));
        setGrid(Items.BUCKET);
        check(!selected(selector, milk), "Container remainder is not an ingredient");
        setGrid(Items.MILK_BUCKET);
        check(selected(selector, milk), "Remainder-wrapped input is an ingredient");
        RecipeDisplay tagRecipe = new ShapelessCraftingRecipeDisplay(List.of(new SlotDisplay.TagSlotDisplay(
                TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("planks")))),
                item(Items.STICK), item(Items.CRAFTING_TABLE));
        setGrid(Items.OAK_PLANKS);
        check(selected(selector, tagRecipe), "Vanilla item tags resolve in the client world");

        setGrid(Items.COAL, Items.STICK, Items.STICK);
        check(selected(selector, torch), "Repeated type across input slots is ignored");
        var entry1 = entry(1, torch);
        var entry2 = entry(2, tagRecipe);
        RecipeCollection group = new RecipeCollection(List.of(entry1, entry2));
        group.selectRecipes(new StackedItemContents(), recipe -> {
            try { return selected(selector, recipe); } catch (Exception e) { throw new RuntimeException(e); }
        });
        check(group.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY).equals(List.of(entry1)),
                "Grouped recipes keep only matching individual variants");
        check(!group.hasCraftable(), "Material matching does not mark incomplete recipes craftable");

        Button button = (Button) getField("gridRecipeFilter$button");
        boolean clicked = component.mouseClicked(new MouseButtonEvent(button.getX() + 5, button.getY() + 5,
                new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)), false);
        boolean afterClickEnabled = GridRecipeFilter.ENABLED.get();
        boolean afterClickMatch = selected(selector, tagRecipe);
        check(clicked && !afterClickEnabled && afterClickMatch, "Mouse toggle restores all materials"
                + " (clicked=" + clicked + ", enabled=" + afterClickEnabled + ", match=" + afterClickMatch + ")");
        check(component.keyPressed(new KeyEvent(GLFW.GLFW_KEY_G, 0, GLFW.GLFW_MOD_CONTROL))
                && GridRecipeFilter.ENABLED.get() && !selected(selector, tagRecipe), "Ctrl+G toggles the filter");

        EditBox search = (EditBox) getField("searchBox");
        search.setValue("torch");
        Method searchUpdate = RecipeBookComponent.class.getDeclaredMethod("checkSearchStringUpdate");
        searchUpdate.setAccessible(true);
        searchUpdate.invoke(component);
        setGrid(Items.STICK);
        check(search.getValue().equals("torch"), "Grid refresh retains the vanilla search text");
        search.setValue("");
        searchUpdate.invoke(component);
        setGrid();
        check(selected(selector, torch) && selected(selector, tagRecipe), "Empty grid restores vanilla recipe visibility");
        setGrid(Items.COAL, Items.STICK);
    }

    private static void testModCatalog(Minecraft mc) throws Exception {
        GridRecipeFilter.ENABLED.set(true);
        GridRecipeFilter.ALL_CRAFTING_RECIPES.set(true);
        CraftingCatalog.refresh();
        menu = mc.player.inventoryMenu;
        mc.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, true);
        mc.setScreen(new InventoryScreen(mc.player));
        Field field = AbstractRecipeBookScreen.class.getDeclaredField("recipeBookComponent");
        field.setAccessible(true);
        component = (RecipeBookComponent<?>) field.get(mc.screen);
        var manager = mc.getSingleplayerServer().getRecipeManager();
        RecipeDisplayEntry plate = fixture(manager, "smoke_plate");
        RecipeDisplayEntry frame = fixture(manager, "smoke_frame");
        RecipeDisplayEntry tag = fixture(manager, "smoke_tag");
        var known = ((ClientRecipeBookAccess) mc.player.getRecipeBook()).gridRecipeFilter$known();
        check(CraftingCatalog.entry(plate.id()) != null && CraftingCatalog.entry(frame.id()) != null
                && CraftingCatalog.entry(tag.id()) != null, "Network-synchronized catalog includes all three real mod recipes");
        check(!known.containsKey(plate.id()) && !known.containsKey(frame.id()) && !known.containsKey(tag.id()),
                "Catalog does not add locked recipes to the player's actual known recipes");
        check(CraftingCatalog.locked(plate.id()), "Locked recipe status uses the real player unlock state");

        setGrid(SmokeFixtures.INGOT.get());
        Set<RecipeDisplayId> expected = Set.of(plate.id(), frame.id(), tag.id());
        check(selectedIds(mc).equals(expected), "A registered mod ingot shows every matching crafting recipe, including locked 3x3");
        menu.getInputGridSlots().getFirst().set(new ItemStack(SmokeFixtures.INGOT.get(), 64));
        component.tick();
        check(selectedIds(mc).equals(expected), "Mod input stack count does not change the filtered results");
        component.slotClicked(menu.getInputGridSlots().getFirst());
        StackedItemContents sufficient = new StackedItemContents();
        menu.fillCraftSlotsStackedContents(sufficient);
        RecipeCollection plateCollection = mc.player.getRecipeBook().getCollection(SearchRecipeBookCategory.CRAFTING).stream()
                .filter(c -> c.getRecipes().stream().anyMatch(e -> e.id().equals(plate.id()))).findFirst().orElseThrow();
        check(plate.canCraft(sufficient) && !plateCollection.isCraftable(plate.id()) && !plateCollection.hasCraftable(),
                "Locked recipe stays red even when its materials are sufficient");
        check(plateCollection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE).isEmpty()
                && !plateCollection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY).isEmpty(),
                "Only-craftable mode excludes locked recipes while the full list retains them");
        RecipeButton lockedButton = new RecipeButton(() -> 0);
        lockedButton.init(plateCollection, false, (RecipeBookPage) getField("recipeBookPage"), SlotDisplayContext.fromLevel(mc.level));
        var plateStack = new ItemStack(SmokeFixtures.PLATE.get());
        check(lockedButton.getTooltipText(plateStack).equals(Screen.getTooltipFromItem(mc, plateStack)),
                "Locked recipe button retains the normal item tooltip without the extra yellow locked explanation");
        setGrid(SmokeFixtures.ALLOY.get());
        check(selectedIds(mc).equals(Set.of(tag.id())), "A second mod item matches through a mod-defined ingredient tag");
        setGrid(SmokeFixtures.PLATE.get());
        check(selectedIds(mc).isEmpty(), "A mod output item does not falsely count as its recipe ingredient");

        setGrid(SmokeFixtures.INGOT.get());
        EditBox search = (EditBox) getField("searchBox");
        search.setValue("smoke_plate");
        Method searchUpdate = RecipeBookComponent.class.getDeclaredMethod("checkSearchStringUpdate");
        searchUpdate.setAccessible(true);
        searchUpdate.invoke(component);
        Object page = getField("recipeBookPage");
        Field pageCollections = page.getClass().getDeclaredField("recipeCollections");
        pageCollections.setAccessible(true);
        @SuppressWarnings("unchecked") List<RecipeCollection> searched = (List<RecipeCollection>) pageCollections.get(page);
        check(searched.size() == 1 && searched.getFirst().getRecipes().stream().anyMatch(e -> e.id().equals(plate.id())),
                "Vanilla text search includes locked mod recipes from the full catalog (results=" + searched.size()
                        + ", mode=" + CraftingCatalog.enabled() + ")");
        search.setValue("");
        searchUpdate.invoke(component);

        Method place = RecipeBookComponent.class.getDeclaredMethod("tryPlaceRecipe", RecipeCollection.class, RecipeDisplayId.class, boolean.class);
        place.setAccessible(true);
        var originalScreen = mc.screen;
        var originalMenu = mc.player.containerMenu;
        check((boolean) place.invoke(component, new RecipeCollection(List.of(plate)), plate.id(), false)
                        && mc.screen == originalScreen && ghostIngredients().size() == 2,
                "Locked 2x2-compatible recipe uses native ingredient and result ghosts without opening another UI");
        check((boolean) place.invoke(component, new RecipeCollection(List.of(frame)), frame.id(), false)
                        && mc.screen == originalScreen && ghostIngredients().size() == 1
                        && ghostIngredients().containsKey(menu.getResultSlot()),
                "Oversized inventory recipe shows its result safely without indexing nonexistent input slots");
        check(mc.player.containerMenu == originalMenu && menu.getInputGridSlots().getFirst().getItem().is(SmokeFixtures.INGOT.get()),
                "Native ghosts keep the active crafting container and input material intact");
        check(!known.containsKey(frame.id()) && mc.getSingleplayerServer().submit(() ->
                        !mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getRecipeBook()
                                .contains(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(GridRecipeFilter.MOD_ID, "smoke_frame"))))
                        .get(), "Native ghosts do not unlock the recipe on the client or server");

        List<RecipeDisplayEntry> all = CraftingCatalogSync.collect(manager);
        boolean wireValid = true;
        for (int offset = 0; offset < all.size(); offset += 32) {
            int end = Math.min(offset + 32, all.size());
            var payload = new CraftingCatalogPayload(offset == 0, end == all.size(), all.subList(offset, end));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level.registryAccess());
            try {
                CraftingCatalogPayload.STREAM_CODEC.encode(buffer, payload);
                wireValid &= buffer.readableBytes() < 1_000_000;
                var decoded = CraftingCatalogPayload.STREAM_CODEC.decode(buffer);
                wireValid &= decoded.entries().size() == payload.entries().size()
                        && decoded.first() == payload.first() && decoded.last() == payload.last();
                for (int i = 0; i < decoded.entries().size(); i++) {
                    wireValid &= decoded.entries().get(i).id().equals(payload.entries().get(i).id());
                }
            } finally { buffer.release(); }
        }
        check(wireValid, "Complete catalog round-trips through the actual registry-aware network codec in bounded batches");
        int count = CraftingCatalog.category(SearchRecipeBookCategory.CRAFTING).size();
        CraftingCatalog.receive(new CraftingCatalogPayload(true, false, all.subList(0, all.size() / 2)));
        check(CraftingCatalog.category(SearchRecipeBookCategory.CRAFTING).size() == count, "Partial catalog batches do not replace the visible catalog");
        CraftingCatalog.receive(new CraftingCatalogPayload(false, true, all.subList(all.size() / 2, all.size())));
        check(CraftingCatalog.category(SearchRecipeBookCategory.CRAFTING).size() == count, "Completed catalog batches atomically refresh the view");

        CraftingCatalog.clear();
        CraftingCatalog.refresh();
        check(!CraftingCatalog.available() && !mc.player.getRecipeBook().getCollections().stream()
                        .flatMap(c -> c.getRecipes().stream()).anyMatch(e -> e.id().equals(frame.id())),
                "Without server catalog the client falls back to its real unlocked recipes");
        CraftingCatalog.receive(new CraftingCatalogPayload(true, true, all));
        setGrid(SmokeFixtures.INGOT.get());
        check(selectedIds(mc).equals(expected), "Catalog replacement restores locked mod recipes without duplicating them");
        menu = new CraftingMenu(1, mc.player.getInventory());
        mc.player.containerMenu = menu;
        mc.setScreen(new CraftingScreen((CraftingMenu) menu, mc.player.getInventory(), Component.translatable("container.crafting")));
        component = (RecipeBookComponent<?>) field.get(mc.screen);
        setGrid(SmokeFixtures.INGOT.get());
        originalScreen = mc.screen;
        place.invoke(component, new RecipeCollection(List.of(frame)), frame.id(), false);
        var ghosts = ghostIngredients();
        check(mc.screen == originalScreen && ghosts.size() == 9 && ghosts.containsKey(menu.getResultSlot()),
                "Locked 3x3 recipe shows eight ingredient ghosts and its output inside the original workbench");
        check(!ghosts.containsKey(menu.getInputGridSlots().get(4)), "Shaped recipe preserves its empty center slot");
        component.slotClicked(menu.getInputGridSlots().getFirst());
        check(ghostIngredients().isEmpty(), "Clicking a crafting slot clears native recipe ghosts");
        menu.getInputGridSlots().getFirst().set(new ItemStack(SmokeFixtures.INGOT.get(), 64));
        place.invoke(component, new RecipeCollection(List.of(frame)), frame.id(), false);
        testOccupiedGhosts(mc);
    }

    private static void testOccupiedGhosts(Minecraft mc) throws Exception {
        GhostSlots ghosts = (GhostSlots) getField("ghostSlots");
        var occupied = menu.getInputGridSlots().getFirst();
        var later = menu.getInputGridSlots().get(1);
        GuiRenderState render = ghostRenderState(mc, ghosts);
        check(ghostItemCount(render) == 8 && !hasGhostAt(render, occupied),
                "Native render extraction draws the eight empty slots only, without any icon or red overlay over the real input");
        check(occupied.getItem().is(SmokeFixtures.INGOT.get()) && occupied.getItem().getCount() == 64,
                "Clicking a locked recipe preserves the real 64-item input stack");
        later.set(new ItemStack(Items.DIAMOND, 12));
        render = ghostRenderState(mc, ghosts);
        check(ghostItemCount(render) == 7 && !hasGhostAt(render, later) && later.getItem().getCount() == 12,
                "A different real item added after selecting the recipe immediately suppresses its ghost and overlay");

        GuiGraphicsExtractor tooltipGraphics = new GuiGraphicsExtractor(mc, new GuiRenderState(), 0, 0);
        Field deferredTooltip = GuiGraphicsExtractor.class.getDeclaredField("deferredTooltip");
        deferredTooltip.setAccessible(true);
        tooltipGraphics.setComponentTooltipForNextFrame(mc.font, Screen.getTooltipFromItem(mc, later.getItem()),
                0, 0, later.getItem(), null);
        Object realTooltip = deferredTooltip.get(tooltipGraphics);
        ghosts.extractTooltip(tooltipGraphics, mc, 0, 0, later);
        check(realTooltip != null && deferredTooltip.get(tooltipGraphics) == realTooltip,
                "Hovering an occupied preview slot keeps the real item's tooltip");
        later.set(ItemStack.EMPTY);
        render = ghostRenderState(mc, ghosts);
        check(ghostItemCount(render) == 8 && hasGhostAt(render, later),
                "Emptying the slot restores its native ingredient preview without selecting the recipe again");
        // Tooltip extraction is deferred once per frame; use a fresh frame for the now-empty slot.
        tooltipGraphics = new GuiGraphicsExtractor(mc, new GuiRenderState(), 0, 0);
        ghosts.extractTooltip(tooltipGraphics, mc, 0, 0, later);
        check(deferredTooltip.get(tooltipGraphics) != null,
                "An empty preview slot still provides the ghost ingredient tooltip");

        var output = menu.getResultSlot();
        output.set(new ItemStack(Items.APPLE, 3));
        render = ghostRenderState(mc, ghosts);
        check(ghostItemCount(render) == 7 && !hasGhostAt(render, output) && output.getItem().getCount() == 3,
                "An occupied output slot also retains its real item without a ghost or red overlay");
        output.set(ItemStack.EMPTY);
        check(ghostItemCount(ghostRenderState(mc, ghosts)) == 8 && ghostIngredients().size() == 9,
                "Empty output restores its preview while the full stored recipe remains intact");
    }

    private static GuiRenderState ghostRenderState(Minecraft mc, GhostSlots ghosts) {
        GuiRenderState state = new GuiRenderState();
        ghosts.extractRenderState(new GuiGraphicsExtractor(mc, state, 0, 0), mc, true);
        return state;
    }

    private static int ghostItemCount(GuiRenderState state) {
        List<GuiItemRenderState> items = new ArrayList<>();
        state.forEachItem(items::add);
        return items.size();
    }

    private static boolean hasGhostAt(GuiRenderState state, net.minecraft.world.inventory.Slot slot) {
        boolean[] found = {false};
        state.forEachItem(item -> { if (item.x() == slot.x && item.y() == slot.y) found[0] = true; });
        state.forEachElement(element -> {
            if (element instanceof ColoredRectangleRenderState rectangle && rectangle.x0() <= slot.x
                    && rectangle.y0() <= slot.y && rectangle.x1() >= slot.x + 16 && rectangle.y1() >= slot.y + 16) {
                found[0] = true;
            }
        }, GuiRenderState.TraverseRange.ALL);
        return found[0];
    }

    private static RecipeDisplayEntry fixture(RecipeManager manager, String name) {
        List<RecipeDisplayEntry> entries = new ArrayList<>();
        manager.listDisplaysForRecipe(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(GridRecipeFilter.MOD_ID, name)), entries::add);
        if (entries.size() != 1) throw new AssertionError("Fixture recipe failed to load: " + name);
        return entries.getFirst();
    }

    private static Set<RecipeDisplayId> selectedIds(Minecraft mc) {
        return mc.player.getRecipeBook().getCollection(SearchRecipeBookCategory.CRAFTING).stream()
                .flatMap(collection -> collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY).stream())
                .map(RecipeDisplayEntry::id).collect(Collectors.toSet());
    }

    private static SlotDisplay item(Item item) { return new SlotDisplay.ItemSlotDisplay(item); }
    private static RecipeDisplayEntry entry(int id, RecipeDisplay display) {
        return new RecipeDisplayEntry(new RecipeDisplayId(id), display, OptionalInt.empty(),
                RecipeBookCategories.CRAFTING_MISC, Optional.empty());
    }
    private static boolean selected(Method method, RecipeDisplay recipe) throws Exception {
        return (boolean) method.invoke(component, recipe);
    }
    private static Object getField(String name) throws Exception {
        Field field = RecipeBookComponent.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(component);
    }
    private static java.util.Map<?, ?> ghostIngredients() throws Exception {
        Object ghosts = getField("ghostSlots");
        Field field = ghosts.getClass().getDeclaredField("ingredients");
        field.setAccessible(true);
        return (java.util.Map<?, ?>) field.get(ghosts);
    }
    private static void setGrid(Item... items) {
        for (int slot = 0; slot < menu.getInputGridSlots().size(); slot++) {
            menu.getInputGridSlots().get(slot).set(slot < items.length ? new ItemStack(items[slot], 1) : ItemStack.EMPTY);
        }
        component.tick();
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks.add("PASS: " + message);
    }
    private static void finish(Minecraft mc, Throwable failure) {
        stage = 99;
        try {
            String result = failure == null ? "PASS: " + checks.size() + " integration checks" : "FAIL: " + failure;
            Files.writeString(Path.of(mc.gameDirectory.toString(), "smoke-result.txt"), result + "\n" + String.join("\n", checks));
            System.out.println("GRID_FILTER_SMOKE " + result);
        } catch (Exception e) { e.printStackTrace(); }
        mc.stop();
    }
}
