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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gochathub.gochathubclient.Auth
import com.gochathub.gochathubclient.R
import kotlinx.coroutines.launch

@Composable
public fun LoginScreen(onLoggedIn: () -> Unit) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    Auth.login(serverUrl.trim().removeSuffix("/"), username.trim(), password)
                        .onSuccess { onLoggedIn() }
                        .onFailure { error = it.message }
                    busy = false
                }
            },
            enabled = !busy && username.isNotBlank() && password.isNotBlank() && serverUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) CircularProgressIndicator() else Text("Sign in", style = MaterialTheme.typography.labelLarge)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}