package dev.muffintastic.fmsaaomgtd.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.muffintastic.fmsaaomgtd.ao.AOWrapperCache;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hook into Minecraft's block rendering code for AO and run our own logic
 * 
 * Yeah this is kinda shit but not as shit as Mojang
 */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
    private static final String TESSELATE_BLOCK_TARGET =
        "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V";

    @ModifyArg(
        method =
            "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At(value = "INVOKE", target = TESSELATE_BLOCK_TARGET)
    )
    private VertexConsumer fmsaaomgtd$wrapBatched(final VertexConsumer consumer) {
        return AOWrapperCache.wrap(consumer);
    }

    @ModifyArg(
        method =
            "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V",
        at = @At(value = "INVOKE", target = TESSELATE_BLOCK_TARGET)
    )
    private VertexConsumer fmsaaomgtd$wrapBreaking(final VertexConsumer consumer) {
        return AOWrapperCache.wrap(consumer);
    }
}
