plugins {
    java
    `groovy-gradle-plugin`

    id("com.gradle.plugin-publish") version "1.3.0"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


dependencies {
    implementation("com.google.code.gson:gson:2.11.0")

    compileOnly("org.apache.httpcomponents:httpclient:4.5.13")
    compileOnly("org.apache.httpcomponents:httpmime:4.5.13")

    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/Karlatemp/maven-central-publish")
    vcsUrl.set("https://github.com/Karlatemp/maven-central-publish")

    testSourceSets(sourceSets.test.get())

    plugins {
        register("maven-publishing") {
            id = "moe.karla.maven-publishing"
            implementationClass = "moe.karla.maven.publishing.MavenPublishingPlugin"

            displayName = "Maven Central Publishing"
            description = "Publishing your software to Maven Central"
            tags.set(listOf("signing", "publishing"))
        }
        register("publishing-signing") {
            id = "moe.karla.signing-setup"
            implementationClass = "moe.karla.maven.publishing.SigningSetupPlugin"

            displayName = "Gradle Signing Setup"
            description = "Fast setup your publications signing"
            tags.set(listOf("signing", "publishing"))
        }
    }
}