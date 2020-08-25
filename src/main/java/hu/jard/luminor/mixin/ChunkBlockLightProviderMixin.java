package hu.jard.luminor.mixin;

import hu.jard.luminor.Luminor;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.BlockLightStorage;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin (ChunkBlockLightProvider.class)
public abstract class ChunkBlockLightProviderMixin extends ChunkLightProvider<BlockLightStorage.Data, BlockLightStorage> {
    private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();

    public ChunkBlockLightProviderMixin(ChunkProvider chunkProvider, LightType type, BlockLightStorage lightStorage) {
        super(chunkProvider, type, lightStorage);
    }

    @Inject(at = @At("HEAD"), method = "getPropagatedLevel(JJI)I", cancellable = true)
    protected void getPropagatedLevel(long sourceId, long targetId, int level, CallbackInfoReturnable<Integer> info) {
        info.cancel();

        if (targetId == Long.MAX_VALUE) {
            info.setReturnValue(Luminor.MAX_LIGHT_LEVEL);
        } else if (sourceId == Long.MAX_VALUE) {
            info.setReturnValue(level + Luminor.MAX_LIGHT_LEVEL - (this.getLightSourceLuminance(targetId) * Luminor.LIGHT_SUBDIVS));
        } else if (level >= Luminor.MAX_LIGHT_LEVEL) {
            info.setReturnValue(level);
        } else {
            int i = Integer.signum(BlockPos.unpackLongX(targetId) - BlockPos.unpackLongX(sourceId));
            int j = Integer.signum(BlockPos.unpackLongY(targetId) - BlockPos.unpackLongY(sourceId));
            int k = Integer.signum(BlockPos.unpackLongZ(targetId) - BlockPos.unpackLongZ(sourceId));

            Luminor.Direction direction = Luminor.Direction.from(i, j, k);

            if (direction == null) {
                info.setReturnValue(Luminor.MAX_LIGHT_LEVEL);
            } else {
                MutableInt mutableInt = new MutableInt();
                BlockState blockState = this.getStateForLighting(targetId, mutableInt);

                if (mutableInt.getValue() >= Luminor.MAX_LIGHT_LEVEL) {
                    info.setReturnValue(Luminor.MAX_LIGHT_LEVEL);
                } else {
                    info.setReturnValue(level + Math.max(direction.attenuation, mutableInt.getValue()));
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "propagateLevel(JIZ)V", cancellable = true)
    protected void propagateLevel(long id, int level, boolean decrease, CallbackInfo info) {
        info.cancel();

        long l = ChunkSectionPos.fromGlobalPos(id);

        int posX = BlockPos.unpackLongX(id);
        int posY = BlockPos.unpackLongY(id);
        int posZ = BlockPos.unpackLongZ(id);

        for (Luminor.Direction direction : Luminor.Direction.values()) {
            long m = BlockPos.asLong(posX + direction.x, posY + direction.y, posZ + direction.z);
            long n = ChunkSectionPos.fromGlobalPos(m);
            if (l == n || this.lightStorage.hasLight(n)) {
                this._propagateLevel(id, m, level, direction.attenuation, decrease);
            }
        }

    }

    @Inject(at = @At("HEAD"), method = "recalculateLevel(JJI)I", cancellable = true)
    protected void recalculateLevel(long id, long excludedId, int maxLevel, CallbackInfoReturnable<Integer> info) {
        info.cancel();

        int i = maxLevel;
        if (Long.MAX_VALUE != excludedId) {
            int j = this.getPropagatedLevel(Long.MAX_VALUE, id, 0);
            if (maxLevel > j) {
                i = j;
            }

            if (i == 0) {
                info.setReturnValue(i);
                return;
            }
        }

        long l = ChunkSectionPos.fromGlobalPos(id);
        ChunkNibbleArray chunkNibbleArray = this.lightStorage.getLightArray(l, true);

        int posX = BlockPos.unpackLongX(id);
        int posY = BlockPos.unpackLongY(id);
        int posZ = BlockPos.unpackLongZ(id);

        for (Luminor.Direction direction : Luminor.Direction.values()) {
            long m = BlockPos.asLong(posX + direction.x, posY + direction.y, posZ + direction.z);

            if (m != excludedId) {
                long n = ChunkSectionPos.fromGlobalPos(m);
                ChunkNibbleArray chunkNibbleArray3;
                if (l == n) {
                    chunkNibbleArray3 = chunkNibbleArray;
                } else {
                    chunkNibbleArray3 = this.lightStorage.getLightArray(n, true);
                }

                if (chunkNibbleArray3 != null) {
                    int k = this.getPropagatedLevel(m, id, this.getCurrentLevelFromArray(chunkNibbleArray3, m));
                    if (i > k) {
                        i = k;
                    }

                    if (i == 0) {
                        info.setReturnValue(i);
                        return;
                    }
                }
            }
        }

        info.setReturnValue(i);
    }







    protected final void _propagateLevel(long sourceId, long targetId, int level, int attenuation, boolean decrease) {
        int i = this.pendingUpdates.get(targetId) & 255;

        int j = MathHelper.clamp(this.getPropagatedLevel(sourceId, targetId, level), 0, this.levelCount - attenuation);
        if (decrease) {
            this.updateLevel(sourceId, targetId, j, this.getLevel(targetId), i, true);
        } else {
            int l;
            boolean bl2;
            if (i == 255) {
                bl2 = true;
                l = MathHelper.clamp(this.getLevel(targetId), 0, this.levelCount - attenuation);
            } else {
                l = i;
                bl2 = false;
            }

            if (j == l) {
                this.updateLevel(sourceId, targetId, this.levelCount - attenuation, bl2 ? l : this.getLevel(targetId), i, false);
            }
        }
    }

    @Shadow
    protected int getPropagatedLevel(long sourceId, long targetId, int level) {
        return 0;
    }

    @Shadow
    @Final
    protected int getLightSourceLuminance(long blockPos) {
        return 0;
    }
}
