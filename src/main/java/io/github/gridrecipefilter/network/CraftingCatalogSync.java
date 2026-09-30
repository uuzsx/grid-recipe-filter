package io.github.gridrecipefilter.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CraftingCatalogSync {
    private static final Map<MinecraftServer, Set<UUID>> pending = new WeakHashMap<>();
    private CraftingCatalogSync() {}

    public static List<RecipeDisplayEntry> collect(RecipeManager manager) {
        List<RecipeDisplayEntry> displays = new ArrayList<>();
        for (var recipe : manager.getRecipes()) {
            if (recipe.value() instanceof CraftingRecipe) {
                manager.listDisplaysForRecipe(recipe.id(), entry -> {
                    if (entry.display() instanceof ShapedCraftingRecipeDisplay
                            || entry.display() instanceof ShapelessCraftingRecipeDisplay) displays.add(entry);
                });
            }
        }
        return List.copyOf(displays);
    }

    public static void sync(OnDatapackSyncEvent event) {
        // This event precedes vanilla tag synchronization. Send at tick end, after tag packets are queued.
        var players = pending.computeIfAbsent(event.getPlayerList().getServer(), ignored -> new HashSet<>());
        event.getRelevantPlayers().forEach(player -> players.add(player.getUUID()));
    }

    public static void flush(ServerTickEvent.Post event) {
        var server = event.getServer();
        Set<UUID> players = pending.remove(server);
        if (players == null) return;
        List<RecipeDisplayEntry> catalog = collect(server.getRecipeManager());
        for (UUID uuid : players) {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player == null || !player.connection.hasChannel(CraftingCatalogPayload.TYPE)) continue;
            if (catalog.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new CraftingCatalogPayload(true, true, List.of()));
            } else {
                for (int offset = 0; offset < catalog.size(); offset += 32) {
                    int end = Math.min(offset + 32, catalog.size());
                    PacketDistributor.sendToPlayer(player, new CraftingCatalogPayload(offset == 0, end == catalog.size(),
                            catalog.subList(offset, end)));
                }
            }
        }
    }
}
