package com.Neo.permissionauditor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.Neo.permissionauditor.ui.screens.AuditorScreen

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // THE FIX: Using standard MaterialTheme directly!
            MaterialTheme {
                // State to track if the vault is unlocked
                var isUnlocked by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        // SUCCESS! Load the actual app.
                        AuditorScreen()
                    } else {
                        // LOCKED! Show the security screen.
                        VaultLockScreen(onUnlock = { isUnlocked = true })
                    }
                }
            }
        }
    }

    @Composable
    fun VaultLockScreen(onUnlock: () -> Unit) {
        // Setup the Biometric engine
        val executor = ContextCompat.getMainExecutor(this@MainActivity)
        val biometricPrompt = remember {
            BiometricPrompt(this@MainActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onUnlock() // Grant access!
                    }
                })
        }

        // Configure the prompt dialogue
        val promptInfo = remember {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Auditor PRO Vault")
                .setSubtitle("Authenticate to access privacy data")
                // Allow Fingerprint, Face, or device PIN/Pattern fallback
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
        }

        // Automatically trigger the prompt the second the app opens
        LaunchedEffect(Unit) {
            biometricPrompt.authenticate(promptInfo)
        }

        // The background UI behind the prompt (what you see if you hit "cancel")
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock, 
                contentDescription = "Locked", 
                modifier = Modifier.size(100.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Vault is Locked", 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { biometricPrompt.authenticate(promptInfo) },
                modifier = Modifier.height(50.dp)
            ) {
                Text("Tap to Unlock")
            }
        }
    }
}