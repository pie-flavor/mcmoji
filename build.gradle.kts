import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    kotlin("kapt") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("flavor.pie.promptsign") version "1.1.0"
    id("maven-publish")
}

group = "flavor.pie"
version = "1.2.0"

repositories {
    mavenCentral()
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io/")
    }
    maven {
        name = "bstats"
        url = uri("https://repo.codemc.org/repository/maven-public")
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

publishing {
    publications {
        create("sponge", MavenPublication::class.java) {
            project.shadow.component(this)
            pom {
                name.set("MCMoji")
                description.set("Emoji for Minecraft")
                url.set("https://ore.spongepowered.org/pie_flavor/mcmoji")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/pie-flavor/mcmoji/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("pie_flavor")
                        name.set("Adam Spofford")
                        email.set("aspofford.as@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/pie-flavor/mcmoji.git")
                    developerConnection.set("scm:git:ssh://github.com/pie-flavor/mcmoji.git")
                    url.set("https://github.com/pie-flavor/mcmoji")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://api.bintray.com/maven/pie-flavor/maven/mcmoji;publish=1")
            credentials {
                username = project.properties["bintrayUsername"].toString()
                password = project.properties["bintrayApiKey"].toString()
            }
        }
    }
}
