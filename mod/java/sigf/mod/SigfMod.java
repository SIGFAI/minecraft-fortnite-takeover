package sigf.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

public final class SigfMod implements ModInitializer {
	public static Item SHOTGUN, SHIELD, BUILD;
	public static SoundEvent SHOTGUN_SOUND, LOOT_SOUND, BUILD_SOUND, VICTORY_SOUND, STORM_SOUND;

	@Override
	public void onInitialize() {
		SHOTGUN = Sigf.item("shotgun", p -> new Item(p.stacksTo(1)));
		SHIELD = Sigf.item("shield", p -> new Item(p.stacksTo(8)));
		BUILD = Sigf.item("build", p -> new Item(p.stacksTo(1)));
		SHOTGUN_SOUND = Sigf.registerSound("shotgun_shot");
		LOOT_SOUND = Sigf.registerSound("loot_pop");
		BUILD_SOUND = Sigf.registerSound("build_up");
		VICTORY_SOUND = Sigf.registerSound("victory");
		STORM_SOUND = Sigf.registerSound("storm");

		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack st = player.getItemInHand(hand);
			if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
			if (st.is(SHOTGUN)) {
				if (sp.getCooldowns().isOnCooldown(st)) return InteractionResult.FAIL;
				Gear.fireShotgun(sp);
				sp.getCooldowns().addCooldown(st, 14);
				return InteractionResult.SUCCESS;
			}
			if (st.is(SHIELD)) {
				Gear.drinkShield(sp);
				if (!sp.getAbilities().instabuild) st.shrink(1);
				return InteractionResult.SUCCESS;
			}
			if (st.is(BUILD)) {
				if (sp.getCooldowns().isOnCooldown(st)) return InteractionResult.FAIL;
				Gear.build(sp, sp.isShiftKeyDown());
				sp.getCooldowns().addCooldown(st, 20);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> Match.onDeath(entity, source));
		ServerTickEvents.END_SERVER_TICK.register(server -> Match.tick());
		Match.init();

		// Outside the demo a round starts by itself; the first bus is in the sky within seconds.
		if (!Sigf.isDemo()) {
			Sigf.after(2, () -> Match.startRound(false, 8));
			Sigf.after(30, () -> { if (Match.llamas.isEmpty()) Match.spawnLlama(Sigf.ground(10)); });
			Sigf.every(45, () -> { if (Match.llamas.isEmpty()) Match.spawnLlama(Sigf.ground(10)); });
		}

		demo();
	}

	static ServerPlayer h() { return Sigf.host(); }

	/** Nearest living bot to the stream player. */
	static Mob nearestBot() {
		Mob best = null; double bd = 1e9;
		for (Mob m : Match.bots) if (m.isAlive()) {
			double d = m.distanceTo(h());
			if (d < bd) { bd = d; best = m; }
		}
		return best;
	}

	static void aimAndShoot() {
		Mob m = nearestBot();
		if (m == null || h() == null || m.distanceTo(h()) > 24) return;
		Sigf.lookAt(h(), m.position().add(0, m.getBbHeight() * 0.6, 0));
		Gear.fireShotgun(h());
	}

	static void demo() {
		Sigf.demo(0.1, () -> {
			Sigf.command("clear @a");
			Vec3 hp = h().position();
			int gy = Sigf.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(hp.x), (int) Math.floor(hp.z));
			Sigf.teleport(h(), new Vec3(hp.x, gy, hp.z), Vec3.ZERO);
			Match.startRound(true, 9);
			Sigf.title("FORTNITE TAKEOVER", "Welcome to Blocky Island", 3);
		});
		// the bus is flying; the player looks around from the roof, then jumps
		for (int i = 0; i < 14; i++) Sigf.demo(0.5 + i * 0.5, () -> { if (Match.bus != null) Sigf.lookAt(h(), Match.busPos.add(0, 12, 0)); });
		Sigf.demo(7.5, Match::jumpHost);
		// the loot llama pops up just ahead of the landing spot
		Sigf.demo(18, () -> {
			Vec3 look = h().getLookAngle();
			Vec3 at = Sigf.ground(h().position().add(look.x * 6, 0, look.z * 6), 0.5);
			if (at.distanceTo(h().position()) < 3) at = Sigf.ground(h().position().add(5, 0, 0), 0.5);
			Match.spawnLlama(at);
			Sigf.title("LOOT LLAMA!", "Smash it open", 3);
		});
		Sigf.demo(19, () -> { if (!Match.llamas.isEmpty()) Sigf.lookAt(h(), Match.llamas.get(0).position().add(0, 1.2, 0)); });
		for (int i = 0; i < 3; i++) {
			Sigf.demo(21 + i * 0.8, () -> {
				if (Match.llamas.isEmpty()) return;
				var l = Match.llamas.get(0);
				Sigf.lookAt(h(), l.position().add(0, 1.2, 0));
				h().swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
				l.hurtServer(Sigf.level(), Sigf.level().damageSources().playerAttack(h()), 11f);
				Sigf.sound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, l.position(), 1f, 1f);
				Sigf.particles(net.minecraft.core.particles.ParticleTypes.CRIT, l.position().add(0, 1.2, 0), 15, 0.5);
			});
		}
		Sigf.demo(24.5, () -> {
			Sigf.command("give @a sigf:shotgun");
			Sigf.command("give @a sigf:shield 3");
			Sigf.command("give @a sigf:build");
			Sigf.command("item replace entity @a weapon.mainhand with sigf:shotgun");
			Sigf.title("LOOT ACQUIRED", "Pump Shotgun, Shields, Build Tool", 3);
		});
		for (int i = 0; i < 9; i++) Sigf.demo(26 + i * 1.6, SigfMod::aimAndShoot);
		// cover: a wall between the player and the next bots, then a ramp behind it to shoot over it from above
		Sigf.demo(36, () -> { Mob m = nearestBot(); if (m != null) Sigf.lookAt(h(), m.position()); Gear.build(h(), false); Sigf.title("BUILD!", "", 2); });
		Sigf.demo(41, () -> Gear.drinkShield(h()));
		Sigf.demo(44, () -> {
			var f = net.minecraft.core.Direction.fromYRot(h().getYRot());
			Sigf.lookAt(h(), h().getEyePosition().add(-f.getStepX() * 6, 0, -f.getStepZ() * 6));
		});
		Sigf.demo(44.5, () -> Gear.build(h(), true));
		for (int i = 1; i <= 4; i++) {
			Sigf.demo(45.2 + i * 0.4, () -> {
				var f = net.minecraft.core.Direction.fromYRot(h().getYRot());
				Vec3 p = h().position();
				Sigf.teleport(h(), new Vec3(p.x + f.getStepX() * 0.95, p.y + 1, p.z + f.getStepZ() * 0.95), Vec3.ZERO);
			});
		}
		for (int i = 0; i < 9; i++) Sigf.demo(48 + i * 1.5, SigfMod::aimAndShoot);
		Sigf.demo(66, () -> {
			for (Mob m : new java.util.ArrayList<>(Match.bots)) m.hurtServer(Sigf.level(), Sigf.level().damageSources().playerAttack(h()), 100f);
		});
		Sigf.demo(64.5, SigfMod::aimAndShoot);
		Sigf.demo(62, () -> {
			for (Mob m : new java.util.ArrayList<>(Match.bots)) if (m != nearestBot()) m.hurtServer(Sigf.level(), Sigf.level().damageSources().playerAttack(h()), 100f);
		});
	}
}
