plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(project(":library"))
    implementation(project(":linux"))
}

tasks.register<Copy>("copyToLibs") {
    from(configurations.runtimeClasspath)
    into("$buildDir/libs")
}

var classPath = configurations.runtimeClasspath.get().joinToString(" ") { it.name }

tasks.jar {
    dependsOn("copyToLibs")
    manifest {
        attributes["Main-Class"] = "org.egility.chert.ChertMainKt"
        attributes["Class-Path"] = classPath
    }
}

