package sigf.mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** The match: the Battle Bus flies over, bots glide down, the Loot Llama appears, last one standing wins. */
public final class Match {
	private Match() {}

	public static final String BOT = "fn_bot", LLAMA = "fn_llama";
	static final String[] NAMES = {"Tilted Tim", "Salty Sam", "Greasy Greg", "Dusty Dave", "Lazy Larry", "Retail Rob", "Pleasant Pete", "Loot Lake Lou", "Moisty Mike", "Risky Rick"};

	static Rig bus;
	static Vec3 busPos;
	static int dropsLeft;
	static boolean active, victoryDone;
	static ArmorStand busSeat;
	static final List<Mob> bots = new ArrayList<>();
	static final List<Glide> glides = new ArrayList<>();
	static final List<Llama> llamas = new ArrayList<>();
	static ServerBossEvent bar;
	static int round;
	static Vec3 stageC = Vec3.ZERO;
	static double stormR = -1;

	static final class Glide {
		Entity who; Rig rig; ArmorStand seat; double vx, vz; int age;
	}

	public static void init() {
		bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.literal("PLAYERS LEFT"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
		Sigf.every(1, Match::slowTick);
	}

	// ---- round flow ----

	/** Starts a round: the bus comes in from the west. withHost = the stream player rides it. */
	public static void startRound(boolean withHost, int botCount) {
		round++;
		for (Mob m : bots) m.discard();
		bots.clear();
		for (Llama l : llamas) l.discard();
		llamas.clear();
		victoryDone = false;
		active = true;
		dropsLeft = botCount;
		Vec3 c = center();
		stageC = c;
		if (round == 1) Arena.build(c);
		double gy = Sigf.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(c.x), (int) Math.floor(c.z));
		busPos = new Vec3(c.x - 70, gy + 22, c.z + (withHost ? 16 : (Sigf.level().getRandom().nextDouble() - 0.5) * 16));
		bus = Models.battleBus(busPos);
		Sigf.sound(SoundEvents.NOTE_BLOCK_BELL.value(), c, 2f, 0.6f);
		Sigf.title("BATTLE BUS", "Drop onto Blocky Island!", 3);
		updateBar();
		stormR = -1;
		Sigf.after(22, () -> {
			stormR = 34;
			Sigf.title("§5THE STORM IS CLOSING", "§dStay inside the circle", 3);
			Sigf.sound(SigfMod.STORM_SOUND, center(), 1.5f, 0.8f);
		});
		// the storm warning
		Sigf.after(3, () -> Sigf.sound(SigfMod.STORM_SOUND, center(), 1f, 1f));
	}

	static double stageGround() {
		return Sigf.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(stageC.x), (int) Math.floor(stageC.z));
	}

	static Vec3 center() { return Sigf.host() != null ? Sigf.host().position() : sigf.kit.SigfKit.center(); }

	static ArmorStand seat(Vec3 at) {
		ArmorStand s = Sigf.spawn(EntityTypes.ARMOR_STAND, at);
		s.setInvisible(true);
		s.setNoGravity(true);
		s.setPermanentlyInvulnerable(true);
		return s;
	}

	/** The stream player jumps off the bus and opens the glider. */
	public static void jumpHost() {
		ServerPlayer h = Sigf.host();
		if (h == null || bus == null) return;
		Vec3 p = busPos.add(3, -4, 0);
		Sigf.teleport(h, p, Vec3.ZERO);
		Sigf.lookAt(h, p.add(40, -3, 0));
		ArmorStand s = seat(p);
		h.startRiding(s, true, false);
		Glide g = new Glide();
		double ticks = Math.max(30, (p.y - stageGround()) / 0.17);
		g.who = h; g.seat = s; g.vx = (stageC.x + 4 - p.x) / ticks; g.vz = (stageC.z + 4 - p.z) / ticks;
		g.rig = Models.glider(p, 3.5);
		glides.add(g);
		Sigf.sound(SoundEvents.ELYTRA_FLYING, p, 1f, 1f);
		h.sendSystemMessage(Component.literal("§b§lGLIDING"), true);
		Sigf.sound(SoundEvents.WOOL_PLACE, p, 1.5f, 0.7f);
		Sigf.title("GLIDE!", "Aim for the Loot Llama", 2);
	}

	static void dropBot(Vec3 at, boolean zombie) {
		Mob m = zombie ? Sigf.spawn(EntityTypes.ZOMBIE, at) : Sigf.spawn(EntityTypes.SKELETON, at);
		if (m == null) return;
		String name = zombie ? NAMES[Sigf.level().getRandom().nextInt(NAMES.length)] : "Storm Trooper";
		m.setCustomName(Component.literal((zombie ? "§e" : "§d") + name));
		m.setCustomNameVisible(true);
		m.addTag(BOT);
		m.setPersistenceRequired();
		ItemStack cap = new ItemStack(Items.LEATHER_HELMET);
		cap.set(DataComponents.DYED_COLOR, new DyedItemColor(zombie ? 0x2E86FF : 0x8A2BE2));
		m.setItemSlot(EquipmentSlot.HEAD, cap);
		m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(zombie ? SigfMod.SHOTGUN : Items.BOW));
		m.setDropChance(EquipmentSlot.HEAD, 0f);
		m.setDropChance(EquipmentSlot.MAINHAND, zombie ? 0.25f : 0f);
		if (zombie) m.addEffect(new MobEffectInstance(MobEffects.SPEED, 20000, 0, false, false));
		bots.add(m);
		Glide g = new Glide();
		g.who = m;
		var rnd = Sigf.level().getRandom();
		double ticks = Math.max(30, (at.y - stageGround()) / 0.17);
		g.vx = (stageC.x + (rnd.nextDouble() - 0.5) * 28 - at.x) / ticks;
		g.vz = (stageC.z + (rnd.nextDouble() - 0.5) * 28 - at.z) / ticks;
		g.rig = Models.glider(at, 3.0);
		glides.add(g);
		Sigf.sound(SoundEvents.WOOL_PLACE, at, 1.2f, 0.8f);
	}

	// ---- every tick ----

	public static void tick() {
		if (Sigf.level() == null) return;
		long now = Sigf.level().getGameTime();
		if (bus != null) {
			busPos = busPos.add(0.45, 0, 0);
			bus.move(busPos);
			if (busSeat != null) busSeat.setPos(busPos.x + 1.5, busPos.y + 3.3, busPos.z);
			Vec3 c = stageC;
			if (dropsLeft > 0 && busPos.x > c.x - 18 && now % 5 == 0) {
				Vec3 at = busPos.add(0, -3.5, (Sigf.level().getRandom().nextDouble() - 0.5) * 4);
				boolean zombie = dropsLeft % 3 != 0;
				dropsLeft--;
				dropBot(at, zombie);
				updateBar();
			}
			if (now % 10 == 0) Sigf.particles(ParticleTypes.CLOUD, busPos.add(-10, 1, 0), 2, 0.4);
			if (busPos.x > c.x + 110) { bus.remove(); bus = null; }
		}
		Iterator<Glide> it = glides.iterator();
		while (it.hasNext()) {
			Glide g = it.next();
			g.age++;
			if (g.seat != null) {
				Vec3 p = g.seat.position().add(g.vx, -0.17, g.vz);
				int gy = Sigf.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(p.x), (int) Math.floor(p.z));
				if (p.y <= gy + 0.2) { land(g, it); continue; }
				g.seat.setPos(p.x, p.y, p.z);
				if (g.age % 10 == 0 && g.who instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§b§lGLIDING"), true);
				g.rig.move(p.add(0, 1.375, 0));
			} else {
				if (!g.who.isAlive()) { g.rig.remove(); it.remove(); continue; }
				if (g.who.onGround() && g.age > 15) { land(g, it); continue; }
				Vec3 d = g.who.getDeltaMovement();
				g.who.setDeltaMovement(g.vx * (g.age > 400 ? 0 : 1), -0.17, g.vz);
				g.who.syncVelocity = true;
				g.who.resetFallDistance();
				g.rig.move(g.who.position());
			}
		}
		// the storm: a purple wall that closes in on the island
		if (stormR > 0 && active) {
			if (stormR > 13) stormR -= 0.0035;
			if (now % 6 == 0) {
				int steps = 48;
				double gyS = stageGround();
				for (int i = 0; i < steps; i++) {
					double a = i * Math.PI * 2 / steps;
					for (int hh = 0; hh < 9; hh++) {
						Sigf.level().sendParticles(new DustParticleOptions(hh % 2 == 0 ? 0x8A2BE2 : 0xD070FF, 3.0f),
							stageC.x + Math.cos(a) * stormR, gyS + hh * 2.2, stageC.z + Math.sin(a) * stormR, 1, 0.3, 0.2, 0.3, 0);
					}
				}
			}
		}
		// loot beams over llamas
		if (now % 4 == 0) for (Llama l : llamas) if (l.isAlive()) {
			Vec3 b = l.position();
			for (int i = 0; i < 18; i++) Sigf.level().sendParticles(ParticleTypes.END_ROD, b.x, b.y + 2 + i * 1.3, b.z, 1, 0.1, 0.1, 0.1, 0);
			Sigf.level().sendParticles(new DustParticleOptions(0xFF4FD8, 1.4f), b.x, b.y + 1.5, b.z, 2, 0.6, 0.6, 0.6, 0);
		}
	}

	static void land(Glide g, Iterator<Glide> it) {
		g.rig.remove();
		if (g.seat != null) {
			if (g.who instanceof ServerPlayer p) { p.stopRiding(); p.sendSystemMessage(Component.empty(), true); }
			Vec3 p = g.seat.position();
			g.seat.discard();
			Sigf.teleport(g.who, p, Vec3.ZERO);
			if (g.who instanceof ServerPlayer sp2) sp2.sendSystemMessage(Component.empty(), true);
		}
		g.who.resetFallDistance();
		Sigf.particles(ParticleTypes.CLOUD, g.who.position(), 12, 0.6);
		Sigf.sound(SoundEvents.WOOL_BREAK, g.who.position(), 1f, 1f);
		it.remove();
	}

	// ---- every second ----

	static void slowTick() {
		for (ServerPlayer p : Sigf.players()) bar.addPlayer(p);
		if (stormR > 0) {
			for (Mob m : bots) if (m.isAlive() && Math.hypot(m.getX() - stageC.x, m.getZ() - stageC.z) > stormR) {
				m.hurtServer(Sigf.level(), Sigf.level().damageSources().outOfBorder(), 2f);
				Sigf.particles(new DustParticleOptions(0x8A2BE2, 1.2f), m.position().add(0, 1, 0), 10, 0.4);
			}
			ServerPlayer hp = Sigf.host();
			if (hp != null && !hp.isSpectator() && !hp.isInvulnerable() && Math.hypot(hp.getX() - stageC.x, hp.getZ() - stageC.z) > stormR)
				hp.hurtServer(Sigf.level(), Sigf.level().damageSources().outOfBorder(), 2f);
		}
		bots.removeIf(m -> !m.isAlive());
		llamas.removeIf(l -> !l.isAlive());
		updateBar();
		ServerPlayer h = Sigf.host();
		boolean playerIn = h != null && !h.isSpectator();
		for (Mob m : bots) {
			if (m.getVehicle() != null) continue;
			if (playerIn && !h.isInvulnerable() && m.distanceTo(h) < 40) { m.setTarget(h); continue; }
			Entity best = null; double bd = 40;
			for (Entity e : Sigf.near(m.position(), 40, e -> e != m && ((e instanceof Mob o && o.getType() != m.getType() && o.entityTags().contains(BOT)) || (e instanceof Llama l && l.entityTags().contains(LLAMA))))) {
				double d = e.distanceTo(m);
				if (d < bd) { bd = d; best = e; }
			}
			if (best instanceof LivingEntity le) m.setTarget(le);
			else if (playerIn && m.distanceTo(h) < 40 && m.distanceTo(h) > 4) m.getNavigation().moveTo(h, 1.1);
		}
		if (active && !victoryDone && dropsLeft == 0 && busPos != null && bots.size() <= (playerIn ? 0 : 1)) victory();
	}

	static void updateBar() {
		int left = bots.size() + dropsLeft + (Sigf.host() != null && !Sigf.host().isSpectator() ? 1 : 0);
		bar.setName(Component.literal("§d§lPLAYERS LEFT: " + left));
		bar.setProgress(Math.max(0.02f, Math.min(1f, left / 10f)));
	}

	static void victory() {
		victoryDone = true;
		Vec3 c = center();
		Sigf.title("§6§l#1 VICTORY ROYALE", "§eThe last one standing wins!", 5);
		Sigf.sound(SigfMod.VICTORY_SOUND, c, 1.5f, 1f);
		for (int i = 0; i < 5; i++) {
			final int k = i;
			Sigf.after(i * 0.4, () -> {
				Vec3 p = c.add(Math.cos(k * 1.3) * 5, 3 + k, Math.sin(k * 1.3) * 5);
				Sigf.particles(ParticleTypes.FIREWORK, p, 60, 1.2);
				Sigf.particles(ParticleTypes.END_ROD, p, 30, 1.5);
				Sigf.sound(SoundEvents.FIREWORK_ROCKET_BLAST, p, 2f, 1f);
				Sigf.sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, p, 2f, 1f);
			});
		}
		if (!Sigf.isDemo()) Sigf.after(16, () -> startRound(false, 8));
		else if (Sigf.host() != null) {
			// victory dance: the shotgun goes away (the camera swings behind the player) and the player spins
			Sigf.command("item replace entity @a weapon.mainhand with minecraft:air");
			ServerPlayer h = Sigf.host();
			int gyv = (int) stageGround();
			Sigf.teleport(h, new Vec3(stageC.x + 0.5, gyv, stageC.z + 0.5), Vec3.ZERO);
			for (int i = 0; i < 40; i++) {
				final int k = i;
				Sigf.after(0.5 + i * 0.1, () -> {
					Vec3 pp = h.position();
					h.connection.teleport(pp.x, pp.y, pp.z, k * 38f, 8f);
					if (k % 3 == 0) h.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
				});
			}
		}
	}

	// ---- deaths ----

	public static void onDeath(LivingEntity e, DamageSource src) {
		if (e.entityTags().contains(BOT)) {
			Vec3 p = e.position();
			for (int i = 0; i < 14; i++) Sigf.level().sendParticles(ParticleTypes.END_ROD, p.x, p.y + i * 0.7, p.z, 2, 0.15, 0.1, 0.15, 0.02);
			Sigf.particles(new DustParticleOptions(0x3AA8FF, 1.5f), p.add(0, 1, 0), 30, 0.5);
			Sigf.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, p, 2f, 1.6f);
			String killer = src.getEntity() != null ? src.getEntity().getName().getString() : "The Storm";
			if (src.getEntity() instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§6§lELIMINATED §f" + e.getName().getString()), true);
			Sigf.say("§7" + killer + " §feliminated §e" + e.getName().getString());
			if (Sigf.level().getRandom().nextFloat() < 0.6f) {
				ItemEntity it = new ItemEntity(Sigf.level(), p.x, p.y + 0.5, p.z, new ItemStack(SigfMod.SHIELD));
				it.setGlowingTag(true);
				Sigf.level().addFreshEntity(it);
			}
		} else if (e.entityTags().contains(LLAMA)) {
			lootBurst(e.position());
		}
	}

	public static Llama spawnLlama(Vec3 at) {
		Llama l = Sigf.spawn(EntityTypes.LLAMA, at);
		if (l == null) return null;
		l.setCustomName(Component.literal("§d§lLOOT LLAMA"));
		l.setCustomNameVisible(true);
		l.setGlowingTag(true);
		l.addTag(LLAMA);
		l.setPersistenceRequired();
		l.getAttribute(Attributes.SCALE).setBaseValue(1.5);
		l.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);
		l.setHealth(30);
		l.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.CARPET.magenta()));
		llamas.add(l);
		Sigf.particles(ParticleTypes.FIREWORK, at.add(0, 1, 0), 40, 0.8);
		Sigf.sound(SoundEvents.LLAMA_ANGRY, at, 1.5f, 1.2f);
		return l;
	}

	static void lootBurst(Vec3 p) {
		Sigf.sound(SigfMod.LOOT_SOUND, p, 1.5f, 1f);
		Sigf.sound(SoundEvents.FIREWORK_ROCKET_BLAST, p, 2f, 1f);
		Sigf.particles(ParticleTypes.FIREWORK, p.add(0, 1.5, 0), 80, 1.0);
		Sigf.particles(ParticleTypes.HAPPY_VILLAGER, p.add(0, 1.5, 0), 40, 1.2);
		Sigf.particles(new DustParticleOptions(0xFF4FD8, 2f), p.add(0, 1.5, 0), 60, 1.2);
		Sigf.particles(new DustParticleOptions(0xFFD84F, 2f), p.add(0, 1.5, 0), 60, 1.2);
		ItemStack[] loot = {new ItemStack(SigfMod.SHOTGUN), new ItemStack(SigfMod.SHIELD, 3), new ItemStack(SigfMod.BUILD), new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.COOKED_BEEF, 6), new ItemStack(Items.DIAMOND, 2)};
		for (ItemStack s : loot) {
			ItemEntity it = new ItemEntity(Sigf.level(), p.x, p.y + 1.5, p.z, s);
			it.setDeltaMovement((Sigf.level().getRandom().nextDouble() - 0.5) * 0.5, 0.45, (Sigf.level().getRandom().nextDouble() - 0.5) * 0.5);
			it.setGlowingTag(true);
			it.setPickUpDelay(20);
			Sigf.level().addFreshEntity(it);
		}
	}
}
