import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.5.2")
        classpath(kotlin("gradle-plugin", version = "1.3.60"))
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.60"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

allprojects {
    group = "egility.org"
    //version = "3.0.0"
    buildDir = File("/data/gradle_build/" + project.name)

    repositories {
        google()
        jcenter()
        maven(url = "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}