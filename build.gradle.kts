import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.3.31"
    kotlin("kapt") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("flavor.pie.promptsign") version "1.1.0"
}

group = "flavor.pie"
version = "1.1.0"

repositories {
    mavenCentral()
    maven {
        name = "sponge"
        url = URI("https://repo.spongepowered.org/maven/")
    }
    maven {
        name = "jitpack"
        url = URI("https://jitpack.io/")
    }
    maven {
        name = "bstats"
        url = URI("https://repo.codemc.org/repository/maven-public")
    }
}

val shadow by configurations.named("shadow")

dependencies {
    val kt = kotlin("stdlib-jdk8")
    shadow(kt)
    api(kt)
    val sponge = "org.spongepowered:spongeapi:7.1.0"
    kapt(sponge)
    api(sponge)
    val kludge = "com.github.pie-flavor:kludge:477392a"
    shadow(kludge)
    implementation(kludge)
    val bstats = "org.bstats:bstats-sponge-lite:1.4"
    shadow(bstats)
    implementation(bstats)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadow)
    archiveClassifier.set("")
    relocate("flavor.pie.kludge", "flavor.pie.mcmoji.util.kludge")
    relocate("kotlin", "flavor.pie.mcmoji.runtime.kotlin")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Task>("build") {
    dependsOn(tasks.named("shadowJar"))
}
