# maven-central-publish

Configure publication to Maven Central repository for Gradle projects with minimal effort.

# Using this plugin

```groovy
plugins {
    // Full version of maven-center-publish
    id "moe.karla.maven-publishing"
    // Or else if you just want to sign up your publications
    id "moe.karla.signing-setup"
}
```

Goto [Gradle Plugin Portal](https://plugins.gradle.org/plugin/moe.karla.maven-publishing) to view the latest version.

# Configure the plugin

> [!TIP]
>
> You don't need to configure the plugin in `build.gradle`
> if you used `moe.karla.signing-setup` only.
>
> Just goto the `GPG Setup` section and setup GPG configurations.

## Recommended Configure

```groovy
plugins {
    id 'java'
    id 'moe.karla.maven-publishing'
    id 'maven-publish'
}

group = 'moe.karla.mptest'
version = '1.0.0'
description = 'The Example Project. Project description is included in maven pom.'

repositories {
    mavenCentral()
}

mavenPublishing {
    // Add This line if you want to verify your component before publishing.
    publishingType = moe.karla.maven.publishing.MavenPublishingExtension.PublishingType.USER_MANAGED

    url = 'https://github.com/YourUserName/YourProject'
    developer('YourUserName', 'user@example.com')
}

publishing {
    publications {
        main(MavenPublication) {
            from(project.components.java)
        }
    }
}
```

## Minimal Configure

```groovy
plugins {
    id 'java'
    id 'moe.karla.maven-publishing'
    id 'maven-publish'
}

group = 'moe.karla.mptest'
version = '1.0.0'

repositories {
    mavenCentral()
}

publishing {
    publications {
        main(MavenPublication) {
            from(project.components.java)
        }
    }
}
```

## Manually Configure

> This section is for people who want to set up maven pom manually

```groovy
plugins {
    id 'java'
    id 'moe.karla.maven-publishing'
    id 'maven-publish'
}

mavenPublishing {
    // Add This line if you want to verify your component before publishing.
    publishingType = moe.karla.maven.publishing.MavenPublishingExtension.PublishingType.USER_MANAGED
    manuallyPomSetup = true
}

publishing {
    publications {
        main(MavenPublication) {
            from(project.components.java)

            pom {
                // .....
            }
        }
    }
}


```

## GPG Setup

> [!IMPORTANT]
> Based on Sonatype's requirements, you must set up GPG to push your software to Maven Central.
>
> For GPG Installation and key generation, see https://central.sonatype.org/publish/requirements/gpg/

1. Type `gpg --list-secret-keys` to confirm the sign key you want to use.

    ```text
    C:\Users\karlatemp>gpg --list-secret-keys
    /c/Users/karlatemp/.gnupg/pubring.kbx
    -------------------------------------
    sec   ed25519 2024-10-18 [SC]
          C369D088F3CDAD2C7759D03A281BC3003E644D8C  ## This is key id
    uid           [ultimate] Tester <tester@example.com>
    ssb   cv25519 2024-10-18 [E]
    ```

2. Type `gpg --armor --export-secret-key tester@example.com` or `gpg --armor --export-secret-key KEYID`

    ```text
    C:\Users\karlatemp>gpg --armor --export-secret-key tester@example.com
    -----BEGIN PGP PRIVATE KEY BLOCK-----
    
    ........................
    -----END PGP PRIVATE KEY BLOCK-----
    ```

3. Create a file named `data.json` (or any name you want). Fill with following content.

   ```json5
   {
      // NOTE: Don't write comments into your file.
   
      // Don't drop line breaks. An empty line is required for gradle to read the key
      "key": "-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n.....\n-----END PGP PRIVATE KEY BLOCK-----",
      // Optional. When the key has no password, remove this field
      "keyPassword": "The Password Of Your Key",
      // Optional. This field is optional. Used when you want to use a sub key to sign publications 
      "keyId": "Sub Key Id"
   }
   ```

   You can also use the provided script <code>[bash key-export.sh](./key-export.sh)</code> to generate this content.

4. (This step is only for validating signing setup) Open your user `gradle.properties`.
   Add `signing.setup.file=/path/to/your/data.json`.
   And then execute `gradle testSigning` in your project.

   > https://docs.gradle.org/current/userguide/build_environment.html

## CI Setup

1. Create an account and generate User Token on https://central.sonatype.com/account

   > https://central.sonatype.org/register/central-portal/

2. Goto your actions secrets settings.
3. Create a secret named `MAVEN_CENTRAL_PUBLISH_GPG` with the content of `data.json`
4. Create a secret named `MAVEN_CENTRAL_PUBLISH_ACCOUNT` with value `UserName:UserToken`
5. Change your workflow with
   ```yaml
   jobs:
     publish:
       runs-on: ubuntu-latest
       env:
         SIGNING_SETUP: ${{ secrets.MAVEN_CENTRAL_PUBLISH_GPG }}
         MAVEN_PUBLISH_PASSWORD: ${{ secrets.MAVEN_CENTRAL_PUBLISH_ACCOUNT }}
       steps:
         - uses: actions/checkout@v4
         - name: Set up JDK 21
           uses: actions/setup-java@v4
           with:
             java-version: '21'
             distribution: 'zulu'
   
         # Configure Gradle for optimal use in GitHub Actions, including caching of downloaded dependencies.
         # See: https://github.com/gradle/actions/blob/main/setup-gradle/README.md
         - name: Setup Gradle
           uses: gradle/actions/setup-gradle@417ae3ccd767c252f5661f1ace9f835f9654f2b5 # v3.1.0
           with:
             gradle-version: wrapper

         - run: gradle cleanMavenPublishingStage build publishAllPublicationsToMavenStageRepository
         # - run: gradle packMavenPublishingStage
         - run: gradle publishToMavenCentral
   ```
   In addition, you can specify the username and password separately for other scenarios where you need to separate the
   username and password.
   ```yaml 
   jobs:
     publish:
     runs-on: ubuntu-latest
     env:
       SIGNING_SETUP: ${{ secrets.MAVEN_CENTRAL_PUBLISH_GPG }}
       MAVEN_PUBLISH_USER:     ${{ secrets.MAVEN_CENTRAL_PUBLISH_USER }}
       MAVEN_PUBLISH_PASSWORD: ${{ secrets.MAVEN_CENTRAL_PUBLISH_TOKEN }}
     steps:
       - ...
   ```

## Local Setup

Add following values to your `gradle.properties`

> [!IMPORTANT]
>
> Never add your secrets into the `gradle.proerties` in your project!
> You should add it in `GRADLE_USER_HOME/gradle.properties`
>
> https://docs.gradle.org/current/userguide/build_environment.html

```properties
# Maven Central Setup
maven.publish.password=UserName:UserToken
# Local GPG Setup
# Way 1: Setting up using CI configuration
signing.setup.file=/path/to/your/data.json
# Way 2: Setting up using local GPG command
#
# When you choose to use local GPG command to sign, you must set up the gradle signing settings manually.
# See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent
#
# NOTE: This option has the highest priority.
# When you're testing your CI configuration. Run with following command:
# ./gradlew testSigning -Psigning.manually=false
signing.manually=true
```

### The `signing.manually` property

The `signing.manually` property control how maven-publishing setup GPG signing.

You can define this behavior in the project level options via
creating `gradle.properties` in your root project.

Options can be separated by `,` to reference multiple options at once.

| Flag Name | Description                                                                                    |
|:----------|:-----------------------------------------------------------------------------------------------|
| `notest`  | Disable registering `testSigning` task                                                         |
| `nouse`   | Don't setup the signing extension <br/> Useful when you already setup the signing by yourself. |
| `nopub`   | Disable automatic registering publications to the signing extension                            |
| `gpgcmd`  | Use gpg agent. No effect if `nouse` applied.                                                   |
| ---       |                                                                                                |
| `true`    | Alias of `gpgcmd`                                                                              |
| `all`     | Alias of `notest,nouse,nopub`                                                                  |

## Registered Tasks

### `gradle cleanMavenPublishingStage`

Cleanup the local temporary staging repository.

> You need to execute this task when you locally publish your software.
>
> This task well be executed when `gradle clean` executed.

### `gradle publishAllPublicationsToMavenStageRepository`

Publish all publications to the local staging repository.

### `gradle publishToMavenCentral`

Publish everything in stage repository to maven central.

### `gradle testSigning`

Test the signing configuration.

## Multi-Module Configure

1. Apply `moe.karla.maven-publishing` to the root `build.gradle` and only the root script.
2. Setup `mavenPublishing` in the root project.
3. Apply `maven-publish` and configure publication on the modules you want to publish.

## Android Library

```diff
 publishing {
   publications {
     release(MavenPublication) {
       groupId = 'com.my-company'
       artifactId = 'my-library'
       version = '1.0'

-      from(project.components.java)

+      afterEvaluate {
+        from(components.release)
+      }
     }
   }
 }
```

https://developer.android.com/build/publish-library/upload-library#create-pub

## Kotlin Multiplatform

```toml
## rootProject/gradle/libs.versions.toml

[libraries]
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }


mavenPublishing = { id = "moe.karla.maven-publishing", version = "1.3.1" }
```

```kotlin
// rootProject/build.gradle.kts

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    // id("moe.karla.maven-publishing")
    alias(libs.plugins.mavenPublishing)
}

mavenPublishing {
    // Add This line if you want to verify your component before publishing.
    publishingType = moe.karla.maven.publishing.MavenPublishingExtension.PublishingType.USER_MANAGED
}
```

```kotlin
// rootProject/library/build.gradle.kts


plugins {
   alias(libs.plugins.kotlinMultiplatform)
   alias(libs.plugins.androidLibrary)
   `maven-publish`
}

group = "moe.karla.mptest"
version = "1.0.0"

kotlin {
   // ...
}


// NOTE:
//      You don't need to configure the `publishing` extension.
//      Everything you need was already completed by kotlin-multiplatform and the maven-publishing plugin
//
// Technology:
//      publications: 
//          Publications are registered automatically when you applied
//          kotlinMultiplatform and `maven-publish`
//      the `-sources.jar`:
//          The sources jars are automatically registered by kotlinMultiplatform
//      the `-javadoc.jar`:
//          An empty stub javadoc jar will be attached to all available publications by
//          maven-publishing plugin to pass central publishing rule
//      the .asc sign files:
//          maven-publishing plugin will register all publications to the signing extension
//      the .pom:
//          maven-publishing will fill all necessary fields to all publication pom files
//publishing {
//}
```
