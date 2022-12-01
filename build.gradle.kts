plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "1.50"
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
}

taboolib {
    description {
        contributors {
            name("坏黑")
        }
    }
    install("common")
    install("common-5")
    install("module-ai")
    install("module-database")
    install("module-configuration")
    install("module-chat")
    install("module-nms")
    install("module-nms-util")
    install("module-ui")
    install("module-kether", "expansion-command-helper", "expansion-player-database")
    install("platform-bukkit")
    classifier = null
    version = "6.0.10-22"
    relocate("ink.ptms.um", "ink.ptms.chemdah.um")
    options("keep-kotlin-module")
}

repositories {
    mavenCentral()
}

dependencies {
    taboo("ink.ptms:um:1.0.0-beta-23")
    compileOnly("ink.ptms:Zaphkiel:2.0.14")
    compileOnly("ink.ptms:Adyeshach:1.5.13-op19")
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

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.tabooproject.org/storages/public/releases")
            credentials {
                username = project.findProperty("taboolibUsername").toString()
                password = project.findProperty("taboolibPassword").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = "ink.ptms"
        }
    }
}