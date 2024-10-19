# maven-center-publish

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

# Configure the plugin

## Recommended Configure

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

mavenPublishing {
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
   
      // Don't drop line breaks. A empty line is required for gradle to read the key
      "key": "-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n.....\n-----END PGP PRIVATE KEY BLOCK-----",
      "keyPassword": "The Password Of Your Key",
      // Optional. This field is optional. Used when you want to use a sub key to sign publications 
      "keyId": "Sub Key Id"
   }
   ```

4. (This step is only for validating signing setup) Open your user `gradle.properties`.
   Add `signing.setup.file=/path/to/your/data.json`.
   And then execute `gradle testSigning` in your project.

   > https://docs.gradle.org/current/userguide/build_environment.html

## CI Setup

1. Create an account and generate User Token on https://central.sonatype.com/account

   > https://central.sonatype.org/register/central-portal/

2. Goto your actions secrets settings.
3. Create a secret named `MAVEN_CENTER_PUBLISH_PGP` with the content of `data.json`
4. Create a secret named `MAVEN_CENTER_PUBLISH_ACCOUNT` with value `UserName:UserToken`
5. Change your workflow with
   ```yaml
   jobs:
     publish:
       runs-on: ubuntu-latest
       env:
         SIGNING_SETUP: ${{ secrets.MAVEN_CENTER_PUBLISH_PGP }}
         MAVEN_PUBLISH_PASSWORD: ${{ secrets.MAVEN_CENTER_PUBLISH_ACCOUNT }}
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

## Local Setup

Add following values to your `gradle.properties`

> [!IMPORTANT]
>
> Never add your secrets into the `gradle.proerties` in your project!
> You should add it in `GRADLE_USER_HOME/gradle.properties`
>
> https://docs.gradle.org/current/userguide/build_environment.html

```properties
signing.setup.file=/path/to/your/data.json
maven.publish.password=UserName:UserToken
```

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

