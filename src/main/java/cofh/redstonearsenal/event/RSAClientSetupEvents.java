package cofh.redstonearsenal.event;

import cofh.lib.client.renderer.entity.NothingRenderer;
import cofh.redstonearsenal.RedstoneArsenal;
import cofh.redstonearsenal.client.renderer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;

import static cofh.lib.util.constants.Constants.ID_REDSTONE_ARSENAL;
import static cofh.redstonearsenal.init.RSAReferences.*;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_REDSTONE_ARSENAL, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RSAClientSetupEvents {

    @SubscribeEvent
    public static void entityLayerSetup(final EntityRenderersEvent.AddLayers event) {

        EntityModelSet models = event.getEntityModels();
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>> renderer = event.getSkin(skin);
            if (renderer != null) {
                try {
                    castRenderer(renderer).addLayer(new FluxElytraLayer<>(castRenderer(renderer), models));
                } catch (ClassCastException e) {
                    RedstoneArsenal.LOG.log(Level.ERROR, "Failed render layer cast: " + renderer);
                }
            }
        }
        EntityRenderDispatcher manager = Minecraft.getInstance().getEntityRenderDispatcher();
        for (EntityRenderer<?> renderer : manager.renderers.values()) {
            if (renderer instanceof HumanoidMobRenderer || renderer instanceof ArmorStandRenderer) {
                LivingEntityRenderer<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> livingRenderer = (LivingEntityRenderer<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) renderer;
                try {
                    castRenderer(livingRenderer).addLayer(new FluxElytraLayer<>(castRenderer(livingRenderer), models));
                } catch (ClassCastException e) {
                    RedstoneArsenal.LOG.log(Level.ERROR, "Failed render layer cast: " + renderer);
                }
            }
        }
    }

    // pain
    private static <T extends LivingEntity, M extends EntityModel<T>> LivingEntityRenderer<T, M> castRenderer(LivingEntityRenderer<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> renderer) throws ClassCastException {

        return (LivingEntityRenderer<T, M>) renderer;
    }

    @SubscribeEvent
    public static void entityRendererSetup(final EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(FLUX_SLASH_ENTITY, FluxSlashRenderer::new);
        event.registerEntityRenderer(FLUX_ARROW_ENTITY, FluxArrowRenderer::new);
        event.registerEntityRenderer(FLUX_TRIDENT_ENTITY, ThrownFluxTridentRenderer::new);
        event.registerEntityRenderer(FLUX_WRENCH_ENTITY, FluxWrenchRenderer::new);
        event.registerEntityRenderer(SHOCKWAVE_ENTITY, NothingRenderer::new);
        event.registerEntityRenderer(FISH_HOOK_ENTITY, FluxFishingHookRenderer::new);
    }

    // region RELOAD
    @SubscribeEvent
    public static void registerReloadListeners(final RegisterClientReloadListenersEvent event) {

        event.registerReloadListener(FluxTridentBEWLR.INSTANCE);
    }
    // endregion

}
