package io.github.gridrecipefilter.mixin;

import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GhostSlots.class)
abstract class GhostSlotsMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/Reference2ObjectMap;forEach(Ljava/util/function/BiConsumer;)V"), index = 0)
    private <T> BiConsumer<Slot, T> gridRecipeFilter$emptySlotsOnly(BiConsumer<Slot, T> drawGhost) {
        // Check every frame so items placed or synced after selecting a recipe stay visible too.
        // Keep the stored preview intact: emptying a slot can show its ingredient again.
        return (slot, ghost) -> {
            if (!slot.hasItem()) drawGhost.accept(slot, ghost);
        };
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void gridRecipeFilter$realItemTooltip(GuiGraphics graphics, Minecraft minecraft,
            int mouseX, int mouseY, Slot hoveredSlot, CallbackInfo ci) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) ci.cancel();
    }
}
