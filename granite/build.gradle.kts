plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(28)

    signingConfigs {
        getByName("debug") {
            storeFile = file("/store/google/kotlin/sign/onyx.keystore")
            storePassword = "TheWrongTrousers1993"
            keyAlias = "scrime"
            keyPassword = "TheWrongTrousers1993"
        }
        create("release") {
            storeFile = file("/store/google/kotlin/sign/onyx.keystore")
            storePassword = "TheWrongTrousers1993"
            keyAlias = "scrime"
            keyPassword = "TheWrongTrousers1993"
        }
    }

    defaultConfig {
        applicationId ="org.egility.granite"
        minSdkVersion(17)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            //isMinifyEnabled = false
            //proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
    }

    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.60")
    implementation("com.android.support:appcompat-v7:28.0.0")
    //implementation("com.android.support:support-v4:28.0.0")
    implementation("com.android.support.constraint:constraint-layout:1.1.3")
    implementation("com.android.support:design:28.0.0")

    testImplementation("junit:junit:4.12")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
    implementation(project(":library"))
    implementation(project(":android"))
}

