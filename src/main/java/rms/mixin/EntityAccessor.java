package rms.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("NO_GRAVITY")
    static TrackedData<Boolean> getNoGravity() {
        throw new IllegalStateException();
    }

    @Accessor("CURRENT_ID")
    static AtomicInteger getCurrentId() {
        throw new IllegalStateException();
    }
}