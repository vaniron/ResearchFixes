package cz.pokeland.researchfixes.mixin;

import com.cobblemon.mod.common.pokemon.Pokemon;
import cz.pokeland.researchfixes.Researchfixes;
import github.jorgaomc.events.CobblemonMasteryEventHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(value = CobblemonMasteryEventHandler.class, remap = false)
public class CobblemonMasteryEventHandlerMixin {

    // CHANGED: ServerPlayerEntity -> ServerPlayer
    @Inject(method = "tryResolveOwnerPlayer", at = @At("HEAD"), cancellable = true)
    private static void injectResolveOwnerFix(Pokemon p, CallbackInfoReturnable<ServerPlayer> cir) {
        try {
            if (p == null) {
                cir.setReturnValue(null);
                return;
            }

            UUID ownerUUID = p.getOwnerUUID();
            if (ownerUUID == null) {
                cir.setReturnValue(null);
                return;
            }

            var server = Researchfixes.getServer();
            if (server != null) {
                // CHANGED: getPlayerManager() -> getPlayerList() in Mojang mappings
                // If getPlayerManager() is red, try .getPlayerList()
                ServerPlayer player = server.getPlayerList().getPlayer(ownerUUID);

                cir.setReturnValue(player);
            }
        } catch (Exception e) {
            cir.setReturnValue(null);
        }
    }
}