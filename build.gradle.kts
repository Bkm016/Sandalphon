import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.31"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

taboolib {
    description {
        contributors {
            name("坏黑")
        }
        dependencies {
            name("DecentHolograms")
        }
    }
    env {
        install(Basic, Bukkit, BukkitNMS, BukkitNMSUtil, BukkitUtil, Kether)
        install(CommandHelper, BukkitUI, BukkitNMSEntityAI)
        install(BukkitNMSItemTag, BukkitNMSDataSerializer)
        install(BukkitHook, MinecraftChat, MinecraftEffect)
        install(Database, DatabasePlayer)
        install(I18n, JavaScript)
    }
    version { taboolib = "6.2.4-99fb800" }
    relocate("ink.ptms.um", "ink.ptms.sandalphon.um")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven {
        url = uri("https://maven.devs.beer/")
    }
}

dependencies {
    taboo("ink.ptms:um:1.0.0-beta-23")
    compileOnly("ink.ptms:Zaphkiel:2.0.14")
    compileOnly("ink.ptms.adyeshach:api:2.0.24")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v11701:11701-minimize:universal")
    compileOnly("ink.ptms.core:v11600:11600-minimize")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.7")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}