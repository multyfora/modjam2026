package net.multyfora.modjam.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface DialogueTrigger {
    Map<Identifier, MapCodec<? extends DialogueTrigger>> TYPES = Map.of(
        Identifier.fromNamespaceAndPath("modjam", "inventory"), InventoryDialogueTrigger.CODEC
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