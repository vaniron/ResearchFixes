package cz.pokeland.researchfixes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class Researchfixes implements ModInitializer {
    private static MinecraftServer serverInstance;

    @Override
    public void onInitialize() {
        // Capture the server when it starts
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            serverInstance = server;
        });

        // Clear it when it stops to prevent leaks
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            serverInstance = null;
        });
    }

    // Static helper for our Mixin to use
    public static MinecraftServer getServer() {
        return serverInstance;
    }
}