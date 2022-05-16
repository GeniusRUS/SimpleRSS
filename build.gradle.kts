extra.set("compose_version", "1.2.0-beta01")

plugins {
    id("com.android.application") version "7.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.6.21" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.4.2" apply false
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}