# MoonAeroAndroid: tema Frutiger Aero para Moonlight Android

Cristal brillante, degradados aqua, sol, colinas y burbujas en las pantallas de menú (lista de PCs,
lista de apps, ajustes, añadir PC). El streaming no se toca.

## Personalizar
Ajustes → **Appearance (Frutiger Aero)**; los cambios se aplican al instante:
tema on/off (apagado = aspecto original), preset (Aqua, Bliss, Sunset, Aurora, Custom),
color propio (paleta de 10 o hex `#rrggbb`), opacidad del cristal (10-90 %) y burbujas.

## Compilar
Herramientas: **Android Studio** (JDK 17 incluido), Android **SDK 37**, **NDK 29.0.14206865**
(SDK Manager → SDK Tools → NDK Side by side).

```bash
git clone --recursive https://github.com/WriesArtz/MoonAeroAndroid.git
cd MoonAeroAndroid
git checkout frutiger-aero
```
Crea `local.properties` en la raíz:
```properties
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk
ndk.dir=/Users/TU_USUARIO/Library/Android/sdk/ndk/29.0.14206865
```
```bash
./gradlew assembleNonRootDebug
adb install -r app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk
```
(o abre el proyecto en Android Studio → Build → Build APK). Un APK release necesita tu propia firma.

## Estado
Recursos compilados con `aapt`, paquete `com.limelight.aero` compilado sin avisos y sin errores nuevos
en los archivos modificados. No se ha ejecutado en un dispositivo: pendiente de prueba visual (táctil y mando/TV).
Los diálogos del sistema conservan el estilo Material estándar.

## Liquid glass and wallpaper (v2)

**Liquid glass.** Tiles, toolbar orbs and the settings pane now show a *blurred copy of the background*
through a generously rounded, softly tinted pane, with a specular rim (bright top-left / bottom-right,
faint along the sides), a soft sheen with no hard gloss band, an inner glow, diffuse shadow and a glow
for hover / selection. The glass is springy: it grows slightly when hovered or selected (TV remote,
keyboard, mouse) and squashes when pressed, then settles with a small overshoot. Settings → Appearance →
**Glass blur** (0–100 %) controls the frost; 0 gives plain translucent glass. The blur is a tiny
(~190 px) pre-blurred render of the background, so it works on every Android version (no RenderEffect /
RenderScript) and costs almost nothing per tile.

**Wallpaper.** Settings → Appearance → *Wallpaper (image, GIF or video)* opens the system file picker
(any image or video). It replaces the sky, is cropped to fill the screen and can be dimmed (0–80 %).
GIF / animated WebP animate on Android 9+ (older versions show the first frame); videos loop silently.
It pauses whenever the window loses focus (so it is idle while streaming) and falls back to the sky if
the file cannot be read. The glass blur uses the wallpaper's first frame. *Remove wallpaper* goes back to
the sky. Access to the chosen file is kept with a persistable URI permission.

New preference keys: `seekbar_aero_blur`, `edit_aero_wallpaper`, `seekbar_aero_wallpaper_dim`.
