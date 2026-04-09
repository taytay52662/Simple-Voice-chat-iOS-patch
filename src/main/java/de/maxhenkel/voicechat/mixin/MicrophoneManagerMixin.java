package de.maxhenkel.voicechat.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import de.maxhenkel.voicechat.voice.client.microphone.IOSMicrophone;

@Mixin(MicrophoneManager.class)
public class MicrophoneManagerMixin {

    @Redirect(
        method = "createMicrophone",
        at = @At(
            value = "NEW",
            target = "Lde/maxhenkel/voicechat/voice/client/microphone/Microphone;"
        )
    )
    private static Microphone redirectCreateMicrophone() {
        // Replace the original Microphone with the iOS version
        return new IOSMicrophone();
    }

}