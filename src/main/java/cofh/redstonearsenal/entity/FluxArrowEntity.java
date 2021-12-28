package cofh.redstonearsenal.entity;

import cofh.redstonearsenal.item.IFluxItem;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Predicate;

import static cofh.redstonearsenal.init.RSAReferences.FLUX_ARROW_ENTITY;

public class FluxArrowEntity extends AbstractArrowEntity {

    protected static final DataParameter<Byte> RSA_FLAGS = EntityDataManager.defineId(FluxArrowEntity.class, DataSerializers.BYTE);
    protected static final int LIFESPAN = 200;
    protected static final float EXPLOSION_RANGE = 4.0F;

    public FluxArrowEntity(EntityType<? extends FluxArrowEntity> entityIn, World worldIn) {

        super(entityIn, worldIn);
    }

    public FluxArrowEntity(World worldIn, LivingEntity shooter) {

        super(FLUX_ARROW_ENTITY, shooter, worldIn);
    }

    public FluxArrowEntity(World worldIn, double x, double y, double z) {

        super(FLUX_ARROW_ENTITY, x, y, z, worldIn);
    }

    @Override
    protected ItemStack getPickupItem() {

        return ItemStack.EMPTY;
    }

    public void setExplodeArrow(boolean explode) {

        setRSAFlag(1, explode);
    }

    public boolean isExplodeArrow() {

        return (this.entityData.get(RSA_FLAGS) & 1) != 0;
    }

    @Override
    protected void defineSynchedData() {

        super.defineSynchedData();
        this.entityData.define(RSA_FLAGS, (byte) 0);
    }

    private void setRSAFlag(int flag, boolean value) {

        byte b0 = this.entityData.get(RSA_FLAGS);
        if (value) {
            this.entityData.set(RSA_FLAGS, (byte) (b0 | flag));
        } else {
            this.entityData.set(RSA_FLAGS, (byte) (b0 & ~flag));
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {

        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public DamageSource getDamageSource(AbstractArrowEntity arrow, @Nullable Entity shooter) {

        return IFluxItem.fluxRangedDamage(arrow, shooter == null ? arrow : shooter);
    }

    public void explode(Vector3d pos) {

        if (!level.isClientSide()) {
            //level.addParticle(ParticleTypes.EXPLOSION, pos.x(), pos.y(), pos.z(), 0, 0, 0);
            //level.playLocalSound(pos.x(), pos.y(), pos.z(), SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
            ((ServerWorld) level).sendParticles(ParticleTypes.EXPLOSION, pos.x(), pos.y(), pos.z(), 1, 0, 0, 0, 0);
            level.playSound(null, pos.x(), pos.y(), pos.z(), SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F);
            double r2 = EXPLOSION_RANGE * EXPLOSION_RANGE;
            AxisAlignedBB searchArea = this.getBoundingBox().move(pos.subtract(this.position())).inflate(EXPLOSION_RANGE, 1, EXPLOSION_RANGE);
            Predicate<Entity> filter = EntityPredicates.NO_CREATIVE_OR_SPECTATOR.and(entity -> entity instanceof LivingEntity);
            for (Entity target : level.getEntities(this, searchArea, filter)) {
                if (pos.distanceToSqr(target.position()) < r2) {
                    target.hurt(getDamageSource(this, getOwner()), (float) getBaseDamage());
                }
            }
            level.broadcastEntityEvent(this, (byte) 3);
        }
        remove();
    }

    @Override
    public void tick() {

        if (!level.isClientSide() && tickCount > LIFESPAN) {
            level.broadcastEntityEvent(this, (byte) 3);
            remove();
        } else {
            super.tick();
        }
    }

    @Override
    protected float getWaterInertia() {

        return 0.99F;
    }

    //@Override
    //protected void onHit(RayTraceResult result) {
    //
    //    RayTraceResult.Type type = result.getType();
    //    if (type == RayTraceResult.Type.ENTITY) {
    //            this.onHitEntity((EntityRayTraceResult) result);
    //    } else if (type == RayTraceResult.Type.BLOCK) {
    //        this.onHitBlock((BlockRayTraceResult) result);
    //    }
    //}

    @Override
    protected void onHitEntity(EntityRayTraceResult result) {

        Entity target = result.getEntity();
        float speed = (float) this.getDeltaMovement().length();
        int dmg = MathHelper.ceil(MathHelper.clamp(speed * this.baseDamage, 0.0D, 2.147483647E9D));
        if (this.getPierceLevel() > 0) {
            if (this.piercingIgnoreEntityIds == null) {
                this.piercingIgnoreEntityIds = new IntOpenHashSet(5);
            }

            if (this.piercedAndKilledEntities == null) {
                this.piercedAndKilledEntities = Lists.newArrayListWithCapacity(5);
            }

            if (this.piercingIgnoreEntityIds.size() >= this.getPierceLevel() + 1) {
                this.remove();
                return;
            }

            this.piercingIgnoreEntityIds.add(target.getId());
        }

        if (this.isCritArrow()) {
            dmg = Math.min(this.random.nextInt(dmg / 2 + 2) + dmg, Integer.MAX_VALUE);
        }

        Entity owner = this.getOwner();
        DamageSource dmgSource = getDamageSource(this, owner);
        if (owner instanceof LivingEntity) {
            ((LivingEntity) owner).setLastHurtMob(target);
        }

        boolean canHurt = canHurtEntity(target);
        if (target.hurt(dmgSource, (float) dmg)) {
            if (!canHurt) {
                return;
            }

            if (target instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) target;

                if (this.knockback > 0) {
                    Vector3d vector3d = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double) this.knockback * 0.6D);
                    if (vector3d.lengthSqr() > 0.0D) {
                        living.push(vector3d.x, 0.1D, vector3d.z);
                    }
                }

                if (!this.level.isClientSide && owner instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(living, owner);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) owner, living);
                }

                this.doPostHurtEffects(living);
                if (living != owner && living instanceof PlayerEntity && owner instanceof ServerPlayerEntity && !this.isSilent()) {
                    ((ServerPlayerEntity) owner).connection.send(new SChangeGameStatePacket(SChangeGameStatePacket.ARROW_HIT_PLAYER, 0.0F));
                }

                if (!target.isAlive() && this.piercedAndKilledEntities != null) {
                    this.piercedAndKilledEntities.add(living);
                }

                if (!this.level.isClientSide && owner instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) owner;
                    if (this.piercedAndKilledEntities != null && this.shotFromCrossbow()) {
                        CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayerentity, this.piercedAndKilledEntities);
                    } else if (!target.isAlive() && this.shotFromCrossbow()) {
                        CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayerentity, Arrays.asList(target));
                    }
                }
            }

            this.playSound(getDefaultHitGroundSoundEvent(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));

            if (isExplodeArrow()) {
                this.explode(this.position());
                return;
            }
            if (this.getPierceLevel() <= 0) {
                this.remove();
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
            this.yRot += 180.0F;
            this.yRotO += 180.0F;
            if (!this.level.isClientSide && this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                this.remove();
            }
        }
    }

    protected boolean canHurtEntity(Entity entity) {

        return entity.getType() != EntityType.ENDERMAN;
    }

    @Override
    protected void onHitBlock(BlockRayTraceResult result) {

        if (isExplodeArrow()) {
            this.explode(result.getLocation());
            return;
        }
        level.broadcastEntityEvent(this, (byte) 3);
        this.remove();
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {

        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("explode", this.isExplodeArrow());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {

        super.readAdditionalSaveData(nbt);
        this.setExplodeArrow(nbt.getBoolean("explode"));
    }

}
