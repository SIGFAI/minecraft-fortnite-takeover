package sigf.mod;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** What the loot does: the pump shotgun, the shield potion and instant building. */
public final class Gear {
	private Gear() {}

	private static final DustParticleOptions BLOOD = new DustParticleOptions(0xE02030, 0.7f);

	/** Pump shotgun: 8 pellets in a cone, each one hurts what it hits and shows it. */
	public static int fireShotgun(LivingEntity shooter) {
		ServerLevel lvl = Sigf.level();
		Vec3 eye = shooter.getEyePosition();
		Vec3 look = shooter.getLookAngle();
		Vec3 muzzle = eye.add(look.scale(1.2)).add(0, -0.15, 0);
		Sigf.sound(SigfMod.SHOTGUN_SOUND, shooter.position(), 1.2f, 1f + lvl.getRandom().nextFloat() * 0.1f);
		Sigf.particles(ParticleTypes.FLAME, muzzle.add(look.scale(0.6)), 3, 0.05);
		Sigf.particles(ParticleTypes.SMOKE, muzzle.add(look.scale(0.6)), 4, 0.08);
		
		int hits = 0;
		java.util.Map<LivingEntity, Integer> landed = new java.util.LinkedHashMap<>();
		for (int i = 0; i < 8; i++) {
			Vec3 dir = look.add((lvl.getRandom().nextDouble() - 0.5) * 0.14, (lvl.getRandom().nextDouble() - 0.5) * 0.10, (lvl.getRandom().nextDouble() - 0.5) * 0.14).normalize();
			Vec3 end = eye.add(dir.scale(26));
			var blockHit = lvl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
			if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
			Entity best = null; double bestD = eye.distanceTo(end); Vec3 bestAt = null;
			for (Entity e : lvl.getEntities(shooter, new AABB(eye, end).inflate(1.0), x -> x.isAlive() && x instanceof LivingEntity && !x.isSpectator())) {
				Optional<Vec3> at = e.getBoundingBox().inflate(0.2).clip(eye, end);
				if (at.isPresent()) {
					double d = eye.distanceTo(at.get());
					if (d < bestD) { bestD = d; best = e; bestAt = at.get(); }
				}
			}
			Vec3 stop = best != null ? bestAt : end;
			double len = eye.distanceTo(stop);
			for (double t = 1.5; t < len; t += 1.6) lvl.sendParticles(ParticleTypes.CRIT, eye.x + dir.x * t, eye.y + dir.y * t, eye.z + dir.z * t, 1, 0, 0, 0, 0);
			if (best instanceof LivingEntity target) {
				hits++;
				landed.merge(target, 1, Integer::sum);
				lvl.sendParticles(BLOOD, stop.x, stop.y, stop.z, 5, 0.15, 0.15, 0.15, 0.1);
				lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, stop.x, stop.y, stop.z, 4, 0.1, 0.1, 0.1, 0.2);
			} else if (blockHit.getType() == HitResult.Type.BLOCK) {
				BlockState bs = lvl.getBlockState(blockHit.getBlockPos());
				lvl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, bs), stop.x, stop.y, stop.z, 6, 0.1, 0.1, 0.1, 0.1);
			}
		}
		// one combined hit per target: the damage-immunity frames would swallow the other pellets
		for (var en : landed.entrySet()) {
			LivingEntity target = en.getKey();
			float dmg = 2.4f * en.getValue();
			var src = shooter instanceof ServerPlayer sp ? lvl.damageSources().playerAttack(sp) : lvl.damageSources().mobAttack(shooter);
			target.setInvulnerableTime(0);
			target.hurtServer(lvl, src, dmg);
			target.setDeltaMovement(target.getDeltaMovement().add(look.x * 0.35, 0.12, look.z * 0.35));
			target.syncVelocity = true;
		}
		if (hits > 0) Sigf.sound(SoundEvents.NOTE_BLOCK_PLING.value(), shooter.position(), 1.2f, 2f);
		return hits;
	}

	/** Shield potion: +10 absorption hearts-worth and a blue burst. */
	public static void drinkShield(LivingEntity who) {
		who.setAbsorptionAmount(Math.min(20f, who.getAbsorptionAmount() + 10f));
		Sigf.sound(SoundEvents.BREWING_STAND_BREW, who.position(), 1f, 1.4f);
		Sigf.sound(SoundEvents.BEACON_ACTIVATE, who.position(), 0.8f, 1.6f);
		Sigf.particles(new DustParticleOptions(0x3AA8FF, 0.8f), who.position().add(0, 1, 0), 40, 0.5);
		Sigf.particles(ParticleTypes.GLOW, who.position().add(0, 1, 0), 15, 0.6);
	}

	/** Instant build: a 5x3 wall appearing piece by piece in front of the player, or a ramp when sneaking. */
	public static void build(ServerPlayer p, boolean ramp) {
		Direction f = Direction.fromYRot(p.getYRot());
		Direction side = f.getClockWise();
		BlockPos base = p.blockPosition();
		Sigf.sound(SigfMod.BUILD_SOUND, p.position(), 1f, 1f);
		if (!ramp) {
			for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++) {
				BlockPos pos = base.relative(f, 3).relative(side, w).above(h);
				double delay = (Math.abs(w) * 0.07) + h * 0.07;
				Sigf.after(delay + 0.01, () -> place(pos, Blocks.OAK_PLANKS.defaultBlockState()));
			}
		} else {
			for (int s = 0; s < 4; s++) for (int w = -1; w <= 1; w++) {
				BlockPos pos = base.relative(f, 2 + s).relative(side, w).above(s);
				BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, f);
				Sigf.after(s * 0.12 + 0.01, () -> {
					for (int d = 0; d < pos.getY() - base.getY(); d++) place(pos.below(d + 1), Blocks.OAK_PLANKS.defaultBlockState());
					place(pos, stairs);
				});
			}
		}
	}

	private static void place(BlockPos pos, BlockState st) {
		ServerLevel lvl = Sigf.level();
		if (!lvl.getBlockState(pos).canBeReplaced()) return;
		lvl.setBlockAndUpdate(pos, st);
		Vec3 c = Vec3.atCenterOf(pos);
		lvl.sendParticles(new DustParticleOptions(0x4FC3FF, 0.6f), c.x, c.y, c.z, 3, 0.4, 0.4, 0.4, 0);
		lvl.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0.01);
		Sigf.sound(SoundEvents.WOOD_PLACE, c, 0.7f, 0.9f + lvl.getRandom().nextFloat() * 0.3f);
	}
}
