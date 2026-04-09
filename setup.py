import os

files = {}

files['settings.gradle'] = 'rootProject.name = "voicechat-ios-patch"\n'

files['build.gradle'] = '''plugins {
    id 'fabric-loom' version '1.7-SNAPSHOT'
    id 'java'
}
version = '1.0.0'
group = 'de.maxhenkel.voicechat'
archivesBaseName = 'voicechat-ios-patch'
repositories {
    maven { url 'https://maven.fabricmc.net/' }
    maven { url 'https://maven.maxhenkel.de/repository/public' }
    mavenCentral()
}
dependencies {
    minecraft "com.mojang:minecraft:1.21.11"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:0.16.0"
    modCompileOnly "de.maxhenkel.voicechat:voicechat-fabric-1.21.11:2.6.15"
    compileOnly 'net.java.dev.jna:jna:5.13.0'
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 21
}
jar {
    from('src/main/resources') {
        include 'fabric.mod.json'
        include '*.mixins.json'
    }
}
'''

files['.github/workflows/build.yml'] = '''name: Build
on:
  push:
    branches: [ main ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.8'
      - run: gradle build
      - uses: actions/upload-artifact@v4
        with:
          name: voicechat-ios-patch
          path: build/libs/voicechat-ios-patch-*.jar
          if-no-files-found: error
'''

files['src/main/resources/fabric.mod.json'] = '''{
  "schemaVersion": 1,
  "id": "voicechat-ios",
  "version": "1.0.0",
  "name": "Simple Voice Chat iOS Patch",
  "description": "Patches Simple Voice Chat microphone capture to use AudioQueue on iOS",
  "environment": "client",
  "entrypoints": {
    "main": ["de.maxhenkel.voicechat.ios.VoicechatIOSPatch"]
  },
  "mixins": ["voicechat-ios.mixins.json"],
  "depends": {
    "fabricloader": ">=0.14.0",
    "minecraft": ">=1.21",
    "java": ">=21",
    "voicechat": "*"
  }
}
'''

files['src/main/resources/voicechat-ios.mixins.json'] = '''{
  "required": true,
  "minVersion": "0.8",
  "package": "de.maxhenkel.voicechat.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["MicrophoneManagerMixin","MicrophoneListMixin"],
  "injectors": {"defaultRequire": 1}
}
'''

files['src/main/java/de/maxhenkel/voicechat/ios/VoicechatIOSPatch.java'] = '''package de.maxhenkel.voicechat.ios;
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
'''

files['src/main/java/de/maxhenkel/voicechat/ios/AudioQueueMicrophone.java'] = '''package de.maxhenkel.voicechat.ios;
import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import de.maxhenkel.voicechat.voice.client.MicrophoneException;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioQueueMicrophone implements Microphone {
    public interface AudioToolbox extends Library {
        AudioToolbox INSTANCE = Native.load("AudioToolbox", AudioToolbox.class);
        int AudioQueueNewInput(Pointer inFormat, Callback inCallbackProc, Pointer inUserData,
            Pointer inCallbackRunLoop, Pointer inCallbackRunLoopMode, int inFlags, PointerByReference outAQ);
        int AudioQueueAllocateBuffer(Pointer inAQ, int inBufferByteSize, PointerByReference outBuffer);
        int AudioQueueEnqueueBuffer(Pointer inAQ, Pointer inBuffer, int inNumPacketDescs, Pointer inPacketDescs);
        int AudioQueueStart(Pointer inAQ, Pointer inStartTime);
        int AudioQueueStop(Pointer inAQ, byte inImmediate);
        int AudioQueueDispose(Pointer inAQ, byte inImmediate);
    }

    public interface AudioQueueInputCallback extends Callback {
        void invoke(Pointer userData, Pointer inAQ, Pointer inBuffer,
            Pointer inStartTime, int inNumPackets, Pointer inPacketDescs);
    }

    private static final int kAudioFormatLinearPCM = 0x6C70636D;
    private static final int PCM_FLAGS = 0x4 | 0x8;
    private static final int AQ_OK = 0;

    private final int sampleRate;
    private final int bufferSize;
    private final String deviceName;
    private Pointer audioQueue;
    private final Pointer[] aqBuffers = new Pointer[3];
    private volatile boolean open = false;
    private volatile boolean started = false;
    private final BlockingQueue<byte[]> pcmQueue = new ArrayBlockingQueue<>(32);
    private AudioQueueInputCallback callback;
    private int bytesPerBuffer;

    public AudioQueueMicrophone(int sampleRate, int bufferSize, String deviceName) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.deviceName = deviceName;
        this.bytesPerBuffer = bufferSize * 2;
    }

    @Override
    public void open() throws MicrophoneException {
        if (open) throw new MicrophoneException("Microphone already open");
        try {
            AudioToolbox at = AudioToolbox.INSTANCE;
            Memory asbd = new Memory(40);
            asbd.setDouble(0, (double) sampleRate);
            asbd.setInt(8, kAudioFormatLinearPCM);
            asbd.setInt(12, PCM_FLAGS);
            asbd.setInt(16, 2);
            asbd.setInt(20, 1);
            asbd.setInt(24, 2);
            asbd.setInt(28, 1);
            asbd.setInt(32, 16);
            asbd.setInt(36, 0);
            callback = (userData, inAQ, inBuffer, inStartTime, inNumPackets, inPacketDescs) -> {
                try {
                    Pointer audioData = inBuffer.getPointer(8);
                    int byteSize = inBuffer.getInt(16);
                    if (byteSize > 0 && audioData != null) {
                        pcmQueue.offer(audioData.getByteArray(0, byteSize));
                    }
                    if (started) at.AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, null);
                } catch (Exception ignored) {}
            };
            PointerByReference queueRef = new PointerByReference();
            int err = at.AudioQueueNewInput(asbd, callback, null, null, null, 0, queueRef);
            if (err != AQ_OK) throw new MicrophoneException("AudioQueueNewInput failed: " + err);
            audioQueue = queueRef.getValue();
            for (int i = 0; i < 3; i++) {
                PointerByReference bufRef = new PointerByReference();
                err = at.AudioQueueAllocateBuffer(audioQueue, bytesPerBuffer, bufRef);
                if (err != AQ_OK) throw new MicrophoneException("AudioQueueAllocateBuffer failed: " + err);
                aqBuffers[i] = bufRef.getValue();
                at.AudioQueueEnqueueBuffer(audioQueue, aqBuffers[i], 0, null);
            }
            open = true;
        } catch (MicrophoneException e) {
            throw e;
        } catch (Throwable t) {
            throw new MicrophoneException("Failed to open iOS microphone: " + t.getMessage());
        }
    }

    @Override
    public void start() throws MicrophoneException {
        if (!open) throw new MicrophoneException("Microphone is not open");
        started = true;
        int err = AudioToolbox.INSTANCE.AudioQueueStart(audioQueue, null);
        if (err != AQ_OK) { started = false; throw new MicrophoneException("AudioQueueStart failed: " + err); }
    }

    @Override
    public void stop() {
        if (!started) return;
        started = false;
        try { AudioToolbox.INSTANCE.AudioQueueStop(audioQueue, (byte) 0); } catch (Throwable ignored) {}
    }

    @Override
    public void close() {
        stop(); open = false; pcmQueue.clear();
        if (audioQueue != null) {
            try { AudioToolbox.INSTANCE.AudioQueueDispose(audioQueue, (byte) 1); } catch (Throwable ignored) {}
            audioQueue = null;
        }
    }

    @Override public boolean isOpen() { return open; }
    @Override public boolean isStarted() { return started; }
    @Override public int available() { return pcmQueue.size() * bufferSize; }

    @Override
    public short[] read() throws MicrophoneException {
        if (!open) throw new MicrophoneException("Microphone was not opened");
        try {
            byte[] bytes = pcmQueue.poll(100, TimeUnit.MILLISECONDS);
            if (bytes == null) return new short[0];
            short[] shorts = new short[bytes.length / 2];
            for (int i = 0; i < shorts.length; i++)
                shorts[i] = (short) ((bytes[i*2] & 0xFF) | ((bytes[i*2+1] & 0xFF) << 8));
            return shorts;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return new short[0]; }
    }

    public static List<String> getDeviceNames() { return Collections.singletonList("Default"); }

    public static boolean isAvailable() {
        try { AudioToolbox.INSTANCE.getClass(); return true; } catch (Throwable t) { return false; }
    }
}
'''

files['src/main/java/de/maxhenkel/voicechat/mixin/MicrophoneManagerMixin.java'] = '''package de.maxhenkel.voicechat.mixin;
import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.MicrophoneException;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.lang.reflect.Field;

@Mixin(value = MicrophoneManager.class, remap = false)
public class MicrophoneManagerMixin {
    @Inject(method = "createMicrophone", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectIOSMicrophone(CallbackInfoReturnable<Microphone> cir) throws MicrophoneException {
        if (!AudioQueueMicrophone.isAvailable()) return;
        String deviceName = null;
        try {
            Object config = Class.forName("de.maxhenkel.voicechat.VoicechatClient").getField("CLIENT_CONFIG").get(null);
            Object entry = config.getClass().getField("microphone").get(config);
            Object value = entry.getClass().getMethod("get").invoke(entry);
            if (value instanceof String s && !s.isBlank()) deviceName = s;
        } catch (Throwable ignored) {}
        int sampleRate = 48000;
        int bufferSize = 960;
        try {
            MicrophoneManager self = (MicrophoneManager)(Object) this;
            Field srField = self.getClass().getDeclaredField("sampleRate");
            srField.setAccessible(true);
            sampleRate = srField.getInt(self);
            Field bsField = self.getClass().getDeclaredField("bufferSize");
            bsField.setAccessible(true);
            bufferSize = bsField.getInt(self);
        } catch (Throwable ignored) {}
        AudioQueueMicrophone mic = new AudioQueueMicrophone(sampleRate, bufferSize, deviceName);
        mic.open();
        cir.setReturnValue(mic);
    }
}
'''

files['src/main/java/de/maxhenkel/voicechat/mixin/MicrophoneListMixin.java'] = '''package de.maxhenkel.voicechat.mixin;
import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = MicrophoneManager.class, remap = false)
public class MicrophoneListMixin {
    @Inject(method = "getAllMicrophones", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectIOSMicList(CallbackInfoReturnable<List<String>> cir) {
        if (!AudioQueueMicrophone.isAvailable()) return;
        cir.setReturnValue(AudioQueueMicrophone.getDeviceNames());
    }
}
'''

for path, content in files.items():
    dirpath = os.path.dirname(path)
    if dirpath:
        os.makedirs(dirpath, exist_ok=True)
    with open(path, 'w') as f:
        f.write(content)
    print(f"wrote {path}")

print("ALL FILES WRITTEN")