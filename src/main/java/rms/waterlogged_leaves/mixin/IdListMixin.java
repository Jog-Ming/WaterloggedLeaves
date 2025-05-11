package rms.waterlogged_leaves.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.IdList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IdList.class)
public abstract class IdListMixin {
    @Shadow
    public abstract int getRawId(Object object);

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void add(Object value, CallbackInfo ci) {
        if (value instanceof BlockState state && state.getBlock() instanceof LeavesBlock && state.get(Properties.WATERLOGGED)) {
            ci.cancel();
        }
    }

    @Inject(method = "getRawId", at = @At("HEAD"), cancellable = true)
    private void getRawId(Object entry, CallbackInfoReturnable<Integer> cir) {
        if (entry instanceof BlockState state && state.getBlock() instanceof LeavesBlock && state.get(Properties.WATERLOGGED)) {
            cir.setReturnValue(this.getRawId(state.with(Properties.WATERLOGGED, false)));
        }
    }
}