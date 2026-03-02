package com.Neo.permissionauditor

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.Neo.permissionauditor.ui.screens.AuditorScreen

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("AuditorPrefs", Context.MODE_PRIVATE)

        setContent {
            // NEW: Read the theme from settings dynamically
            var themePref by remember { mutableStateOf(sharedPrefs.getString("theme", "system") ?: "system") }
            
            val isDarkTheme = when (themePref) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // NEW: Check if the user opted to set a PIN
            var isUnlocked by remember {
                val savedPin = sharedPrefs.getString("app_pin", "")
                mutableStateOf(savedPin.isNullOrEmpty()) // Unlock immediately if no PIN exists
            }

            // Apply dynamic Theme
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        // Pass a callback so the Settings screen can instantly trigger a theme update
                        AuditorScreen(onThemeChange = { themePref = it })
                    } else {
                        val correctPin = sharedPrefs.getString("app_pin", "") ?: ""
                        val useBiometrics = sharedPrefs.getBoolean("use_biometrics", false)
                        
                        EnterPinScreen(
                            correctPin = correctPin,
                            useBiometrics = useBiometrics,
                            onUnlock = { isUnlocked = true },
                            activity = this@MainActivity
                        )
                    }
                }
            }
        }
    }
}

// The Lock Screen (Stays identical to before, just triggers IF a PIN exists)
@Composable
fun EnterPinScreen(correctPin: String, useBiometrics: Boolean, onUnlock: () -> Unit, activity: FragmentActivity) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

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
        
        if (isError) { Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (useBiometrics) {
                Button(
                    onClick = { try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { } },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Biometrics") }
            }
            Button(
                onClick = { if (pinInput == correctPin) onUnlock() else { isError = true; pinInput = "" } },
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Unlock") }
        }
    }
}