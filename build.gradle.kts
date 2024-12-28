import io.izzel.taboolib.gradle.*

plugins {
    java
    id("io.izzel.taboolib") version "2.0.21"
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
}

taboolib {
    description {
        contributors {
            name("坏黑")
        }
    }
    env {
        install(Basic, Bukkit, BukkitNMS, BukkitNMSUtil, BukkitUtil, Kether)
        install(CommandHelper, BukkitUI, BukkitNMSEntityAI)
        install(Database, DatabasePlayer)
    }
    version { taboolib = "6.2.1-df22fb1" }
    relocate("ink.ptms.um", "ink.ptms.sandalphon.um")
}

repositories {
    mavenCentral()
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
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}