package rms.waterlogged_leaves;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public interface WaterloggedLeavesChangeListener {
    void waterloggedLeavesAdded(final BlockPos pos);

    void waterloggedLeavesRemoved(final BlockPos pos, final BlockState state);
}