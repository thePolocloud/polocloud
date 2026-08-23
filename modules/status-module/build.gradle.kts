plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    // `compileOnly`, not `implementation`: at runtime these classes are loaded from the
    // node's own classpath, not bundled into this module's jar — ModuleClassLoader
    // delegates de.polocloud.moduleapi/api/shared/proto/common and kotlin(x).* straight
    // to the node's classloader (see node/.../module/ModuleClassLoader.kt), so shading
    // them in here would just create duplicate, incompatible class definitions.
    compileOnly(projects.moduleApi)
    compileOnly(libs.kotlinx.serialization.json)

    testImplementation(projects.moduleApi)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// A plain jar — deliberately not shaded (see the dependency comment above). Drop the
// output of `:modules:status-module:jar` straight into a node's `local/modules/`.
// The bundled `webroot/` resources (the optional status website) travel with it.
kotlin {
    jvmToolchain(25)
}
