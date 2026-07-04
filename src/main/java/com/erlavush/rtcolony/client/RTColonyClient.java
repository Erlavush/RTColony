package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = RTColony.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RTColony.MOD_ID, value = Dist.CLIENT)
public class RTColonyClient {
    public RTColonyClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        RTColony.LOGGER.info("RTColony client setup");
    }
}

