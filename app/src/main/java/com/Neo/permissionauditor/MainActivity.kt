package com.Neo.permissionauditor

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

// --- 1. MANUAL THEMES (Cream & Twilight) ---
private val PastelDayColors = lightColorScheme(
    primary = Color(0xFFE57373), background = Color(0xFFFFF9F5), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFEBE8), onPrimary = Color.White, onBackground = Color(0xFF4A4A4A),
    onSurface = Color(0xFF4A4A4A), onSurfaceVariant = Color(0xFF757575)
)

private val PastelNightColors = darkColorScheme(
    primary = Color(0xFFB39DDB), background = Color(0xFF1E1C2A), surface = Color(0xFF282534),
    surfaceVariant = Color(0xFF383447), onPrimary = Color(0xFF1E1C2A), onBackground = Color(0xFFEAE6F3),
    onSurface = Color(0xFFEAE6F3), onSurfaceVariant = Color(0xFFB0A8C0)
)

private val ManualTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

// --- 2. EXCLUSIVE AUTO THEMES (Matcha & Velvet) ---
private val AutoDayColors = lightColorScheme(
    primary = Color(0xFF81C784), // Pastel Mint Green
    background = Color(0xFFF1F8E9), // Morning Dew
    surface = Color(0xFFFFFFFF), // Pure White
    surfaceVariant = Color(0xFFE8F5E9), // Soft Mint Surface
    onPrimary = Color.White, onBackground = Color(0xFF2E7D32),
    onSurface = Color(0xFF33691E), onSurfaceVariant = Color(0xFF558B2F)
)

private val AutoNightColors = darkColorScheme(
    primary = Color(0xFFFFB74D), // Soft Golden Orange
    background = Color(0xFF263238), // Deep Velvet Blue-Grey
    surface = Color(0xFF37474F), // Elevated Velvet Cards
    surfaceVariant = Color(0xFF455A64), // Muted Teal-Grey
    onPrimary = Color(0xFF263238), onBackground = Color(0xFFFFF3E0),
    onSurface = Color(0xFFFFF3E0), onSurfaceVariant = Color(0xFFFFCC80)
)

private val AutoTypography = Typography(
    // Cursive/Stylized fonts exclusively for Auto mode to make it distinct!
    headlineMedium = TextStyle(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

// --- 3. DYNAMIC BACKGROUND PAINTER ---
@Composable
fun AestheticBackground(themePref: String, isDayTime: Boolean) {
    val bgColor = MaterialTheme.colorScheme.background
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        when {
            // AUTO NIGHT: Golden Moon
            themePref == "auto_time" && !isDayTime -> {
                drawCircle(color = Color(0xFFFFB74D).copy(alpha = 0.15f), radius = 450f, center = Offset(size.width + 100f, -50f))
                drawCircle(color = bgColor, radius = 380f, center = Offset(size.width - 50f, 100f)) // Cutout
            }
            // AUTO DAY: Morning Mint Sun
            themePref == "auto_time" && isDayTime -> {
                drawCircle(color = Color(0xFFAED581).copy(alpha = 0.2f), radius = 550f, center = Offset(size.width + 100f, -100f))
                drawCircle(color = Color(0xFF81C784).copy(alpha = 0.3f), radius = 350f, center = Offset(size.width + 100f, -100f))
            }
            // MANUAL DARK: Lavender Moon
            themePref == "dark" || (themePref == "system" && !isDayTime) -> {
                drawCircle(color = Color(0xFFD1C4E9).copy(alpha = 0.15f), radius = 450f, center = Offset(size.width + 100f, -50f))
                drawCircle(color = bgColor, radius = 380f, center = Offset(size.width - 50f, 100f)) // Cutout
            }
            // MANUAL LIGHT: Rose Sun
            else -> {
                drawCircle(color = Color(0xFFFFD54F).copy(alpha = 0.1f), radius = 550f, center = Offset(size.width + 100f, -100f))
                drawCircle(color = Color(0xFFFFCA28).copy(alpha = 0.2f), radius = 350f, center = Offset(size.width + 100f, -100f))
            }
        }
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("AuditorPrefs", Context.MODE_PRIVATE)

        setContent {
            var themePref by remember { mutableStateOf(sharedPrefs.getString("theme", "auto_time") ?: "auto_time") }
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDayTime = currentHour in 6..17 

            // Pick the Exact Color Palette
            val colorScheme = when (themePref) {
                "light" -> PastelDayColors
                "dark" -> PastelNightColors
                "auto_time" -> if (isDayTime) AutoDayColors else AutoNightColors
                else -> if (isSystemInDarkTheme()) PastelNightColors else PastelDayColors
            }

            // Pick the Exact Typography
            val typography = if (themePref == "auto_time") AutoTypography else ManualTypography

            var isUnlocked by remember {
                val savedPin = sharedPrefs.getString("app_pin", "")
                mutableStateOf(savedPin.isNullOrEmpty()) 
            }

            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Pass the exact theme so it draws the correct Sun/Moon!
                        AestheticBackground(themePref = themePref, isDayTime = isDayTime)
                        
                        if (isUnlocked) {
                            AuditorScreen(onThemeChange = { themePref = it })
                        } else {
                            val correctPin = sharedPrefs.getString("app_pin", "") ?: ""
                            val useBiometrics = sharedPrefs.getBoolean("use_biometrics", false)
                            EnterPinScreen(correctPin, useBiometrics, { isUnlocked = true }, this@MainActivity)
                        }
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("App Locked", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = pinInput, onValueChange = { isError = false; if (it.length <= 4) pinInput = it.filter { char -> char.isDigit() } },
            label = { Text("Enter PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), isError = isError, singleLine = true
        )
        
        if (isError) { Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (useBiometrics) {
                Button(onClick = { triggerBiometrics() }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Biometrics") }
            }
            Button(onClick = { if (pinInput == correctPin) onUnlock() else { isError = true; pinInput = "" } }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Unlock") }
        }
    }
}