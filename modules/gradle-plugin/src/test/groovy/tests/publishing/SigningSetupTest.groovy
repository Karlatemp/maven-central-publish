package tests.publishing

import com.google.gson.Gson
import moe.karla.maven.publishing.signsetup.SignSetupConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SigningSetupTest {
    public static final String TESTER_PRIVATE_KEY = """
-----BEGIN PGP PRIVATE KEY BLOCK-----

xcASBGcSuXYTBSuBBAAiAwMERiHVqcYuCPAyhUo475wsbglVfO75B208wAX+hW/C
DARyP8b5s902nuo0LAtUpsfFr3NvWYT1ilNyp/iOIQ4CNRIU6kVet1pRU+kV+RHC
hoqBGM+2VnYuX7aOYawlKZyV/gkDCPrqan+MvzVqYFWdR5icN9ftpJl70UIlpF3P
FzJ+6MRJfSMKjZV7ULK0VU/e7E1baOjuNlx7UUfKSmntQK9STPdwA0KntwfVQI2p
KtX7wf6d6ZzcTcgYLAnVp9Z6ulfZzRtUZXN0ZXIgPHRlc3RlckBleGFtcGxlLmNv
bT7CjwQTEwoAFwUCZxK5dgIbLwMLCQcDFQoIAh4BAheAAAoJEDsEJ73ePVhJBmgB
gIKWigmwTuGl971NifDWvhw57YG1TsvOY0YX9ews6UKR7tu5oEifrqGUEiPjydX9
mQGApwB1c3w1vh1F10Ic0tRXwBhQcrRgRHnuG5lIIikJmwav3Kx5W37BLK1mE/v+
RW37x6UEZxK5dhMIKoZIzj0DAQcCAwRpSnsw1FUElmqhH2tvcGOCOksa4eEC6ggy
8AZ3rIkpnXR7DTfWD1AAHwg1P950MW7jzRZsKYmGS1fyulpHQ5KI/gkDCCib6yLH
y34uYC8iX9aA2ql3DQXnxRY3eGxdrwqOTXc27Ob89BFyERiMij8fpGwAkd7XKFb1
aEt6BsSwEuPj6cDlxo4IT/fkWCHQAs9xH4TCwCcEGBMKAA8FAmcSuXYFCQ8JnAAC
Gy4AagkQOwQnvd49WElfIAQZEwoABgUCZxK5dgAKCRAOd/14upL0f0i1AQDoLl2F
GtGr9QLAuZVDLB/QkXDTMA9zv6FLN1suDqUIkwD9ErHSeV9l2CsLHKsyEjRCcnax
X0BGkfPNXo6pUhwnVZ7GNgF+P+d5PxOXzRfUbwon798YE1r+FRJ3zMsw9LiqKC60
gBDJgkPTO+X9lkNqUvfzExb6AYCcPb7//kVxA33b8uPddwLDcwsXXyTRnSV4ib83
Bx69dYLSrJEyO/5YVnsQYpSReGTHpQRnErl2EwgqhkjOPQMBBwIDBKiHxaGLiowz
3ezjcqbppTXeWJCeqcXqntIpbRxqpHDAt5eO6YozmsZqmL9Vb8FKOeCVfDEvPOUo
PKY7HzypbX3+CQMI2yp0DitZvR1gGzec03TGqlJI02QPC53lTHbe94ZtWOsXZ9M2
qLkJQCw0BpyKklMLTEGBWc66kK6LkTJloKFUnBznTRmJHk3vaOOJQXDmzsLAJwQY
EwoADwUCZxK5dgUJDwmcAAIbLgBqCRA7BCe93j1YSV8gBBkTCgAGBQJnErl2AAoJ
EK5HIofsYefV/c4A/0R61y03mXtwD5p/2Ka7Pkfh3Cbqmci6AWgavwDEZJHWAQDJ
U/dL2mYJV6QiLhkux/WiBOiHpNijd7JOlorD7rp9CAL5AYC1XAdeu0uS9BC3SO+J
rrzsyZJPqPqXNrgYD1PUb8dnQKqtR4eX/oniq5voDBHQKVsBgKmolb3rpjLT91a3
cermW8rb0eLpVD6EzZ/9vb++JddGm66qC3s3hSsza6EAvUuavA==
=OnwD

-----END PGP PRIVATE KEY BLOCK-----
""".trim()

    private static final String TESTER_KEY_PASSWORD = "Tester"

    public static String getConfig() {
        def configuration = new SignSetupConfiguration();
        configuration.key = TESTER_PRIVATE_KEY
        configuration.keyPassword = TESTER_KEY_PASSWORD

        return new Gson().toJson(configuration)
    }

    @Nested
    class Integration extends BaseIntegrationTest {

        @Test
        void testPluginApply() {
            mainScript << """
apply plugin: 'moe.karla.signing-setup'

if (tasks.findByName('testSigning') != null)
    throw new RuntimeException("Assertion Error: Signing should not be enabled")
"""
            runner().build()
        }

        @Test
        void testPluginApplyWithSigning() {
            mainScript << """
apply plugin: 'signing'
apply plugin: 'moe.karla.signing-setup'

if (tasks.findByName('testSigning') == null)
    throw new RuntimeException("Assertion Error: Plugin not applied")
"""
            runner().build()


            runner()
                    .withArguments('testSigning')
                    .buildAndFail()
        }

        @Test
        void testPluginApplyWithKey() {
            mainScript << """
apply plugin: 'moe.karla.signing-setup'
"""
            runner()
                    .withEnvironment(System.getenv() + [
                            'SIGNING_SETUP': getConfig(),
                    ])
                    .withArguments('testSigning')
                    .forwardOutput()
                    .build()
        }


        @Test
        void testPublishing() {
            settingsScript << """
rootProject.name = 'mptest'
"""
            mainScript << """
apply plugin: 'moe.karla.signing-setup'
apply plugin: 'maven-publish'
apply plugin: 'java'

group = 'moe.karla.mptest'
version = '1.0.0'

publishing {
    publishing {
        repositories {
            maven {
                name = 'TestRepo'
                url = '${projectDir.resolve('repo').toUri()}'
            }
        }
    }
    publications {
        main(MavenPublication) {
            from(project.components.java)
        }
    }
}
"""
            runner()
                    .withEnvironment(System.getenv() + [
                            'SIGNING_SETUP': getConfig(),
                    ])
                    .forwardOutput()
                    .withArguments('publishAllPublicationsToTestRepoRepository')
                    .build()

            Assertions.assertTrue(projectDir.resolve('repo/moe/karla/mptest/mptest/1.0.0/mptest-1.0.0.jar.asc').toFile().exists())
        }
    }
}
