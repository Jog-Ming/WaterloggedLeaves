package rms.waterlogged_leaves.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rms.waterlogged_leaves.ClientWater;
import rms.waterlogged_leaves.WaterloggedLeavesChangeListener;
import rms.waterlogged_leaves.WaterloggedLeavesTracker;

import java.util.stream.Stream;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin implements WaterloggedLeavesChangeListener {
    @Unique
    private final Object2ObjectOpenHashMap<BlockPos, ClientWater> clientWaterMap = new Object2ObjectOpenHashMap<>();
    @Shadow
    int watchDistance;

    @Shadow
    public abstract Stream<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean bl);

    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void waterloggedLeavesAdded(final BlockPos pos) {
        if (!this.clientWaterMap.containsKey(pos)) {
            ClientWater clientWater = new ClientWater(pos);
            this.clientWaterMap.put(pos, clientWater);
            this.getPlayersWatchingChunk(new ChunkPos(pos), false).forEach(player -> clientWater.updatePlayer(player, this.watchDistance));
        }
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void waterloggedLeavesRemoved(final BlockPos pos, final BlockState state) {
        ClientWater clientWater = this.clientWaterMap.remove(pos);
        if (clientWater != null) {
            this.getPlayersWatchingChunk(new ChunkPos(pos), false).forEach(player -> clientWater.removePlayerAndSendPackets(player, state));
        }
    }

    @Inject(method = "updatePosition", at = @At("HEAD"))
    private void updatePosition(ServerPlayerEntity player, CallbackInfo ci) {
        for (ClientWater clientWater : this.clientWaterMap.values()) {
            clientWater.updatePlayer(player, this.watchDistance);
        }
    }

    @Inject(method = "unloadEntity", at = @At("HEAD"))
    private void unloadEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity player) {
            IntArrayList clientWaterIds = new IntArrayList(this.clientWaterMap.size());
            for (ClientWater clientWater : this.clientWaterMap.values()) {
                int id = clientWater.removePlayerAndGetId(player);
                if (id != -1) {
                    clientWaterIds.add(id);
                }
            }
            player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(clientWaterIds));
        }
    }

    @Inject(method = "sendChunkDataPackets", at = @At("HEAD"))
    private void sendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo ci) {
        WaterloggedLeavesTracker waterloggedLeavesTracker = (WaterloggedLeavesTracker) chunk;
        waterloggedLeavesTracker.setWaterloggedLeavesChangeListener(this);
        for (int i : waterloggedLeavesTracker.getWaterloggedLeaves()) {
            BlockPos pos = chunk.getPos().getBlockPos(i >> 12, (i >> 4) & 255, i & 15);
            if (this.clientWaterMap.containsKey(pos)) {
                continue;
            }
            ClientWater clientWater = new ClientWater(pos);
            this.clientWaterMap.put(pos, clientWater);
            clientWater.updatePlayer(player, this.watchDistance);
        }
    }
}
