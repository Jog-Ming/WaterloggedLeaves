package rms;

import it.unimi.dsi.fastutil.ints.IntSet;

public interface WaterloggedLeavesTracker {
    IntSet getWaterloggedLeaves();

    void setWaterloggedLeavesChangeListener(final WaterloggedLeavesChangeListener waterloggedLeavesChangeListener);
}