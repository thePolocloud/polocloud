repositories {
    maven {
        name = "buf"
        url = uri("https://buf.build/gen/maven")
    }
}

dependencies {
    implementation("build.buf.gen:minekube_gate_connectrpc_kotlin:0.7.1.1.20241118150055.50fffb007499")
    implementation("com.connectrpc:connect-kotlin:0.7.4")

    implementation(projects.sdk.sdkKotlin)
    implementation(projects.bridges.bridgeApi)
}