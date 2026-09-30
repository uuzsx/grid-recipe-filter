package io.github.gridrecipefilter.client;
import io.github.gridrecipefilter.GridRecipeFilter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
@Mod(value=GridRecipeFilter.MOD_ID,dist=Dist.CLIENT)
public final class GridRecipeFilterClient {
 public GridRecipeFilterClient(IEventBus bus,ModContainer container){
  container.registerConfig(ModConfig.Type.CLIENT,GridRecipeFilter.CONFIG);
  container.registerExtensionPoint(IConfigScreenFactory.class,ConfigurationScreen::new);
  NeoForge.EVENT_BUS.addListener(GridRecipeFilterClient::tick);
 }
 private static void tick(ClientTickEvent.Post event){ CraftingCatalog.tick(); }
}
