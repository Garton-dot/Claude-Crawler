package com.papyrusfollower.client;

import com.papyrusfollower.PapyrusFollower;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class PapyrusFollowerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(PapyrusFollower.PAPYRUS, PapyrusRenderer::new);
    }
}
