package io.github.gridrecipefilter.client;

import io.github.gridrecipefilter.GridRecipeFilter;
import io.github.gridrecipefilter.network.CraftingCatalogPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(value = GridRecipeFilter.MOD_ID, dist = Dist.CLIENT)
public final class GridRecipeFilterClient {
    public GridRecipeFilterClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, GridRecipeFilter.CONFIG);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(GridRecipeFilterClient::registerHandlers);
        NeoForge.EVENT_BUS.addListener(GridRecipeFilterClient::logout);
        NeoForge.EVENT_BUS.addListener(GridRecipeFilterClient::tick);
    }

    private static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CraftingCatalogPayload.TYPE, (payload, context) -> CraftingCatalog.receive(payload));
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        CraftingCatalog.clear();
    }

    private static void tick(ClientTickEvent.Post event) { CraftingCatalog.tick(); }
}
