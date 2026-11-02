package dev.muffintastic.fmsaaomgtd.ao;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Cache our patching class so it doesn't create a new instance every time because of @ModifyArg. Just a small optimization.
 */
public final class AOWrapperCache {
    private static final Map<VertexConsumer, FixedAOVertexConsumer> CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private AOWrapperCache() {}

    public static VertexConsumer wrap(final VertexConsumer consumer) {
        if (consumer instanceof FixedAOVertexConsumer) {
            return consumer;
        }
        final FixedAOVertexConsumer existing = CACHE.get(consumer);
        if (existing != null) {
            return existing;
        }
        final FixedAOVertexConsumer created = new FixedAOVertexConsumer(consumer);
        final FixedAOVertexConsumer conflicting = CACHE.putIfAbsent(consumer, created);
        return conflicting != null ? conflicting : created;
    }
}
