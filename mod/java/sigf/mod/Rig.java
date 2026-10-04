package sigf.mod;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import sigf.kit.Sigf;

/** A 3D model made of scaled block displays that moves as one piece (the game's own blocks as materials). */
public final class Rig {
	private final List<Display.BlockDisplay> parts = new ArrayList<>();
	public Vec3 pos;
	private double s = 1;

	public Rig scale(double k) { s = k; return this; }

	public Rig(Vec3 pos) { this.pos = pos; }

	/** Adds a box from corner (x0,y0,z0) to (x1,y1,z1), relative to the rig origin. */
	public Rig box(Block b, double x0, double y0, double z0, double x1, double y1, double z1) {
		ServerLevel lvl = Sigf.level();
		Display.BlockDisplay d = EntityTypes.BLOCK_DISPLAY.create(lvl, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		d.setBlockState(b.defaultBlockState());
		d.setTransformation(new Transformation(new Vector3f((float) (x0 * s), (float) (y0 * s), (float) (z0 * s)), null,
			new Vector3f((float) ((x1 - x0) * s), (float) ((y1 - y0) * s), (float) ((z1 - z0) * s)), null));
		d.setBrightnessOverride(new Brightness(15, 15));
		d.setPosRotInterpolationDuration(3);
		d.setViewRange(8f);
		d.snapTo(pos.x, pos.y, pos.z, 0f, 0f);
		lvl.addFreshEntity(d);
		parts.add(d);
		return this;
	}

	public void move(Vec3 p) {
		pos = p;
		for (Display.BlockDisplay d : parts) d.teleportTo(p.x, p.y, p.z);
	}

	public void remove() {
		for (Display.BlockDisplay d : parts) d.discard();
		parts.clear();
	}
}
