package dev.yai1024.nomoredie.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla client handles the "protected by totem" entity event (id 35) in
 * {@code ClientPacketListener#handleEntityEvent}. Only when the affected entity
 * is the local player does it call {@code LocalPlayer#displayItemActivation}.
 * Hooking that exact call means we fire only for the user's own Totem of Undying,
 * on the client thread, with no guessing about event ids or entity identity.
 *
 * <p>Instead of merely closing the socket, the mod invokes
 * {@link Minecraft#disconnectFromWorld}, which is the same method the vanilla
 * "Save and Quit to Title" button uses. It disconnects the channel with
 * {@code ClientLevel.DEFAULT_QUIT_MESSAGE}, so the server sees an ordinary
 * client quit (channel closed) rather than a kick or a custom packet.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(
			method = "handleEntityEvent",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/player/LocalPlayer;displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V"
			),
			cancellable = true
	)
	private void nomoredie$leaveOnOwnTotem(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		ci.cancel();

		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			Minecraft client = Minecraft.getInstance();
			if (client.level != null) {
				client.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
			}
		});
	}
}
