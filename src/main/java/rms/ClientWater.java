package rms;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.Set;

public final class ClientWater {
    private static final BlockStateParticleEffect WATER_PARTICLE_EFFECT = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.WATER.getDefaultState());
    private static final int WATER_ID = Block.getRawIdFromState(Blocks.BLUE_STAINED_GLASS.getDefaultState());
    private static final DataTracker DATA_TRACKER = new DataTracker(null);

    static {
        DATA_TRACKER.startTracking(Entity.NO_GRAVITY, true);
    }

    private final int id = Entity.CURRENT_ID.incrementAndGet();
    private final double x;
    private final double y;
    private final double z;
    private final Set<EntityTrackingListener> listeners = Sets.newIdentityHashSet();
    private ParticleS2CPacket addWaterParticlePacket;
    private EntitySpawnS2CPacket entitySpawnPacket;
    private EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket;
    private EntitiesDestroyS2CPacket entityDestroyPacket;

    public ClientWater(final BlockPos pos) {
        this.x = pos.getX() + 0.5;
        this.y = pos.getY();
        this.z = pos.getZ() + 0.5;
    }

    public void updatePlayer(final ServerPlayerEntity player, final int watchDistance) {
        double dx = player.getX() - this.x;
        double dz = player.getZ() - this.z;
        int d = (watchDistance - 1) << 4;
        if (dx >= -d && dx <= d && dz >= -d && dz <= d) {
            if (this.listeners.add(player.networkHandler)) {
                if (this.addWaterParticlePacket == null) {
                    this.addWaterParticlePacket = new ParticleS2CPacket(WATER_PARTICLE_EFFECT, true, this.x, this.y + 0.5, this.z, 0.4f, 0.4f, 0.4f, 0, 64);
                }
                player.networkHandler.sendPacket(this.addWaterParticlePacket);
                if (this.entitySpawnPacket == null) {
                    this.entitySpawnPacket = new EntitySpawnS2CPacket(this.id, MathHelper.randomUuid(new Random()), this.x, this.y, this.z, 0, 0, EntityType.FALLING_BLOCK, WATER_ID, Vec3d.ZERO);
                }
                player.networkHandler.sendPacket(this.entitySpawnPacket);
                if (this.entityTrackerUpdatePacket == null) {
                    this.entityTrackerUpdatePacket = new EntityTrackerUpdateS2CPacket(this.id, DATA_TRACKER, true);
                }
                player.networkHandler.sendPacket(this.entityTrackerUpdatePacket);
            }
            return;
        }
        removePlayerAndSendPackets(player, null);
    }

    public int removePlayerAndGetId(final ServerPlayerEntity player) {
        if (this.listeners.remove(player.networkHandler)) {
            return this.id;
        }
        return -1;
    }

    public void removePlayerAndSendPackets(final ServerPlayerEntity player, final BlockState state) {
        if (this.listeners.remove(player.networkHandler)) {
            if (state != null) {
                player.networkHandler.sendPacket(new ParticleS2CPacket(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), true, this.x, this.y + 0.5, this.z, 0.4f, 0.4f, 0.4f, 0, 64));
            }
            if (this.entityDestroyPacket == null) {
                this.entityDestroyPacket = new EntitiesDestroyS2CPacket(this.id);
            }
            player.networkHandler.sendPacket(this.entityDestroyPacket);
        }
    }
}