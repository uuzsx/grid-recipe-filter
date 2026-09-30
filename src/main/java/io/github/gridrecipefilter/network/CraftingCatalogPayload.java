package io.github.gridrecipefilter.network;

import io.github.gridrecipefilter.GridRecipeFilter;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

public record CraftingCatalogPayload(boolean first, boolean last, List<RecipeDisplayEntry> entries)
        implements CustomPacketPayload {
    public static final Type<CraftingCatalogPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GridRecipeFilter.MOD_ID, "crafting_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingCatalogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CraftingCatalogPayload::first,
            ByteBufCodecs.BOOL, CraftingCatalogPayload::last,
            RecipeDisplayEntry.STREAM_CODEC.apply(ByteBufCodecs.list(64)), CraftingCatalogPayload::entries,
            CraftingCatalogPayload::new);

    public CraftingCatalogPayload { entries = List.copyOf(entries); }

    @Override public Type<CraftingCatalogPayload> type() { return TYPE; }
}
