package dev.muffintastic.fmsaaomgtd;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = fixmojangsstupidassambientocclusionmeshgeneratortheyredumb.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = fixmojangsstupidassambientocclusionmeshgeneratortheyredumb.MODID, value = Dist.CLIENT)
public class fixmojangsstupidassambientocclusionmeshgeneratortheyredumbClient {
    public fixmojangsstupidassambientocclusionmeshgeneratortheyredumbClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }
}
