# Simple Voice Chat iOS Patch

An experimental iOS / iPadOS patch for Simple Voice Chat on Fabric 1.21.11.

This project attempts to replace the default microphone handling with an iOS-compatible implementation.

## Scope

This project is currently focused only on:

- iOS / iPadOS  
- Fabric  
- Minecraft 1.21.11  

Everything else is not supported.

## Current Status

This does not work yet.  
The microphone implementation is still in progress and the mod will currently fail or crash when attempting to initialize audio.

However, the required pieces are being added and it is getting closer to working.

## What this patch aims to do

- Replace desktop microphone backend  
- Add iOS audio input support  
- Hook into Simple Voice Chat microphone creation  
- Allow microphone detection on iOS / iPadOS  

## Requirements

- Minecraft 1.21.11  
- Fabric Loader  
- Simple Voice Chat 2.6.x  
- iOS / iPadOS device  

## Download

Prebuilt jars are available in GitHub Actions.  
Download the latest successful workflow run.

## Notes

This is a patch, not a full port.  
Only microphone support for iOS is being worked on right now.

Issues and pull requests are welcome.