package net.multyfora.don.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface DialogueTrigger {
    Map<Identifier, MapCodec<? extends DialogueTrigger>> TYPES = Map.of(
        Identifier.fromNamespaceAndPath("don", "inventory"), InventoryDialogueTrigger.CODEC,
        Identifier.fromNamespaceAndPath("don", "never"), NeverDialogueTrigger.CODEC,
        Identifier.fromNamespaceAndPath("don", "nearby_block"), NearbyBlockDialogueTrigger.CODEC,
        Identifier.fromNamespaceAndPath("don", "time_after"), TimeAfterDialogueTrigger.CODEC,
        Identifier.fromNamespaceAndPath("don", "elapsed"), ElapsedTimeDialogueTrigger.CODEC,
        Identifier.fromNamespaceAndPath("don", "at_structure"), AtStructureDialogueTrigger.CODEC,
        Identifier.fromNamespaceAndPath("don", "not_fired"), NotFiredDialogueTrigger.CODEC
    );

    Codec<DialogueTrigger> CODEC = Codec.STRING.fieldOf("type")
        .xmap(Identifier::parse, id -> id.toString())
        .partialDispatch(
            trigger -> DataResult.success(trigger.type()),
            id -> {
                MapCodec<? extends DialogueTrigger> codec = TYPES.get(id);
                return codec == null
                    ? DataResult.error(() -> "Unknown dialogue trigger type: " + id)
                    : DataResult.success(codec);
            }
        );

    Identifier type();

    boolean matches(ServerPlayer player);
}