package com.Neo.permissionauditor

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.Neo.permissionauditor.ui.screens.AuditorScreen

enum class VaultState { SETUP, LOCKED, UNLOCKED }

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("AuditorPrefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                var appState by remember {
                    mutableStateOf(
                        when (sharedPrefs.getString("app_pin", null)) {
                            null -> VaultState.SETUP
                            "skipped" -> VaultState.UNLOCKED
                            else -> VaultState.LOCKED
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (appState) {
                        VaultState.UNLOCKED -> {
                            AuditorScreen()
                        }
                        VaultState.SETUP -> {
                            SetupPinScreen(
                                onPinSet = { pin, useBiometrics ->
                                    sharedPrefs.edit()
                                        .putString("app_pin", pin)
                                        .putBoolean("use_biometrics", useBiometrics)
                                        .apply()
                                    appState = VaultState.UNLOCKED
                                },
                                onSkip = {
                                    sharedPrefs.edit().putString("app_pin", "skipped").apply()
                                    appState = VaultState.UNLOCKED
                                }
                            )
                        }
                        VaultState.LOCKED -> {
                            val correctPin = sharedPrefs.getString("app_pin", "") ?: ""
                            val useBiometrics = sharedPrefs.getBoolean("use_biometrics", false)
                            
                            EnterPinScreen(
                                correctPin = correctPin,
                                useBiometrics = useBiometrics,
                                onUnlock = { appState = VaultState.UNLOCKED },
                                activity = this@MainActivity
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPinScreen(onPinSet: (String, Boolean) -> Unit, onSkip: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // Check if the hardware actually supports biometrics
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember { biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS }
    var useBiometric by remember { mutableStateOf(canAuthenticate) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Secure Your App", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Create a 4-digit PIN to lock Auditor PRO.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 4) pinInput = it.filter { char -> char.isDigit() } },
            label = { Text("Enter 4-Digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Only show the biometric option if the phone supports it!
        if (canAuthenticate) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Fingerprint Unlock")
                Switch(checked = useBiometric, onCheckedChange = { useBiometric = it })
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Button(
            onClick = { 
                if (pinInput.length == 4) onPinSet(pinInput, useBiometric) 
                else Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Set Security Lock")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onSkip) {
            Text("Skip, don't lock the app")
        }
    }
}

@Composable
fun EnterPinScreen(correctPin: String, useBiometrics: Boolean, onUnlock: () -> Unit, activity: FragmentActivity) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Setup the Biometric logic
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = remember {
        BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock()
                }
            })
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Auditor PRO")
            .setSubtitle("Use your fingerprint or enter your PIN")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    // Trigger fingerprint scanner automatically if they enabled it
    LaunchedEffect(Unit) {
        if (useBiometrics) {
            try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("App Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = pinInput,
            onValueChange = { 
                isError = false
                if (it.length <= 4) pinInput = it.filter { char -> char.isDigit() } 
            },
            label = { Text("Enter PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            isError = isError,
            singleLine = true
        )
        
        if (isError) {
            Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            // Re-trigger biometric button just in case they cancelled it earlier
            if (useBiometrics) {
                IconButton(
                    onClick = { try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { } },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Use Fingerprint", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Button(
                onClick = { 
                    if (pinInput == correctPin) onUnlock() 
                    else { isError = true; pinInput = "" }
                },
                modifier = Modifier.weight(1f).padding(start = if (useBiometrics) 16.dp else 0.dp).height(50.dp)
            ) {
                Text("Unlock")
            }
        }
    }
}