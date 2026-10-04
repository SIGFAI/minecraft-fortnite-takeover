package sigf.mod;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Blocky Island's landing zone: a flattened plaza with a tower, a house and cover to fight around. */
public final class Arena {
	private Arena() {}

	static String f(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
		return "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " " + block;
	}

	public static void build(Vec3 c) {
		int cx = (int) Math.floor(c.x), cz = (int) Math.floor(c.z);
		int gy = Sigf.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
		int r = 22;
		Sigf.command(f(cx - r, gy, cz - r, cx + r, gy + 14, cz + r, "air"));
		Sigf.command(f(cx - r, gy - 8, cz - r, cx + r, gy - 2, cz + r, "dirt replace air"));
		Sigf.command(f(cx - r, gy - 8, cz - r, cx + r, gy - 2, cz + r, "dirt replace water"));
		Sigf.command(f(cx - r, gy - 1, cz - r, cx + r, gy - 1, cz + r, "grass_block"));
		// path of gravel to the tower
		Sigf.command(f(cx, gy - 1, cz, cx + 12, gy - 1, cz + 1, "dirt_path"));
		// "Tilted Tower": stone brick tower, three floors
		int tx = cx + 12, tz = cz + 12;
		Sigf.command(f(tx - 3, gy, tz - 3, tx + 3, gy + 15, tz + 3, "stone_bricks hollow"));
		Sigf.command(f(tx - 2, gy + 5, tz - 2, tx + 2, gy + 5, tz + 2, "oak_planks"));
		Sigf.command(f(tx - 2, gy + 10, tz - 2, tx + 2, gy + 10, tz + 2, "oak_planks"));
		Sigf.command(f(tx - 3, gy, tz - 3, tx - 3, gy + 1, tz - 3, "air")); // door gap
		Sigf.command(f(tx - 3, gy + 1, tz - 1, tx - 3, gy + 2, tz, "air"));
		Sigf.command(f(tx - 3, gy + 7, tz, tx - 3, gy + 8, tz, "glass_pane"));
		Sigf.command(f(tx + 3, gy + 7, tz, tx + 3, gy + 8, tz, "glass_pane"));
		Sigf.command(f(tx, gy + 7, tz - 3, tx, gy + 8, tz - 3, "glass_pane"));
		Sigf.command(f(tx - 3, gy + 12, tz - 1, tx - 3, gy + 13, tz + 1, "air"));
		Sigf.command(f(tx - 3, gy + 16, tz - 3, tx + 3, gy + 16, tz + 3, "blue_concrete"));
		Sigf.command(f(tx - 3, gy + 17, tz - 3, tx + 3, gy + 17, tz - 3, "stone_brick_wall"));
		Sigf.command(f(tx - 3, gy + 17, tz + 3, tx + 3, gy + 17, tz + 3, "stone_brick_wall"));
		Sigf.command(f(tx - 3, gy + 17, tz - 3, tx - 3, gy + 17, tz + 3, "stone_brick_wall"));
		Sigf.command(f(tx + 3, gy + 17, tz - 3, tx + 3, gy + 17, tz + 3, "stone_brick_wall"));
		// "Pleasant" house with a flat blue roof
		int hx = cx - 13, hz = cz + 11;
		Sigf.command(f(hx - 4, gy, hz - 3, hx + 4, gy + 4, hz + 3, "oak_planks hollow"));
		Sigf.command(f(hx - 5, gy + 5, hz - 4, hx + 5, gy + 5, hz + 4, "light_blue_concrete"));
		Sigf.command(f(hx + 4, gy, hz, hx + 4, gy + 1, hz, "air"));
		Sigf.command(f(hx - 1, gy + 2, hz - 3, hx + 1, gy + 3, hz - 3, "glass_pane"));
		Sigf.command(f(hx - 1, gy + 2, hz + 3, hx + 1, gy + 3, hz + 3, "glass_pane"));
		// low stone-brick cover and hay bales
		Sigf.command(f(cx - 6, gy, cz - 8, cx - 3, gy + 1, cz - 8, "stone_bricks"));
		Sigf.command(f(cx + 6, gy, cz - 9, cx + 9, gy + 1, cz - 9, "stone_bricks"));
		Sigf.command(f(cx + 5, gy, cz + 6, cx + 6, gy + 1, cz + 6, "hay_block"));
		Sigf.command(f(cx - 8, gy, cz - 2, cx - 8, gy + 1, cz - 1, "hay_block"));
		// a campfire and a glowing loot chest
		Sigf.command("setblock " + (cx - 2) + " " + gy + " " + (cz + 5) + " campfire");
		Sigf.command("setblock " + (cx + 3) + " " + gy + " " + (cz - 6) + " chest[facing=south]");
		Sigf.command("setblock " + (cx + 3) + " " + (gy + 1) + " " + (cz - 6) + " air");
		Sigf.command("setblock " + (cx + 3) + " " + (gy - 1) + " " + (cz - 6) + " gold_block");
	}
}
