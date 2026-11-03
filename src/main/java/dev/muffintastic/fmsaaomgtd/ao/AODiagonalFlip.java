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

    /**
     * In-place version of {@link #rotateQuad} for Sodium's render pipeline, where all the
     * vertex data of each quad lives in a single flat {@code int[]} with one contiguous block of
     * {@code perVertexStride} ints per vertex (position, color, UV, light, normal, etc.
     *
     * @param vertexData         the flat array holding the quad
     * @param firstVertexBlock   index of the first int of vertex 0
     * @param perVertexStride    ints per vertex
     * @param forwardTurns
     */
    public static void rotateVertexBlocksInPlace(final int[] vertexData, final int firstVertexBlock, final int perVertexStride, final int forwardTurns) {
        final int[] savedFirstCorner = new int[perVertexStride];
        for (int turn = 0; turn < forwardTurns; turn++) {
            // Save corner 0, then shift corners 3→0, 2→3, 1→2 (each copy reads a slot that hasn't
            // been overwritten yet), then restore the saved corner 0 into slot 1. Net effect:
            // old slot (m) lands in slot ((m + 1) % 4)
            System.arraycopy(vertexData, firstVertexBlock, savedFirstCorner, 0, perVertexStride);
            System.arraycopy(vertexData, firstVertexBlock + 3 * perVertexStride, vertexData, firstVertexBlock, perVertexStride);
            System.arraycopy(vertexData, firstVertexBlock + 2 * perVertexStride, vertexData, firstVertexBlock + 3 * perVertexStride, perVertexStride);
            System.arraycopy(vertexData, firstVertexBlock + perVertexStride, vertexData, firstVertexBlock + 2 * perVertexStride, perVertexStride);
            System.arraycopy(savedFirstCorner, 0, vertexData, firstVertexBlock + perVertexStride, perVertexStride);
        }
    }

    /**
     * In-place rotation for the 4 per-vertex values of a quad (e.g. {@code float[] brightnesses})
     */
    public static void rotateFourValuesInPlace(final float[] perVertex, final int forwardTurns) {
        for (int turn = 0; turn < forwardTurns; turn++) {
            final float savedFirst = perVertex[0];
            perVertex[0] = perVertex[3];
            perVertex[3] = perVertex[2];
            perVertex[2] = perVertex[1];
            perVertex[1] = savedFirst;
        }
    }
}
