package de.maxhenkel.voicechat.ios;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class VoicechatIOSPatch implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("voicechat-ios");
    @Override
    public void onInitialize() {
        if (AudioQueueMicrophone.isAvailable()) {
            LOGGER.info("[voicechat-ios] AudioToolbox detected - iOS microphone patch active");
        } else {
            LOGGER.info("[voicechat-ios] AudioToolbox not found - patch inactive");
        }
    }
}
