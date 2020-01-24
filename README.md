# MCMoji

This is the source repository for the MCMoji Sponge plugin. The releases and the plugin readme can be found on [Ore](https://ore.spongepowered.org/pie_flavor/mcmoji/).

The resource pack used by the plugin can be found in the [`pack`](https://github.com/pie-flavor/mcmoji/tree/master/pack) folder. The pack is created using [Twemoji](https://github.com/twitter/twemoji/).

## Maven

MCMoji is hosted on Bintray JCenter. To depend on it, add the following to your `build.gradle.kts`:

```kotlin
repositories {
    jcenter()
}

dependencies {
    implementation("flavor.pie:mcmoji:1.2.0:all")
}
```

## Building

To build MCMoji from source:

```powershell
git clone 'https://github.com/pie-flavor/mcmoji.git'
cd mcmoji
.\gradlew build
```
