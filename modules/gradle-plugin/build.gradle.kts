plugins {
    java
    `groovy-gradle-plugin`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


dependencies {
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(gradleTestKit())
}

gradlePlugin {
    testSourceSets(sourceSets.test.get())

    plugins {
        register("maven-publishing") {
            id = "moe.karla.maven-publishing"
            implementationClass = "moe.karla.maven.publishing.MavenPublishingPlugin"
        }
        register("publishing-signing") {
            id = "moe.karla.signing-setup"
            implementationClass = "moe.karla.maven.publishing.SigningSetupPlugin"
        }
    }
}