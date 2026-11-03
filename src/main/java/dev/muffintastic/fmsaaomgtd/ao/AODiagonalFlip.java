package dev.muffintastic.fmsaaomgtd.ao;

/**
 * Flipping logic
 */
public final class AODiagonalFlip {
    private AODiagonalFlip() {}

    /**
     * Decide whether the quad's per-vertex values should be rotated before being written
     * by {@code VertexConsumer.putBulkData}.
     *
     * @param brightness 4 AO brightness values in vertex order
     * @param lightmap   4 packed light values (block | sky) in vertex order
     * @return {@code true} = rotate; {@code false} = leave as-is
     */
    public static boolean shouldFlipQuad(final float[] brightness, final int[] lightmap) {
        if (brightness.length != 4 || lightmap.length != 4) {
            return false; // Probably not a regular face so don't bother
        }
        
        if (!allSame(brightness[0], brightness[1], brightness[2], brightness[3])) {
            return shouldFlipQuadInner(brightness[0], brightness[1], brightness[2], brightness[3]);
        }

        if (!allSame(lightmap[0], lightmap[1], lightmap[2], lightmap[3])) {
            return shouldFlipQuadInner(lightmap[0], lightmap[1], lightmap[2], lightmap[3]);
        }
        
        return false;
    }

    /**
     * GPU interpolation between vertex AO values causes gnarly visual artifacts in some cases
     * (https://bugs.mojang.com/browse/MC/issues/MC-138211)
     * 
     * This basic logic helps us determine if we should flip a quad's vertices around to prevent that
     */
    private static boolean shouldFlipQuadInner(final double v0, final double v1, final double v2, final double v3) {
        final double currentDiagonalDelta = Math.abs(v0 - v2);
        final double flippedDiagonalDelta = Math.abs(v1 - v3);

        if (currentDiagonalDelta != flippedDiagonalDelta) {
            return flippedDiagonalDelta < currentDiagonalDelta;
        } else {
            return Math.min(v1, v3) > Math.min(v0, v2);
        }
    }

    private static boolean allSame(final double a, final double b, final double c, final double d) {
        return a == b && b == c && c == d;
    }

    /**
     * @param inVertices   source {@code BakedQuad.getVertices()} (32 ints)
     * @param inBrightness source brightness per corner (4)
     * @param inLightmap   source lightmap per corner (4)
     * @param outVertices  destination (32 ints)
     * @param outBrightness destination (4)
     * @param outLightmap   destination (4)
     */
    public static void rotateQuad(
        final int[] inVertices,
        final float[] inBrightness,
        final int[] inLightmap,
        final int[] outVertices,
        final float[] outBrightness,
        final int[] outLightmap
    ) {
        outBrightness[0] = inBrightness[3]; outLightmap[0] = inLightmap[3];
        outBrightness[1] = inBrightness[0]; outLightmap[1] = inLightmap[0];
        outBrightness[2] = inBrightness[1]; outLightmap[2] = inLightmap[1];
        outBrightness[3] = inBrightness[2]; outLightmap[3] = inLightmap[2];
        if (inVertices.length == 32) {
            System.arraycopy(inVertices, 3 * 8, outVertices, 0 * 8, 8);
            System.arraycopy(inVertices, 0 * 8, outVertices, 1 * 8, 8);
            System.arraycopy(inVertices, 1 * 8, outVertices, 2 * 8, 8);
            System.arraycopy(inVertices, 2 * 8, outVertices, 3 * 8, 8);
        }
    }
}
