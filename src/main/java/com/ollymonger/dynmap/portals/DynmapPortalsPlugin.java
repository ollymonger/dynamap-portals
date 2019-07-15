package com.ollymonger.dynmap.portals;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public class DynmapPortalsPlugin extends JavaPlugin implements Listener {
    static final String DYNMAP_PLUGIN_NAME = "dynmap";

    static final String SET_ID_PORTALS = "nether_portals";
    static final String SET_NAME_PORTALS = "Nether Portals";

    private MarkerAPI markerApi;
    private MarkerSet portalSet;

    @Override
    public void onEnable() {
        getLogger().info("DynmapPortalsPlugin is now enabled");

        this.getServer().getPluginManager().registerEvents(this, this);

        this.initialiseMarkerApi();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        new Thread(new DynmapStructuresRunnable(event.getChunk())).start();
    }

    private class DynmapStructuresRunnable implements Runnable {
        private Chunk chunk;

        private DynmapStructuresRunnable(Chunk chunk) {
            this.chunk = chunk;
        }

        @Override
        public void run() {
            getLogger().info("runnable running for " + this.chunk.getX() + ", " + this.chunk.getZ());
        }
    }

    private void initialiseMarkerApi() {
        if (Bukkit.getPluginManager().isPluginEnabled(DYNMAP_PLUGIN_NAME) == false) {
            throw new IllegalStateException("No Dynmap plugin found");
        }

        DynmapCommonAPI plugin = (DynmapCommonAPI) Bukkit.getPluginManager().getPlugin(DYNMAP_PLUGIN_NAME);

        if (plugin == null) {
            throw new IllegalStateException("No Dynmap plugin found");
        }

        this.markerApi = plugin.getMarkerAPI();
        getLogger().info("Marker API retrieved");

        this.portalSet = this.markerApi.getMarkerSet(SET_ID_PORTALS);

        if (this.portalSet == null) {
            getLogger().info("Set not found, creating new set");
            this.portalSet = this.markerApi.createMarkerSet(SET_ID_PORTALS, SET_NAME_PORTALS, null, true);
        }

        this.portalSet.setHideByDefault(false);
        getLogger().info("Set initialised");
    }
}
