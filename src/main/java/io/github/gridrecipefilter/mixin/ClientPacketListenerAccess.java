package io.github.gridrecipefilter.mixin;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccess {
    @Invoker("refreshRecipeBook") void gridRecipeFilter$refreshBook(ClientRecipeBook book);
}
