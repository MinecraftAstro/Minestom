import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("minestom.java-binary")
}

dependencies {
    implementation(rootProject)

    runtimeOnly(libs.bundles.logback)
}

application {
    mainClass.set("net.minestom.demo.Main")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("minestom-demo.jar")
}

val deployJar by tasks.registering(Copy::class) {
    val shadowJarTask = tasks.named<ShadowJar>("shadowJar")

    dependsOn(shadowJarTask)

    from(shadowJarTask.flatMap { it.archiveFile })
    into(file("${System.getProperty("user.home")}${File.separator}Desktop${File.separator}MinestomServer"))
    //into(file("${System.getProperty("user.home")}${File.separator}OneDrive${File.separator}Desktop${File.separator}TestingServer"))
}