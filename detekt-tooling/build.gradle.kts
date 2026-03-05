plugins {
    id("module")
    id("public-api")
}

dependencies {
    api(projects.detektApi)
    api(libs.kotlin.compiler)
    testImplementation(libs.assertj.core)
}


