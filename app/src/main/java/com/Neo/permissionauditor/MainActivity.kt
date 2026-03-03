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
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    primary = Color(0xFF81C784), background = Color(0xFFF1F8E9), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8F5E9), onPrimary = Color.White, onBackground = Color(0xFF2E7D32),
    onSurface = Color(0xFF33691E), onSurfaceVariant = Color(0xFF558B2F)
)

private val AutoNightColors = darkColorScheme(
    primary = Color(0xFFFFB74D), background = Color(0xFF263238), surface = Color(0xFF37474F),
    surfaceVariant = Color(0xFF455A64), onPrimary = Color(0xFF263238), onBackground = Color(0xFFFFF3E0),
    onSurface = Color(0xFFFFF3E0), onSurfaceVariant = Color(0xFFFFCC80)
)

private val AutoTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 32.sp, letterSpacing = 6.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 20.sp, letterSpacing = 3.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp)
)

// --- 3. DYNAMIC BACKGROUND PAINTER WITH DESIGNS ---
@Composable
fun AestheticBackground(themePref: String, isDarkTheme: Boolean) {
    val bgColor = MaterialTheme.colorScheme.background

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (isDarkTheme) {
            val moonColor = if (themePref == "auto_time") Color(0xFFFFB74D) else Color(0xFFD1C4E9)
            drawCircle(color = moonColor.copy(alpha = 0.5f), radius = 5f, center = Offset(size.width * 0.15f, size.height * 0.1f))
            drawCircle(color = moonColor.copy(alpha = 0.3f), radius = 8f, center = Offset(size.width * 0.45f, size.height * 0.25f))
            drawCircle(color = moonColor.copy(alpha = 0.6f), radius = 4f, center = Offset(size.width * 0.85f, size.height * 0.4f))
            drawCircle(color = moonColor.copy(alpha = 0.4f), radius = 6f, center = Offset(size.width * 0.25f, size.height * 0.45f))
            drawCircle(color = moonColor.copy(alpha = 0.2f), radius = 10f, center = Offset(size.width * 0.7f, size.height * 0.15f))
            drawCircle(color = moonColor.copy(alpha = 0.15f), radius = 450f, center = Offset(size.width + 100f, -50f))
            drawCircle(color = bgColor, radius = 380f, center = Offset(size.width - 50f, 100f)) 
        } else {
            val sunColor = if (themePref == "auto_time") Color(0xFF81C784) else Color(0xFFFFCA28)
            val sunCenter = Offset(size.width + 100f, -100f)
            for (i in 0 until 12) {
                val angle = (i * 30f) * (PI / 180f)
                val startX = sunCenter.x + (380f * cos(angle)).toFloat()
                val startY = sunCenter.y + (380f * sin(angle)).toFloat()
                val endX = sunCenter.x + (550f * cos(angle)).toFloat()
                val endY = sunCenter.y + (550f * sin(angle)).toFloat()
                drawLine(color = sunColor.copy(alpha = 0.15f), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 50f, cap = StrokeCap.Round)
            }
            drawCircle(color = sunColor.copy(alpha = 0.1f), radius = 600f, center = sunCenter) 
            drawCircle(color = sunColor.copy(alpha = 0.25f), radius = 350f, center = sunCenter) 
        }
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("AuditorPrefs", Context.MODE_PRIVATE)

        setContent {
            var themePref by remember { mutableStateOf(sharedPrefs.getString("theme", "auto_time") ?: "auto_time") }
            
            // 1. Get the actual hour from the device
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDayTime = currentHour in 6..17 // 6 AM to 5:59 PM is Day
            
            // 2. Determine if we should show Dark or Light Theme
            val isDarkTheme = when (themePref) {
                "light" -> false
                "dark" -> true
                "auto_time" -> !isDayTime // True if it's night (before 6AM or after 6PM)
                else -> isSystemInDarkTheme()
            }

            // 3. Pick the Color Palette
            val colorScheme = when (themePref) {
                "light" -> PastelDayColors
                "dark" -> PastelNightColors
                "auto_time" -> if (isDayTime) AutoDayColors else AutoNightColors
                else -> if (isSystemInDarkTheme()) PastelNightColors else PastelDayColors
            }

            // 4. Pick the Typography
            val typography = if (themePref == "auto_time") AutoTypography else ManualTypography

            var isUnlocked by remember {
                val savedPin = sharedPrefs.getString("app_pin", "")
                mutableStateOf(savedPin.isNullOrEmpty()) 
            }

            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Pass the computed isDarkTheme variable directly to the background painter!
                        AestheticBackground(themePref = themePref, isDarkTheme = isDarkTheme)
                        
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
