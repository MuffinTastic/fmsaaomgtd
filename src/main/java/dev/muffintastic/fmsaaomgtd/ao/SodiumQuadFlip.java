package dev.muffintastic.fmsaaomgtd.ao;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * Hacky hack for Sodium.
 * 
 * Because Sodium bypasses the vanilla mesh generation ({@code VertexConsumer}) entirely, {@link FixedAOVertexConsumer} has no effect.
 * 
 * So, this class applies the same quad flipping logic as the ({@link AODiagonalFlip#shouldFlipQuad}) and rotates the quads in a similar
 * way before Sodium submits them to a buffer.
 * 
 * And because we need access to some private stuff, we use a bunch of reflection jank. Ew.
 */
public final class SodiumQuadFlip {

    // Known locations of EncodingFormat + QuadViewImpl, newest first
    private static final String[] SODIUM_QUAD_PACKAGES = {
        "net.caffeinemc.mods.sodium.client.render.frapi.mesh", // sodium 0.8.x (MC 1.21.1)
        "net.caffeinemc.mods.sodium.client.render.model"        // older dev layout
    };

    private static volatile boolean resolved = false;
    private static volatile boolean usable = false;

    private static Field DATA_FIELD;      // QuadViewImpl#data      (protected int[])
    private static Field BASE_FIELD;      // QuadViewImpl#baseIndex  (protected int)
    private static int VERTEX_STRIDE;     // EncodingFormat.VERTEX_STRIDE   (public)
    private static int VERTEX_BASE_OFFSET;// EncodingFormat.HEADER_STRIDE   (public)
    private static int NORMALS_BIT_OFFSET;
    private static int NORMALS_BIT_MASK;

    private SodiumQuadFlip() {}

    public static boolean isUsable() {
        ensureResolved();
        return usable;
    }

    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        synchronized (SodiumQuadFlip.class) {
            if (resolved) {
                return;
            }
            resolved = true;
            for (final String sodiumQuadPackage : SODIUM_QUAD_PACKAGES) {
                if (tryResolve(sodiumQuadPackage)) {
                    usable = true;
                    return;
                }
            }
            usable = false;
        }
    }

    private static boolean tryResolve(final String sodiumQuadPackage) {
        try {
            final Class<?> encodingFormat = Class.forName(sodiumQuadPackage + ".EncodingFormat");
            final Class<?> quadViewImpl = Class.forName(sodiumQuadPackage + ".QuadViewImpl");

                DATA_FIELD = quadViewImpl.getDeclaredField("data");
                DATA_FIELD.setAccessible(true);
                BASE_FIELD = quadViewImpl.getDeclaredField("baseIndex");
                BASE_FIELD.setAccessible(true);

                VERTEX_STRIDE = encodingFormat.getField("VERTEX_STRIDE").getInt(null);
                VERTEX_BASE_OFFSET = encodingFormat.getField("HEADER_STRIDE").getInt(null);
                if (VERTEX_STRIDE <= 0 || VERTEX_BASE_OFFSET < 0) {
                    throw new IllegalStateException("unexpected vertex layout: stride=" + VERTEX_STRIDE + ", baseOffset=" + VERTEX_BASE_OFFSET);
                }

                // Use Sodium's own static EncodingFormat#normalFlags(int bits, int flags) like a freaking puppeteer
                final MethodHandle flagsIntoBits = MethodHandles
                    .privateLookupIn(encodingFormat, MethodHandles.lookup())
                    .findStatic(encodingFormat, "normalFlags", MethodType.methodType(int.class, int.class, int.class));

                final int flagOneEncoded = (int) flagsIntoBits.invoke(0, 1);
                NORMALS_BIT_OFFSET = Integer.numberOfTrailingZeros(flagOneEncoded);

                if (flagOneEncoded == 0 || NORMALS_BIT_OFFSET < 0 || NORMALS_BIT_OFFSET > 16) {
                    throw new IllegalStateException("cannot locate normal-flags bits, encoded 0x" + Integer.toHexString(flagOneEncoded));
                }

                NORMALS_BIT_MASK = 0xF << NORMALS_BIT_OFFSET;
                return true;
            } catch (final Throwable failure) {
                return false;
            }
    }

    /**
     * Do that shit
     *
     * @return true when a rotation was actually applied
     */
    public static boolean applyRotation(final Object quadView, final float[] brightnessesPerVertex) {
        ensureResolved();
        if (!usable || brightnessesPerVertex == null || brightnessesPerVertex.length != 4) {
            return false;
        }

        final int[] data = dataOf(quadView);
        final int base = baseIndexOf(quadView);

        if (data == null || data.length < base + VERTEX_BASE_OFFSET + 4 * VERTEX_STRIDE) {
            return false;
        }

        final boolean shouldFlip = AODiagonalFlip.shouldFlipQuad(brightnessesPerVertex, null);

        if (!shouldFlip) {
            return false;
        }

        AODiagonalFlip.rotateVertexBlocksInPlace(data, base + VERTEX_BASE_OFFSET, VERTEX_STRIDE, 1);
        AODiagonalFlip.rotateFourValuesInPlace(brightnessesPerVertex, 1);
        rotateNormalFlagBits(data, base, true);

        return true;
    }

    public static void undoRotation(final Object quadView, final float[] brightnessesPerVertex) {
        ensureResolved();

        if (!usable || brightnessesPerVertex == null || brightnessesPerVertex.length != 4) {
            return;
        }

        final int[] data = dataOf(quadView);
        final int base = baseIndexOf(quadView);

        if (data == null || data.length < base + VERTEX_BASE_OFFSET + 4 * VERTEX_STRIDE) {
            return;
        }

        AODiagonalFlip.rotateVertexBlocksInPlace(data, base + VERTEX_BASE_OFFSET, VERTEX_STRIDE, 3);
        AODiagonalFlip.rotateFourValuesInPlace(brightnessesPerVertex, 3);

        rotateNormalFlagBits(data, base, false);
    }

    private static int[] dataOf(final Object quadView) {
        try {
            return (int[]) DATA_FIELD.get(quadView);
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }

    private static int baseIndexOf(final Object quadView) {
        try {
            return BASE_FIELD.getInt(quadView);
        } catch (ReflectiveOperationException failure) {
            return -1;
        }
    }

    private static void rotateNormalFlagBits(final int[] data, final int base, final boolean forward) {
        final int headerBits = data[base];
        final int flags = (headerBits & NORMALS_BIT_MASK) >>> NORMALS_BIT_OFFSET;
        final int rotated = forward ? ((flags & 0b0111) << 1) | ((flags >>> 3) & 0b1)
                                    : ((flags & 0b1110) >>> 1) | ((flags & 0b1) << 3);
        data[base] = (headerBits & ~NORMALS_BIT_MASK) | (rotated << NORMALS_BIT_OFFSET);
    }
}
