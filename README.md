# PS4 OWS — Offline Webkit Server

**PS4 OWS** is a lightweight Android application designed to host PS4 files locally from your device. It eliminates the need for an internet connection by turning your Android phone into a local HTTP server that your PS4 can access.

## Features

- **Local HTTP Server**: Hosts web files directly from your Android device.
- **Offline Functionality**: Works without internet data or external Wi-Fi (supports Hotspot mode).
- **Custom Toolkit Support**: Easily select and serve your own toolkit via ZIP file.
- **Real-time Monitoring**: Automatically detects and displays your current IP address and server status.
- **Privacy Focused**: No trackers, no analytics, no ads, and no cloud dependencies.

## Screenshots

<p align="center">
  <img src="screenshots/screenshot_online.jpg" width="300" title="Server Online">
  <img src="screenshots/screenshot_offline.jpg" width="300" title="Server Offline">
</p>

## Requirements

- **Android Version**: 7.0 (Nougat) or higher (API 24+).
- **Network**: Wi-Fi or Mobile Hotspot capability.

## How to Use

1. **Prepare Connection**: It is recommended to turn off Mobile Data and external Wi-Fi. 
2. **Connect**: Enable your Mobile Hotspot and connect your PS4 to it, or ensure both devices are on the same local network.
3. **Select Toolkit**: Tap the "Select Toolkit" field and pick your [ps4ows.zip](https://www.weebsu.com/p/ps4ows.html) file. This will extract the files needed for the server.
4. **Start Server**: Tap the **[Start Server]** button.
5. **Access on PS4**: 
   - Note the URL displayed (e.g., `http://192.168.x.x:8080`).
   - On your PS4, open the **Web Browser** or **User's Guide**.
   - Type in the URL provided by the app.

## Build from Source

You can build this project using Android Studio or via command line:

```bash
./gradlew assembleDebug
```

The output APK will be located in `app/build/outputs/apk/debug/`.

## Project Structure

- `app/src/main/java`: Kotlin source code (Server logic and UI).
- `app/src/main/res`: UI layouts and resources.

## Credits

- **PS4 Web Toolkits**: [rawgame4.github.io](https://rawgame4.github.io)
- **GoldHEN**: [Sistro](https://ko-fi.com/sistro)
- **HTTP Server**: [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

---
*Created by [youtube.com/@truecarbide](https://youtube.com/@truecarbide) | Powered by [weebsu.com](https://weebsu.com)*
