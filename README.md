# WallSetter

A lightweight Android wallpaper utility that supports **manual image selection**, **system share flow**, and **broadcast-based automation**.

## Features

- **Manually pick and apply wallpapers**
  - Select an image inside the app
  - Preview before applying
  - Apply with one tap
- **Target selection**
  - Home + Lock (`both`)
  - Home only (`home`)
  - Lock only (`lock`)
- **Share images from other apps**
  - Share an image from Gallery/File Manager
  - Choose `WallSetter` to continue the wallpaper flow
- **Broadcast API for automation**
  - Send a broadcast intent with image path and target
- **Large image optimization**
  - Downsamples oversized images during decoding
  - Uses centered crop hint based on screen ratio to reduce memory pressure

## Usage

### 1) Manual setup

1. Open the app and tap **Select Image**.
2. Choose a target: `Home + Lock` / `Home only` / `Lock only`.
3. Tap **Set Wallpaper**.

### 2) Share from another app

1. Select an image in Gallery or File Manager.
2. Tap **Share**.
3. Choose `WallSetter`.
4. Confirm target and apply wallpaper.

### 3) Broadcast call (automation)

- **Action**: `top.sakari.wallsetter.SET_WALLPAPER`
- **Extras**:
  - `path`: absolute image path (**required**)
  - `target`: `home` / `lock` / `both` (optional, default is `both`)

Example:

- Action: `top.sakari.wallsetter.SET_WALLPAPER`
- Extras:
  - `path=/storage/emulated/0/Pictures/demo.jpg`
  - `target=both`

## Permissions

The app uses these permissions:

- `android.permission.SET_WALLPAPER`: apply system wallpaper
- `android.permission.READ_EXTERNAL_STORAGE`: compatibility for Android 12 and below
- `android.permission.READ_MEDIA_IMAGES`: image access on Android 13+

## Development

- **Stack**: Kotlin + Jetpack Compose + Material 3 + Coil
- **Min SDK**: API 28 (Android 9.0)
- **Target SDK**: API 36

## Notes

- For broadcast calls, make sure `path` exists and is readable; otherwise wallpaper setting will fail.
- Wallpaper cropping behavior can vary slightly across OEM implementations.
- For share flow, ensure MIME type is `image/*`.

---

If you want, I can further enhance this README for GitHub presentation (badges, screenshot section, extended FAQ, and changelog template).
