package sigf.mod;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The 3D props of the island: the Battle Bus under its balloon, and the glider umbrella. Origin = middle of the bus floor, nose toward +x. */
public final class Models {
	private Models() {}

	public static Rig battleBus(Vec3 at) {
		Rig r = new Rig(at).scale(1.8);
		// body, blue with a white stripe, black windows
		r.box(Blocks.CONCRETE.lightBlue(), -5, 0, -1.7, 5, 3, 1.7);
		r.box(Blocks.CONCRETE.white(), -5.03, 0.9, -1.73, 5.03, 1.3, 1.73);
		r.box(Blocks.CONCRETE.blue(), -4.6, 3, -1.5, 4.0, 3.25, 1.5);
		for (int i = 0; i < 5; i++) {
			double x = -4.3 + i * 1.75;
			r.box(Blocks.CONCRETE.black(), x, 1.6, -1.76, x + 1.35, 2.6, 1.76);
		}
		// hood, grille and headlights
		r.box(Blocks.CONCRETE.lightBlue(), 5, 0.2, -1.5, 6.6, 1.9, 1.5);
		r.box(Blocks.GLOWSTONE, 6.6, 0.8, -1.2, 6.7, 1.3, -0.6);
		r.box(Blocks.GLOWSTONE, 6.6, 0.8, 0.6, 6.7, 1.3, 1.2);
		r.box(Blocks.CONCRETE.black(), 6.6, 0.3, -0.5, 6.7, 0.8, 0.5);
		// tail and bumper
		r.box(Blocks.CONCRETE.white(), -5.4, 0.2, -1.7, -5, 0.8, 1.7);
		// wheels
		for (double x : new double[] {-3.4, 3.4}) {
			r.box(Blocks.CONCRETE.black(), x - 0.7, -0.6, -1.95, x + 0.7, 0.8, -1.35);
			r.box(Blocks.CONCRETE.black(), x - 0.7, -0.6, 1.35, x + 0.7, 0.8, 1.95);
			r.box(Blocks.CONCRETE.lightGray(), x - 0.3, -0.2, -2.0, x + 0.3, 0.4, -1.95);
			r.box(Blocks.CONCRETE.lightGray(), x - 0.3, -0.2, 1.95, x + 0.3, 0.4, 2.0);
		}
		// ropes to the balloon
		for (double x : new double[] {-4, 4}) for (double z : new double[] {-1.2, 1.2})
			r.box(Blocks.CONCRETE.white(), x - 0.07, 3.25, z - 0.07, x + 0.07, 8.2, z + 0.07);
		// the balloon: light blue with yellow band and white star-ish top
		r.box(Blocks.WOOL.lightBlue(), -4.2, 8, -3.2, 4.2, 13, 3.2);
		r.box(Blocks.WOOL.lightBlue(), -3.4, 7.3, -2.4, 3.4, 8, 2.4);
		r.box(Blocks.WOOL.lightBlue(), -3.2, 13, -2.4, 3.2, 14.4, 2.4);
		r.box(Blocks.WOOL.cyan(), -2.2, 14.4, -1.5, 2.2, 15.2, 1.5);
		r.box(Blocks.WOOL.yellow(), -4.25, 9.8, -3.25, 4.25, 10.8, 3.25);
		r.box(Blocks.WOOL.white(), -4.25, 11.3, -3.25, 4.25, 11.7, 3.25);
		// big emblem on the balloon sides: a yellow llama-ish star block
		r.box(Blocks.GOLD_BLOCK, -1, 11.7, 3.2, 1, 12.9, 3.3);
		r.box(Blocks.GOLD_BLOCK, -1, 11.7, -3.3, 1, 12.9, -3.2);
		return r;
	}

	/** The umbrella glider; origin = the feet of whoever carries it. */
	public static Rig glider(Vec3 at, double h) {
		Rig r = new Rig(at);
		// a dome-ish canopy: wide lower skirt, narrower top, striped
		r.box(Blocks.WOOL.lightBlue(), -1.5, h, -1.5, 1.5, h + 0.2, 1.5);
		r.box(Blocks.WOOL.lightBlue(), -1.0, h + 0.2, -1.0, 1.0, h + 0.4, 1.0);
		r.box(Blocks.WOOL.white(), -0.4, h + 0.4, -0.4, 0.4, h + 0.55, 0.4);
		r.box(Blocks.WOOL.yellow(), -1.6, h - 0.12, -1.6, 1.6, h + 0.05, -1.3);
		r.box(Blocks.WOOL.yellow(), -1.6, h - 0.12, 1.3, 1.6, h + 0.05, 1.6);
		r.box(Blocks.WOOL.yellow(), -1.6, h - 0.12, -1.3, -1.3, h + 0.05, 1.3);
		r.box(Blocks.WOOL.yellow(), 1.3, h - 0.12, -1.3, 1.6, h + 0.05, 1.3);
		for (double x : new double[] {-1.3, 1.3}) for (double z : new double[] {-1.3, 1.3})
			r.box(Blocks.CONCRETE.white(), x - 0.025, 1.9, z - 0.025, x + 0.025, h, z + 0.025);
		return r;
	}
}
