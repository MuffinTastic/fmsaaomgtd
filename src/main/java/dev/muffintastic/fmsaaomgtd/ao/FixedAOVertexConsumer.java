package dev.muffintastic.fmsaaomgtd.ao;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;

/**
 * Wraps the quad emission code and flips vertices around if we determine them to cause
 * the nasty AO lighting bug
 */
public class FixedAOVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;

    public FixedAOVertexConsumer(final VertexConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void putBulkData(
        final PoseStack.Pose pose,
        final BakedQuad quad,
        final float[] brightness,
        final float red,
        final float green,
        final float blue,
        final float alpha,
        final int[] lightmap,
        final int packedOverlay,
        final boolean readExistingColor
    ) {
        if (
            brightness.length == 4 &&
            lightmap.length == 4 &&
            quad.getVertices().length == 32 &&
            AODiagonalFlip.shouldFlipQuad(brightness, lightmap)
        ) {
            final int[] verts = quad.getVertices();
            final int[] flippedVertices = new int[verts.length];
            final float[] flippedBrightness = new float[4];
            final int[] flippedLightmap = new int[4];
            AODiagonalFlip.rotateQuad(verts, brightness, lightmap, flippedVertices, flippedBrightness, flippedLightmap);
            final BakedQuad flippedQuad = new BakedQuad(
                flippedVertices,
                quad.getTintIndex(),
                quad.getDirection(),
                quad.getSprite(),
                quad.isShade(),
                quad.hasAmbientOcclusion()
            );
            this.delegate.putBulkData(pose, flippedQuad, flippedBrightness, red, green, blue, alpha, flippedLightmap, packedOverlay, readExistingColor);
        } else {
            this.delegate.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readExistingColor);
        }
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z) {
        return this.delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
        return this.delegate.setColor(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer setUv(final float u, final float v) {
        return this.delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(final int u, final int v) {
        return this.delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(final int u, final int v) {
        return this.delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(final float nx, final float ny, final float nz) {
        return this.delegate.setNormal(nx, ny, nz);
    }
}
