plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

/**
 * Contract drift guard: the snapshot must byte-match the server's live
 * contract. Server-repo-relative when available; CI fetches from GitHub.
 */
tasks.register("checkContract") {
    val snapshot = layout.projectDirectory.file("api/openapi.yaml")
    val serverContract = layout.projectDirectory
        .file("../gochatserver/api/openapi.yaml")
    doLast {
        if (!snapshot.asFile.exists()) throw GradleException("api/openapi.yaml missing")
        val server = serverContract.asFile
        if (server.exists()) {
            if (!server.readBytes().contentEquals(snapshot.asFile.readBytes())) {
                throw GradleException(
                    "Contract drift: api/openapi.yaml differs from ../gochatserver/api/openapi.yaml\n" +
                        "Run: cp ../gochatserver/api/openapi.yaml api/openapi.yaml (then diff client work)"
                )
            }
        } else {
            logger.lifecycle("checkContract: server repo not present; CI should fetch the live contract and diff")
        }
    }
}