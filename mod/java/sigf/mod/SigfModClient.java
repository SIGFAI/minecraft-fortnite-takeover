package sigf.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import sigf.kit.Sigf;

/** In the demo the camera follows the glide over the shoulder, then goes first person once the shotgun is in hand. */
public final class SigfModClient implements ClientModInitializer {
	private static boolean glided;
	private static CameraType last;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null) { glided = false; last = null; return; }
			if (!Sigf.isDemo() || mc.player.isSpectator()) return;
			if (mc.player.getVehicle() != null) glided = true;
			boolean armed = mc.player.getMainHandItem().is(SigfMod.SHOTGUN);
			CameraType want = glided && !armed ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON;
			if (want != last) { mc.options.setCameraType(want); last = want; }
		});
	}
}
