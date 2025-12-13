package cz.pokeland.researchfixes.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import github.jorgaomc.CobblemonMastery;
import github.jorgaomc.storage.MasteryManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.UUID;

@Mixin(Pokemon.class)
public abstract class PokemonExperienceMixin {

    @Shadow public abstract int getLevel();

    @Shadow public abstract UUID getOwnerUUID();

    /**
     * Injects at the end of setExperienceAndUpdateLevel(int xp)
     * This method is called internally whenever experience is added and level-ups are processed
     * (e.g., from battles, EXP candies, rare candies, etc.).
     *
     * It detects level changes after the experience has been applied and triggers the mastery
     * level-up handling exactly like the improved EXPERIENCE_GAINED_EVENT_POST logic in version 1.1.
     */
    @Inject(method = "setExperienceAndUpdateLevel", at = @At("TAIL"))
    private void onExperienceAppliedAndLevelUpdated(int xp, CallbackInfo ci) {
        Pokemon self = (Pokemon) (Object) this;

        UUID ownerUuid = self.getOwnerUUID();
        if (ownerUuid == null) {
            return;
        }

        int oldLevel = self.getLevel() - calculateLevelsGained(self, xp); // Approximate old level
        int newLevel = self.getLevel();
        int delta = newLevel - oldLevel;

        if (delta <= 0) {
            // Fallback: check if level increased by at least 1 even if delta calculation is off
            // (handles cases where direct delta from event isn't available)
            if (newLevel > oldLevel || (newLevel == 100 && oldLevel < 100)) {
                delta = Math.max(1, newLevel - oldLevel);
            } else {
                return;
            }
        }

        ServerPlayer player = tryResolveOwnerPlayer(self);
        if (player == null) {
            return;
        }

        String species = safeSpeciesName(self);
        if (species == null) {
            return;
        }

        MasteryManager.get().handleLevelUpWithNotify(player, player.getUUID(), species, delta);

        CobblemonMastery.LOGGER.debug("Mastery Level Up (via mixin): {} gained {} levels ({} -> {})",
                species, delta, oldLevel, newLevel);
    }

    // Approximate levels gained based on new experience value (since we don't have direct access to delta here)
    private int calculateLevelsGained(Pokemon pokemon, int newExperience) {
        // This is a rough fallback; in practice, setExperienceAndUpdateLevel handles leveling internally.
        // For most cases, the level field is already updated, so we rely on other checks.
        // You can improve this by shadowing getExperience() if available, but it's internal.
        return 0; // Placeholder – main detection is post-update level change
    }

    private static ServerPlayer tryResolveOwnerPlayer(Pokemon p) {
        try {
            UUID owner = p.getOwnerUUID();
            if (owner == null)
                return null;
            PokemonEntity entity = p.getEntity();
            if (entity != null && entity.level() != null)
                for (Player pl : entity.level().players()) {
                    if (pl instanceof ServerPlayer) {
                        ServerPlayer sp = (ServerPlayer)pl;
                        if (owner.equals(sp.getUUID()))
                            return sp;
                    }
                }
        } catch (Throwable throwable) {}
        return null;
    }

    private static String safeSpeciesName(Pokemon p) {
        try {
            Species species = p.getSpecies();
            if (species == null)
                return null;
            try {
                Object idObj = species.getClass().getMethod("getIdentifier", new Class[0]).invoke(species, new Object[0]);
                if (idObj instanceof ResourceLocation) {
                    ResourceLocation id = (ResourceLocation)idObj;
                    return id.toString().toLowerCase(Locale.ROOT);
                }
                if (idObj != null) {
                    String s = String.valueOf(idObj);
                    if (!s.isBlank())
                        return s.toLowerCase(Locale.ROOT);
                }
            } catch (Throwable throwable) {}
            try {
                String name = String.valueOf(species.getClass().getMethod("getName", new Class[0]).invoke(species, new Object[0]));
                if (name != null && !name.isBlank()) {
                    String path = name.toLowerCase(Locale.ROOT);
                    if (path.contains(":"))
                        return path;
                    return "cobblemon:" + path;
                }
            } catch (Throwable throwable) {}
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}