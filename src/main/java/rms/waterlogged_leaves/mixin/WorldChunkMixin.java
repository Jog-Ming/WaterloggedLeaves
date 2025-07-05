package rms.waterlogged_leaves.mixin;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.TickScheduler;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rms.waterlogged_leaves.WaterloggedLeavesChangeListener;
import rms.waterlogged_leaves.WaterloggedLeavesTracker;

import java.util.function.Consumer;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements WaterloggedLeavesTracker {
    @Unique
    private final IntSet waterloggedLeaves = new IntArraySet();
    @Unique
    private WaterloggedLeavesChangeListener waterloggedLeavesChangeListener;
    @Shadow
    @Final
    private ChunkSection[] sections;

    @SuppressWarnings("AddedMixinMembersNamePattern")
    public IntSet getWaterloggedLeaves() {
        return this.waterloggedLeaves;
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void setWaterloggedLeavesChangeListener(final WaterloggedLeavesChangeListener waterloggedLeavesChangeListener) {
        System.out.println("setWaterloggedLeavesChangeListener");
        this.waterloggedLeavesChangeListener = waterloggedLeavesChangeListener;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/biome/source/BiomeArray;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/TickScheduler;Lnet/minecraft/world/TickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    private void init(World world, ChunkPos pos, BiomeArray biomes, UpgradeData upgradeData, TickScheduler<Block> blockTickScheduler, TickScheduler<Fluid> fluidTickScheduler, long inhabitedTime, ChunkSection[] sections, Consumer<WorldChunk> loadToWorldConsumer, CallbackInfo ci) {
        for (byte i = 0; i < 16; ++i) {
            ChunkSection section = this.sections[i];
            if (section == null) {
                continue;
            }
            for (byte x = 0; x < 16; ++x) {
                for (byte y = 0; y < 16; ++y) {
                    for (byte z = 0; z < 16; ++z) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.getBlock() instanceof LeavesBlock && state.get(Properties.WATERLOGGED)) {
                            this.waterloggedLeaves.add(x << 12 | i << 8 | y << 4 | z);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void setBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        int i = (pos.getX() & 15) << 12 | pos.getY() << 4 | pos.getZ() & 15;
        if (state.getBlock() instanceof LeavesBlock && state.get(Properties.WATERLOGGED)) {
            this.waterloggedLeaves.add(i);
            if (this.waterloggedLeavesChangeListener != null) {
                this.waterloggedLeavesChangeListener.waterloggedLeavesAdded(pos);
            }
            return;
        }
        BlockState previousState = cir.getReturnValue();
        if (previousState != null && previousState.getBlock() instanceof LeavesBlock && previousState.get(Properties.WATERLOGGED) && !(state.getBlock() instanceof LeavesBlock && state.get(Properties.WATERLOGGED))) {
            this.waterloggedLeaves.remove(i);
            if (this.waterloggedLeavesChangeListener != null) {
                this.waterloggedLeavesChangeListener.waterloggedLeavesRemoved(pos, previousState);
            }
        }
    }
}
