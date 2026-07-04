package com.erlavush.rtcolony;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(RTColony.MOD_ID)
public class RTColony {
    public static final String MOD_ID = "rtcolony";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RTColony(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("RTColony common setup");
    }
}

