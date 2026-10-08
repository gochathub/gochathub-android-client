package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.core.hub.HubApiException
import com.cometchat.uikit.core.hub.MobileSignIn
import com.gochathub.gochathubclient.Auth
import com.gochathub.gochathubclient.R
import kotlinx.coroutines.launch

@Composable
public fun LoginScreen(onLoggedIn: () -> Unit) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var apiToken by remember { mutableStateOf("") } // a credential: kept out of saved instance state
    var useToken by rememberSaveable { mutableStateOf(true) }
    var scanning by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    // Not saveable: the 2FA challenge is a short-lived credential; rotation just restarts at the password step.
    var challenge by remember { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (scanning) {
        QrScanScreen(
            onScanned = { text ->
                scanning = false
                val creds = MobileSignIn.parse(text)
                if (creds == null) {
                    error = "That QR code is not a goChatHub sign-in code."
                } else {
                    serverUrl = creds.server
                    busy = true
                    error = null
                    scope.launch {
                        Auth.loginWithToken(creds.server, creds.token)
                            .onSuccess { onLoggedIn() }
                            .onFailure { error = it.message }
                        busy = false
                    }
                }
            },
            onCancel = { scanning = false }
        )
        return
    }

    val ready = serverUrl.isNotBlank() && when {
        challenge != null -> code.isNotBlank()
        useToken -> apiToken.isNotBlank()
        else -> username.isNotBlank() && password.isNotBlank()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.gochathub_login),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(96.dp)
        )
        Spacer(Modifier.height(32.dp))

        LoginField(serverUrl, { serverUrl = it }, "Server URL")
        Spacer(Modifier.height(12.dp))
        val pending = challenge
        when {
            pending != null -> LoginField(code, { code = it }, "Authenticator or backup code")
            useToken -> {
                OutlinedButton(
                    onClick = { scanning = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan QR code") }
                Spacer(Modifier.height(12.dp))
                LoginField(apiToken, { apiToken = it }, "API token", secret = true)
            }
            else -> {
                LoginField(username, { username = it }, "Username")
                Spacer(Modifier.height(12.dp))
                LoginField(password, { password = it }, "Password", secret = true)
            }
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    val url = serverUrl.trim().removeSuffix("/")
                    val result = when {
                        pending != null -> Auth.login2fa(pending, code.trim())
                        useToken -> Auth.loginWithToken(url, apiToken.trim())
                        else -> Auth.login(url, username.trim(), password)
                    }
                    result.onSuccess { onLoggedIn() }
                        .onFailure { e ->
                            val needs2fa = (e as? HubApiException)?.takeIf { it.code == "two_factor_required" }
                            if (needs2fa?.challenge != null) {
                                challenge = needs2fa.challenge
                                code = ""
                            } else {
                                error = e.message
                            }
                        }
                    busy = false
                }
            },
            enabled = !busy && ready,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) CircularProgressIndicator()
            else Text(if (pending != null) "Verify" else "Sign in", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(
            onClick = {
                error = null
                if (pending != null) challenge = null else useToken = !useToken
            },
            enabled = !busy
        ) {
            Text(
                when {
                    pending != null -> "Back"
                    useToken -> "Use username and password"
                    else -> "Use an API token instead"
                }
            )
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LoginField(value: String, onChange: (String) -> Unit, label: String, secret: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (secret) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth()
    )
}
