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
