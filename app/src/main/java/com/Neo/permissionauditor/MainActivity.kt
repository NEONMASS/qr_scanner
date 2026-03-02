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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.Neo.permissionauditor.ui.screens.AuditorScreen
import java.util.Calendar

// --- THEME ENGINE: Aesthetic Colors ---
private val AestheticLightColors = lightColorScheme(
    primary = Color(0xFF2563EB), // Royal Blue
    background = Color(0xFFF8FAFC), // Soft Pearl Slate
    surface = Color(0xFFFFFFFF), // Pure White Cards
    surfaceVariant = Color(0xFFF1F5F9), // Light Frost
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A), // Dark Slate Text
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

private val AestheticDarkColors = darkColorScheme(
    primary = Color(0xFF06B6D4), // Neon Cyan (Hacker vibe)
    background = Color(0xFF0B1120), // Deep Midnight Void
    surface = Color(0xFF111827), // Elevated Navy Cards
    surfaceVariant = Color(0xFF1F2937), // Dark Slate
    onPrimary = Color.Black,
    onBackground = Color(0xFFF8FAFC), // Crisp White Text
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

// --- THEME ENGINE: Aesthetic Typography ---
private val AestheticTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-1).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp),
    // Monospace for our security badges to make them look like code/terminal output!
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("AuditorPrefs", Context.MODE_PRIVATE)

        setContent {
            var themePref by remember { mutableStateOf(sharedPrefs.getString("theme", "auto_time") ?: "auto_time") }
            
            // --- THE CLOCK: Native Time Detection ---
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDayTime = currentHour in 6..17 // 6:00 AM to 5:59 PM is Day

            val isDarkTheme = when (themePref) {
                "light" -> false
                "dark" -> true
                "auto_time" -> !isDayTime // True if it's Night!
                else -> isSystemInDarkTheme() // "system" fallback
            }

            var isUnlocked by remember {
                val savedPin = sharedPrefs.getString("app_pin", "")
                mutableStateOf(savedPin.isNullOrEmpty()) 
            }

            // Apply our custom aesthetic themes!
            MaterialTheme(
                colorScheme = if (isDarkTheme) AestheticDarkColors else AestheticLightColors,
                typography = AestheticTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
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

@Composable
fun EnterPinScreen(correctPin: String, useBiometrics: Boolean, onUnlock: () -> Unit, activity: FragmentActivity) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val triggerBiometrics = {
        if (useBiometrics) {
            try {
                val executor = ContextCompat.getMainExecutor(activity)
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onUnlock()
                        }
                    })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Auditor PRO")
                    .setSubtitle("Use your fingerprint or enter your PIN")
                    .setNegativeButtonText("Use Custom PIN")
                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(Unit) { triggerBiometrics() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("App Locked", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        
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
                    onClick = { triggerBiometrics() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text("Biometrics") }
            }
            Button(
                onClick = { if (pinInput == correctPin) onUnlock() else { isError = true; pinInput = "" } },
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Unlock") }
        }
    }
}