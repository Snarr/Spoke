// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = "1.5.0"
    // Compose @Composable functions are intentionally PascalCase; disable the rule here
    // (rather than only in .editorconfig) so it applies regardless of editorconfig discovery.
    val ktlintOverrides = mapOf("ktlint_standard_function-naming" to "disabled")
    kotlin {
        target("app/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintOverrides)
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion).editorConfigOverride(ktlintOverrides)
    }
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom(files("app/src/main/java", "app/src/test/java", "app/src/androidTest/java"))
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}
