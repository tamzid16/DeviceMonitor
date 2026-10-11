package com.omega.devicemonitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omega.devicemonitor.ui.components.AVATAR_COLORS
import com.omega.devicemonitor.ui.theme.CyberCyan
import com.omega.devicemonitor.ui.theme.CyberCyanDark
import com.omega.devicemonitor.ui.theme.NeonEmerald
import com.omega.devicemonitor.ui.theme.TechAmber
import com.omega.devicemonitor.ui.theme.TechCoral
import com.omega.devicemonitor.ui.theme.TechPurple
import com.omega.devicemonitor.ui.viewmodel.DeviceMonitorViewModel

@Composable
fun AuthScreen(
    viewModel: DeviceMonitorViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Register

    // Sign in form state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register form state
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerConfirmPassword by remember { mutableStateOf("") }
    var registerPasswordVisible by remember { mutableStateOf(false) }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }

    // Dialog & Feedback states
    var showHelpDialog by remember { mutableStateOf(false) }
    var localValidationMessage by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // High-tech atmospheric background
        AuthAtmosphericBackground()

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. BRAND HERO HEADER
            BrandHeroHeader()

            Spacer(modifier = Modifier.height(26.dp))

            // 2. SLIDING SEGMENTED TAB CONTROLLER
            AuthSegmentedTab(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    viewModel.clearAuthError()
                    localValidationMessage = null
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. ERROR & VALIDATION BANNER
            AnimatedVisibility(
                visible = authError != null || localValidationMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = localValidationMessage ?: authError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. MAIN INTERACTIVE FORM CARD
            ElevatedCard(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (selectedTab == 0) {
                        // ==================== SIGN IN SECTION ====================
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Authenticate to access your telemetry nodes and live sensors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                        )

                        // Email Field
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = {
                                loginEmail = it
                                viewModel.clearAuthError()
                                localValidationMessage = null
                            },
                            label = { Text("Operator Email") },
                            placeholder = { Text("operator@domain.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (loginEmail.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                viewModel.clearAuthError()
                                localValidationMessage = null
                            },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (loginPassword.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (loginPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (loginEmail.isNotBlank() && loginPassword.isNotBlank()) {
                                        viewModel.login(loginEmail, loginPassword)
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showHelpDialog = true }) {
                                Text(
                                    text = "Access Help?",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Sign In Button
                        AuthGradientButton(
                            text = "Connect to Gateway",
                            isLoading = isLoading,
                            onClick = {
                                if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                    localValidationMessage = "Please enter both email and password."
                                } else {
                                    viewModel.login(loginEmail, loginPassword)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Mode Switcher Prompt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "First time setup? ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Create Operator Profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedTab = 1
                                        viewModel.clearAuthError()
                                        localValidationMessage = null
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                    } else {
                        // ==================== REGISTER SECTION ====================
                        Text(
                            text = "Create Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Initialize an isolated local account with private sensor storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                        )

                        // Full Name
                        OutlinedTextField(
                            value = registerName,
                            onValueChange = {
                                registerName = it
                                localValidationMessage = null
                            },
                            label = { Text("Operator Name") },
                            placeholder = { Text("e.g. Alex Rivera") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (registerName.isNotEmpty()) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                focusedLabelColor = NeonEmerald,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Address
                        OutlinedTextField(
                            value = registerEmail,
                            onValueChange = {
                                registerEmail = it
                                localValidationMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("operator@domain.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (registerEmail.isNotEmpty()) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                focusedLabelColor = NeonEmerald,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password
                        OutlinedTextField(
                            value = registerPassword,
                            onValueChange = {
                                registerPassword = it
                                localValidationMessage = null
                            },
                            label = { Text("Master Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (registerPassword.isNotEmpty()) NeonEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { registerPasswordVisible = !registerPasswordVisible }) {
                                    Icon(
                                        imageVector = if (registerPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (registerPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (registerPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonEmerald,
                                focusedLabelColor = NeonEmerald,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input")
                        )

                        // Live Password Strength Meter
                        if (registerPassword.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PasswordStrengthIndicator(password = registerPassword)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = registerConfirmPassword,
                            onValueChange = {
                                registerConfirmPassword = it
                                localValidationMessage = null
                            },
                            label = { Text("Confirm Master Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (registerConfirmPassword.isNotEmpty()) {
                                        if (registerPassword == registerConfirmPassword) NeonEmerald else TechCoral
                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (registerConfirmPassword.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (registerPassword == registerConfirmPassword) Icons.Default.Check
                                        else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (registerPassword == registerConfirmPassword) NeonEmerald else TechCoral
                                    )
                                }
                            },
                            visualTransformation = if (registerPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (registerPassword == registerConfirmPassword) NeonEmerald else TechCoral,
                                focusedLabelColor = NeonEmerald,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Visual Avatar Accent Selector
                        Text(
                            text = "Profile Accent Tint",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Used to identify this account across multi-device logs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AVATAR_COLORS.forEachIndexed { index, color ->
                                val isSelected = selectedAvatarIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarIndex = index }
                                        .shadow(if (isSelected) 6.dp else 0.dp, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected accent",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Register Submit Button
                        AuthGradientButton(
                            text = "Initialize Operator Account",
                            isLoading = isLoading,
                            onClick = {
                                when {
                                    registerName.isBlank() -> localValidationMessage = "Please enter your operator name."
                                    registerEmail.isBlank() || !registerEmail.contains("@") -> localValidationMessage = "Please enter a valid email address."
                                    registerPassword.length < 6 -> localValidationMessage = "Master password must be at least 6 characters."
                                    registerPassword != registerConfirmPassword -> localValidationMessage = "Passwords do not match."
                                    else -> {
                                        viewModel.register(
                                            name = registerName,
                                            email = registerEmail,
                                            pass = registerPassword,
                                            avatarIndex = selectedAvatarIndex
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_submit_button")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Switch back to Sign In
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account? ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedTab = 0
                                        viewModel.clearAuthError()
                                        localValidationMessage = null
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. SECURITY & ARCHITECTURE TRUST BADGES
            SecurityTrustSection()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Help & Recovery Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Zero-Cloud Account Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DeviceMonitor uses an offline-first cryptographic vault. Your profile, sensor readings, and telemetry are stored in an encrypted local SQLite database directly on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Passwords are stored as PBKDF2 password hashes on this device.\n• No telemetry or credentials are ever sent to third-party clouds.\n• If you need to migrate devices, use the JSON Backup in Settings to export and import your workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Understood", color = Color.White)
                }
            }
        )
    }
}

/**
 * Animated Segmented Tab Pill Controller
 */
@Composable
private fun AuthSegmentedTab(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabTitles = listOf("Sign In", "Create Account")
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / 2

            val targetOffset = if (selectedTab == 0) 0.dp else tabWidth
            val animatedOffset by animateFloatAsState(
                targetValue = targetOffset.value,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "tab_pill_offset"
            )

            // Sliding active indicator pill
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = (animatedOffset * density).toInt(), y = 0) }
                    .width(tabWidth)
                    .height(52.dp)
                    .padding(4.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
            )

            // Clickable tabs
            Row(modifier = Modifier.fillMaxSize()) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tab_text_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(index) }
                            .then(
                                if (index == 0) Modifier.testTag("auth_tab_signin")
                                else Modifier.testTag("auth_tab_register")
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Lock else Icons.Default.Person,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Elevated hardware branding emblem & header
 */
@Composable
private fun BrandHeroHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Geometric Emblem
        val isDark = MaterialTheme.colorScheme.background.red < 0.5f
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            else listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                NeonEmerald,
                                TechPurple
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "DeviceMonitor Platform",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = "DeviceMonitor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.75.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status Badge Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(NeonEmerald, CircleShape)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "SECURE TELEMETRY GATEWAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Premium primary action button with gradient accent and elevation
 */
@Composable
private fun AuthGradientButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val buttonColors = if (isDark) {
        listOf(CyberCyanDark, CyberCyan, NeonEmerald)
    } else {
        listOf(Color(0xFF0369A1), Color(0xFF0284C7), Color(0xFF059669))
    }

    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (!isLoading) buttonColors
                    else listOf(Color.Gray, Color.DarkGray)
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Authenticating...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Real-time Password Strength Meter with color-coded feedback
 */
@Composable
private fun PasswordStrengthIndicator(password: String) {
    val length = password.length
    val hasLetters = password.any { it.isLetter() }
    val hasDigits = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val strengthScore = when {
        length >= 10 && hasLetters && hasDigits && hasSpecial -> 4
        length >= 8 && hasLetters && hasDigits -> 3
        length >= 6 -> 2
        else -> 1
    }

    val (label, color) = when (strengthScore) {
        4 -> "Strong" to NeonEmerald
        3 -> "Good" to MaterialTheme.colorScheme.primary
        2 -> "Moderate" to TechAmber
        else -> "Weak (Min 6 chars)" to TechCoral
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Security Level",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (step in 1..4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (step <= strengthScore) color
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

/**
 * Trust and Security Certification Badges
 */
@Composable
private fun SecurityTrustSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrustBadgeItem(
                icon = Icons.Default.Shield,
                label = "Local Account Protection"
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
            TrustBadgeItem(
                icon = Icons.Default.Security,
                label = "Zero-Cloud Leaks"
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
            TrustBadgeItem(
                icon = Icons.Default.Lock,
                label = "Isolated Profiles"
            )
        }
    }
}

@Composable
private fun TrustBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}

/**
 * Atmospheric background canvas:
 * - Subtle tech grid coordinate points
 * - Ambient radial gradient glow flares
 * - Precision engineering IoT micro-circuit traces
 */
@Composable
private fun AuthAtmosphericBackground() {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgGradient = if (isDark) {
        listOf(
            Color(0xFF090E1A),
            Color(0xFF0E1726),
            Color(0xFF090E1A)
        )
    } else {
        listOf(
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFF8FAFC)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Ambient Glow Flares
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isDark) 0.16f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.82f, height * 0.14f),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(width * 0.82f, height * 0.14f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonEmerald.copy(alpha = if (isDark) 0.10f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.86f),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(width * 0.15f, height * 0.86f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TechPurple.copy(alpha = if (isDark) 0.08f else 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.50f, height * 0.45f),
                    radius = width * 0.50f
                ),
                radius = width * 0.50f,
                center = Offset(width * 0.50f, height * 0.45f)
            )

            // 2. Telemetry Coordinate Grid
            val gridSpacing = 42.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            val dotColor = if (isDark) primaryColor.copy(alpha = 0.08f) else Color.DarkGray.copy(alpha = 0.05f)

            var x = gridSpacing / 2
            while (x < width) {
                var y = gridSpacing / 2
                while (y < height) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += gridSpacing
                }
                x += gridSpacing
            }

            // 3. Hardware Circuit traces in top right and bottom left
            val traceColor = primaryColor.copy(alpha = if (isDark) 0.16f else 0.10f)
            val strokeWidth = 1.3.dp.toPx()

            // Upper right trace system
            val trace1 = Path().apply {
                moveTo(width * 0.60f, 0f)
                lineTo(width * 0.60f, height * 0.06f)
                lineTo(width * 0.90f, height * 0.16f)
                lineTo(width, height * 0.16f)
            }
            drawPath(path = trace1, color = traceColor, style = Stroke(width = strokeWidth))
            drawCircle(color = traceColor, radius = 3.dp.toPx(), center = Offset(width * 0.90f, height * 0.16f))

            val trace2 = Path().apply {
                moveTo(width, height * 0.25f)
                lineTo(width * 0.76f, height * 0.25f)
                lineTo(width * 0.70f, height * 0.31f)
                lineTo(width * 0.70f, height * 0.37f)
            }
            drawPath(path = trace2, color = traceColor, style = Stroke(width = strokeWidth))
            drawCircle(color = traceColor, radius = 2.5.dp.toPx(), center = Offset(width * 0.70f, height * 0.37f))

            // Lower left trace system
            val trace3 = Path().apply {
                moveTo(0f, height * 0.82f)
                lineTo(width * 0.24f, height * 0.82f)
                lineTo(width * 0.34f, height * 0.88f)
                lineTo(width * 0.34f, height)
            }
            drawPath(path = trace3, color = traceColor, style = Stroke(width = strokeWidth))
            drawCircle(color = traceColor, radius = 3.dp.toPx(), center = Offset(width * 0.34f, height * 0.88f))
        }
    }
}
