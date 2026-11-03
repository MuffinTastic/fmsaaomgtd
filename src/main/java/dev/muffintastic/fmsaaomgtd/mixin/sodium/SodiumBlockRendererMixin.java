package dev.muffintastic.fmsaaomgtd.mixin.sodium;

import dev.muffintastic.fmsaaomgtd.ao.SodiumQuadFlip;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BlockRenderer.class)
public abstract class SodiumBlockRendererMixin {
    //private static final ThreadLocal<Boolean> ROTATED_THIS_QUAD = new ThreadLocal<>();

    @Inject(method = "bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V", at = @At("HEAD"))
    private void fmsaaomgtd_rotateQuadBeforeBuffer(
        final MutableQuadViewImpl quad,
        final float[] brightnesses,
        final Material material,
        final CallbackInfo callbackInfo) {
        // ROTATED_THIS_QUAD.set(
            SodiumQuadFlip.applyRotation(quad, brightnesses)
        // )
        ;
    }

    // Commented out because I don't think this is necessary and it has a (probably negligible, but existent) cost

    /*
    @Inject(method = "bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V", at = @At("TAIL"))
    private void fmsaaomgtd_unrotateQuadAfterBuffer(
        final MutableQuadViewImpl quad,
        final float[] brightnesses,
        final Material material,
        final CallbackInfo callbackInfo) {
        try {
            if (Boolean.TRUE.equals(ROTATED_THIS_QUAD.get())) {
                SodiumQuadFlip.undoRotation(quad, brightnesses);
            }
        } finally {
            ROTATED_THIS_QUAD.remove();
        }
    }
    */
}
