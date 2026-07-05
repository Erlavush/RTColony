package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = RTColony.MOD_ID, dist = Dist.CLIENT)
public class RTColonyClient {
    public RTColonyClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(RTColonyKeyMappings::register);
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.register(RTColonyClientEvents.class);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        RTColony.LOGGER.info("RTColony client setup");
        if (ModList.get().isLoaded("jade")) {
            event.enqueueWork(RtsJadeIntegration::register);
        }
    }
}
