package com.irtimaled.bbor.client;

import com.irtimaled.bbor.client.events.*;
import com.irtimaled.bbor.client.gui.SettingsScreen;
import com.irtimaled.bbor.client.keyboard.KeyListener;
import com.irtimaled.bbor.common.CommonProxy;
import com.irtimaled.bbor.common.EventBus;
import com.irtimaled.bbor.common.VillageColorCache;
import com.irtimaled.bbor.config.ConfigManager;
import com.irtimaled.bbor.config.Setting;
import net.minecraft.world.dimension.DimensionType;

import java.net.InetSocketAddress;

public class ClientProxy extends CommonProxy {
    public static final String Name = "Bounding Box Outline Reloaded";
    public static boolean active;

    static {
        KeyListener.register("Toggle Active", 0x42, Name)
                .onKeyPressHandler(ClientProxy::toggleActive)
                .onLongKeyPressHandler(60, SettingsScreen::show);
        KeyListener.register("Toggle Outer Box Only", 0x4f, Name)
                .onKeyPressHandler(ClientProxy::toggleOuterBoxesOnly);
    }

    public static void toggleActive() {
        active = !active;
        if (active)
            PlayerCoords.setActiveY();
    }

    private static void toggleOuterBoxesOnly() {
        Setting<Boolean> outerBoxesOnly = ConfigManager.outerBoxesOnly;
        outerBoxesOnly.set(!outerBoxesOnly.get());
    }

    private ClientRenderer renderer;

    @Override
    public void init() {
        super.init();
        EventBus.subscribe(Render.class, e -> render(e.getDimensionType()));
        EventBus.subscribe(ConnectedToRemoteServer.class, e -> connectedToServer(e.getInternetAddress()));
        EventBus.subscribe(DisconnectedFromRemoteServer.class, e -> disconnectedFromServer());
        EventBus.subscribe(InitializeClientReceived.class, e -> setWorldData(e.getSeed(), e.getSpawnX(), e.getSpawnZ()));
        EventBus.subscribe(AddBoundingBoxReceived.class, e -> runOnCache(e.getDimensionType(), cache -> cache.addBoundingBoxes(e.getKey(), e.getBoundingBoxes())));
        EventBus.subscribe(RemoveBoundingBoxReceived.class, e -> removeBoundingBox(e.getDimensionType(), e.getKey()));

        renderer = new ClientRenderer(this::getCache);
        KeyListener.init();
    }

    private void render(DimensionType dimensionType) {
        if (active) {
            renderer.render(dimensionType, ConfigManager.outerBoxesOnly.get());
        }
    }

    private void connectedToServer(InetSocketAddress internetAddress) {
        NBTFileParser.loadLocalDatFiles(internetAddress.getHostName(), internetAddress.getPort(), this::setWorldData, this::getOrCreateCache);
    }

    private void disconnectedFromServer() {
        active = false;
        if (ConfigManager.keepCacheBetweenSessions.get()) return;
        VillageColorCache.clear();
        clearCaches();
    }

    @Override
    protected void setWorldData(long seed, int spawnX, int spawnZ) {
        super.setWorldData(seed, spawnX, spawnZ);
        renderer.setWorldData(seed, spawnX, spawnZ);
    }


}
