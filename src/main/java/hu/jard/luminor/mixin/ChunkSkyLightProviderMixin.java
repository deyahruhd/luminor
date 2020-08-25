package hu.jard.luminor.mixin;

import hu.jard.luminor.Luminor;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import net.minecraft.world.chunk.light.SkyLightStorage;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin (ChunkSkyLightProvider.class)
public abstract class ChunkSkyLightProviderMixin extends ChunkLightProvider<SkyLightStorage.Data, SkyLightStorage> {
    public ChunkSkyLightProviderMixin(ChunkProvider chunkProvider, LightType type, SkyLightStorage lightStorage) {
        super(chunkProvider, type, lightStorage);
    }

    @Inject (at = @At ("HEAD"), method = "getPropagatedLevel(JJI)I", cancellable = true)
    protected void getPropagatedLevel(long sourceId, long targetId, int level, CallbackInfoReturnable <Integer> info) {
        info.cancel ();

        if (targetId == Long.MAX_VALUE) {
            info.setReturnValue (Luminor.MAX_LIGHT_LEVEL);
        } else {
            if (sourceId == Long.MAX_VALUE) {
                if (!((SkyLightStorage) this.lightStorage).method_15565 (targetId)) {
                    info.setReturnValue (Luminor.MAX_LIGHT_LEVEL);
                    return;
                }

                level = 0;
            }

            if (level >= Luminor.MAX_LIGHT_LEVEL) {
                info.setReturnValue (level);
            } else {
                MutableInt mutableInt = new MutableInt();
                BlockState blockState = this.getStateForLighting(targetId, mutableInt);
                if (mutableInt.getValue() >= Luminor.MAX_LIGHT_LEVEL) {
                    info.setReturnValue (Luminor.MAX_LIGHT_LEVEL);
                } else {
                    int i = BlockPos.unpackLongX(sourceId);
                    int j = BlockPos.unpackLongY(sourceId);
                    int k = BlockPos.unpackLongZ(sourceId);
                    int l = BlockPos.unpackLongX(targetId);
                    int m = BlockPos.unpackLongY(targetId);
                    int n = BlockPos.unpackLongZ(targetId);

                    boolean bl = i == l && k == n;
                    int o = Integer.signum(l - i);
                    int p = Integer.signum(m - j);
                    int q = Integer.signum(n - k);
                    Luminor.Direction direction;
                    if (sourceId == Long.MAX_VALUE) {
                        direction = Luminor.Direction.NEG_Y;
                    } else {
                        direction = Luminor.Direction.from (o, p, q);
                    }

                    BlockState blockState2 = this.getStateForLighting(sourceId, null);
                    VoxelShape voxelShape;
                    if (direction == null) {
                        voxelShape = this.getOpaqueShape(blockState2, sourceId, Direction.DOWN);
                        if (VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty())) {
                            info.setReturnValue (Luminor.MAX_LIGHT_LEVEL);
                            return;
                        }

                        int r = bl ? -1 : 0;
                        Direction direction3 = Direction.fromVector(o, r, q);
                        if (direction3 == null) {
                            info.setReturnValue (Luminor.MAX_LIGHT_LEVEL);
                            return;
                        }

                        VoxelShape voxelShape4 = this.getOpaqueShape(blockState, targetId, direction3.getOpposite());
                        if (VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape4)) {
                            info.setReturnValue (Luminor.MAX_LIGHT_LEVEL);
                        }
                    } else {
                        boolean bl2 = sourceId == Long.MAX_VALUE || bl && j > m;
                        info.setReturnValue (bl2 && level == 0 && mutableInt.getValue() == 0 ? 0 : level + Math.max(direction.attenuation, mutableInt.getValue()));
                    }
                }
            }
        }
    }

    @Inject (at = @At ("HEAD"), method = "propagateLevel(JIZ)V", cancellable = true)
    protected void propagateLevel(long id, int level, boolean decrease, CallbackInfo info) {
        info.cancel ();

        long l = ChunkSectionPos.fromGlobalPos(id);
        int i = BlockPos.unpackLongY(id);
        int j = ChunkSectionPos.getLocalCoord(i);
        int k = ChunkSectionPos.getSectionCoord(i);
        int o;
        if (j != 0) {
            o = 0;
        } else {
            int n;
            for(n = 0; !this.lightStorage.hasLight(ChunkSectionPos.offset(l, 0, -n - 1, 0)) && ((SkyLightStorage)this.lightStorage).isAboveMinHeight(k - n - 1); ++n) {
            }

            o = n;
        }

        long p = BlockPos.add(id, 0, -1 - o * 16, 0);
        long q = ChunkSectionPos.fromGlobalPos(p);
        if (l == q || this.lightStorage.hasLight(q)) {
            this._propagateLevel(id, p, level, Luminor.FACE_ATTENUATION, decrease);
        }

        long r = BlockPos.offset(id, Direction.UP);
        long s = ChunkSectionPos.fromGlobalPos(r);
        if (l == s || this.lightStorage.hasLight(s)) {
            this._propagateLevel(id, r, level, Luminor.FACE_ATTENUATION, decrease);
        }

        int posX = BlockPos.unpackLongX (id);
        int posY = BlockPos.unpackLongX (id);
        int posZ = BlockPos.unpackLongX (id);

        for(Luminor.Direction direction : Luminor.HORIZONTAL_DIRECTIONS) {
            int t = 0;

            while(true) {
                long u = BlockPos.asLong (posX + direction.x, posY + direction.y - t, posZ + direction.z);
                long v = ChunkSectionPos.fromGlobalPos(u);
                if (l == v) {
                    this._propagateLevel(id, u, level, direction.attenuation, decrease);
                    break;
                }

                if (this.lightStorage.hasLight (v)) {
                    this._propagateLevel(id, u, level, direction.attenuation, decrease);
                }

                ++t;
                if (t > o * 16) {
                    break;
                }
            }
        }

    }

    @Inject (at = @At ("HEAD"), method = "recalculateLevel(JJI)I", cancellable = true)
    protected void recalculateLevel(long id, long excludedId, int maxLevel, CallbackInfoReturnable <Integer> info) {
        info.cancel ();

        int i = maxLevel;
        if (Long.MAX_VALUE != excludedId) {
            int j = this.getPropagatedLevel(Long.MAX_VALUE, id, 0);
            if (maxLevel > j) {
                i = j;
            }

            if (i == 0) {
                info.setReturnValue (i);
                return;
            }
        }

        long l = ChunkSectionPos.fromGlobalPos(id);
        ChunkNibbleArray chunkNibbleArray = this.lightStorage.getLightArray(l, true);

        int posX = BlockPos.unpackLongX (id);
        int posY = BlockPos.unpackLongY (id);
        int posZ = BlockPos.unpackLongZ (id);

        for (Luminor.Direction direction : Luminor.Direction.values ()) {
            long m = BlockPos.asLong (posX + direction.x, posY + direction.y, posZ + direction.z);
            long n = ChunkSectionPos.fromGlobalPos(m);
            ChunkNibbleArray chunkNibbleArray3;
            if (l == n) {
                chunkNibbleArray3 = chunkNibbleArray;
            } else {
                chunkNibbleArray3 = this.lightStorage.getLightArray(n, true);
            }

            if (chunkNibbleArray3 != null) {
                if (m != excludedId) {
                    int k = this.getPropagatedLevel(m, id, this.getCurrentLevelFromArray(chunkNibbleArray3, m));
                    if (i > k) {
                        i = k;
                    }

                    if (i == 0) {
                        info.setReturnValue (i);
                        return;
                    }
                }
            } else if (direction != Luminor.Direction.NEG_Y) {
                for(m = BlockPos.removeChunkSectionLocalY (m);
                    !this.lightStorage.hasLight(n) && ! this.lightStorage.isAboveTopmostLightArray(n);
                    m = BlockPos.add(m, 0, 16, 0)) {
                    n = ChunkSectionPos.offset(n, Direction.UP);
                }

                ChunkNibbleArray chunkNibbleArray4 = this.lightStorage.getLightArray(n, true);
                if (m != excludedId) {
                    int p;
                    if (chunkNibbleArray4 != null) {
                        p = this.getPropagatedLevel(m, id, this.getCurrentLevelFromArray(chunkNibbleArray4, m));
                    } else {
                        p = this.lightStorage.isLightEnabled(n) ? 0 : Luminor.MAX_LIGHT_LEVEL;
                    }

                    if (i > p) {
                        i = p;
                    }

                    if (i == 0) {
                        info.setReturnValue (i);
                        return;
                    }
                }
            }
        }

        info.setReturnValue (i);
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
}
