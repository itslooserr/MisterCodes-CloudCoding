package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import android.os.Build
import android.app.ActivityManager
import android.os.StatFs
import android.os.Environment
import android.content.Context
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CodeProject
import com.example.data.SharedSnippet
import com.example.data.TutorialChallenge
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatXp(xp: Double): String {
    return when {
        xp >= 1_000_000_000_000.0 -> {
            val value = xp / 1_000_000_000_000.0
            String.format(java.util.Locale.US, "%.2fT", value)
        }
        xp >= 1_000_000_000.0 -> {
            val value = xp / 1_000_000_000.0
            String.format(java.util.Locale.US, "%.2fB", value)
        }
        xp >= 1_000_000.0 -> {
            val value = xp / 1_000_000.0
            String.format(java.util.Locale.US, "%.2fM", value)
        }
        else -> {
            String.format(java.util.Locale.US, "%.3f", xp)
        }
    }
}

// --- HELPER COMPOSABLE: CODE SYNTAX HIGHLIGHTER ---
@Composable
fun highlightCode(code: String, isDark: Boolean): AnnotatedString {
    return remember(code, isDark) {
        buildAnnotatedString {
            append(code)
            
            // Standard keywords highlighters
            val keywords = listOf(
                "fun", "val", "var", "class", "def", "import", "from", "return", 
                "if", "else", "for", "while", "in", "public", "private", "static", 
                "void", "package", "fn", "let", "const", "include", "int", "char"
            )
            val keywordColor = CodeKeyword
            val stringColor = CodeString
            val commentColor = CodeComment
            val numberColor = CodeNumber

            // 1. Comments highlighter (Simple check for lines starting with # or //)
            val lines = code.split("\n")
            var currentIndex = 0
            for (line in lines) {
                if (line.trim().startsWith("#") || line.trim().startsWith("//")) {
                    addStyle(
                        SpanStyle(color = commentColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        currentIndex,
                        currentIndex + line.length
                    )
                } else {
                    // Match inline strings "..." or '...'
                    var inQuotes = false
                    var startQuote = -1
                    for (i in line.indices) {
                        val char = line[i]
                        if (char == '"' || char == '\'') {
                            if (!inQuotes) {
                                inQuotes = true
                                startQuote = i
                            } else {
                                inQuotes = false
                                addStyle(
                                    SpanStyle(color = stringColor),
                                    currentIndex + startQuote,
                                    currentIndex + i + 1
                                )
                            }
                        }
                    }
                    
                    // Match keywords
                    val words = line.split(Regex("[^a-zA-Z0-9_\$#]+"))
                    for (word in words) {
                        if (word in keywords) {
                            val wordStart = line.indexOf(word)
                            if (wordStart != -1) {
                                addStyle(
                                    SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                                    currentIndex + wordStart,
                                    currentIndex + wordStart + word.length
                                )
                            }
                        }
                        // Match numbers
                        if (word.all { it.isDigit() }) {
                            val wordStart = line.indexOf(word)
                            if (wordStart != -1) {
                                addStyle(
                                    SpanStyle(color = numberColor),
                                    currentIndex + wordStart,
                                    currentIndex + wordStart + word.length
                                )
                            }
                        }
                    }
                }
                currentIndex += line.length + 1 // +1 for newline character
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(
    viewModel: MisterCodesViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "LogoScale"
    )
    val opacity by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1500),
        label = "TextOpacity"
    )

    val userProfile by viewModel.userProfile.collectAsState()

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2600)
        
        val profile = viewModel.userProfile.value
        if (profile != null) {
            if (profile.isLoggedIn) {
                onNavigateToMain()
            } else {
                // If they have registered at some point (or if they aren't brand new), go straight to Auth, else Onboarding
                if (profile.email != "mistercodes@codes.com" || profile.username != "MisterCoder") {
                    onNavigateToAuth()
                } else {
                    onNavigateToOnboarding()
                }
            }
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060913), Color(0xFF101625))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Custom Vector Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .drawBehind {
                        // Futuristic rotating ring highlight
                        drawCircle(
                            color = DarkPrimary.copy(alpha = 0.3f),
                            radius = size.minDimension / 1.6f,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    .background(Color(0xFF151D30), CircleShape)
                    .border(2.dp, DarkPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = "Mister Codes logo icon",
                    tint = DarkPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Brand Name & Slogan with nice font weights
            Text(
                text = "Mister Codes",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.testTag("app_brand_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Code Anywhere. Learn Everywhere.",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = DarkPrimary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.testTag("app_slogan")
            )
        }

        // Processing progress bar
        CircularProgressIndicator(
            color = DarkSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

// ==========================================
// 2. ONBOARDING SCREEN
// ==========================================
@Composable
fun OnboardingScreen(onNavigateToAuth: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPageData(
            title = "Write & Run Code Instantly",
            desc = "Advanced compiler support for 11+ programming languages including Python, Kotlin, Rust, Go, JavaScript, and more right in your pocket.",
            icon = Icons.Outlined.CodeBlock
        ),
        OnboardingPageData(
            title = "AI Coding Mentor",
            desc = "Need debugging help or code translations? Reach out to the built-in Google AI Coach inside Mister Codes. Save time, learn faster.",
            icon = Icons.Outlined.Psychology
        ),
        OnboardingPageData(
            title = "Interactive Coding Academy",
            desc = "Progress through structured quizzes, algorithm roadmaps, and instant-compile coding challenges. Gain experience, track milestones.",
            icon = Icons.Outlined.School
        )
    )

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator dots
                Row {
                    pages.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (currentPage == idx) 20.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentPage == idx) DarkPrimary else Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onNavigateToAuth()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("next_onboarding_btn")
                ) {
                    Text(
                        text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontSize = 15.sp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "Next Icon", modifier = Modifier.size(16.dp))
                }
            }
        },
        containerColor = Color(0xFF070A13)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large styled custom design graphic
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.linearGradient(listOf(DarkPrimary, DarkSecondary)),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        .background(Color(0xFF151D30), RoundedCornerShape(95.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pages[currentPage].icon,
                        contentDescription = "Feature Vector Icon",
                        tint = DarkPrimary,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = pages[currentPage].title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("onboarding_title")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = pages[currentPage].desc,
                    fontSize = 15.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("onboarding_desc")
                )
            }
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// Helper objects for missing icons
val Icons.Outlined.CodeBlock get() = Icons.Outlined.Keyboard

// ==========================================
// 3. AUTH SCREEN (LOGIN / SIGNUP)
// ==========================================
@Composable
fun AuthScreen(viewModel: MisterCodesViewModel, onNavToMain: () -> Unit) {
    var isLoginTab by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSigningIn by remember { mutableStateOf(false) }

    var isCaptchaVerified by remember { mutableStateOf(false) }
    var showCaptchaDialog by remember { mutableStateOf(false) }
    var captchaQuestion by remember { mutableStateOf("") }
    var captchaTargetAnswer by remember { mutableStateOf(0.0) }
    var userCaptchaInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    fun generateChallenge(): Pair<String, Double> {
        val ops = listOf("+", "-", "*", "/", "fraction")
        return when (ops.random()) {
            "+" -> {
                val a = (12..48).random()
                val b = (10..40).random()
                Pair("$a + $b", (a + b).toDouble())
            }
            "-" -> {
                val a = (50..99).random()
                val b = (10..45).random()
                Pair("$a - $b", (a - b).toDouble())
            }
            "*" -> {
                val a = (2..9).random()
                val b = (3..9).random()
                Pair("$a * $b", (a * b).toDouble())
            }
            "/" -> {
                val b = (3..9).random()
                val target = (2..9).random()
                Pair("${b * target} / $b", target.toDouble())
            }
            else -> { // fraction
                val choices = listOf(
                    Pair("1/2 of 50", 25.0),
                    Pair("1/4 of 80", 20.0),
                    Pair("3/4 of 40", 30.0),
                    Pair("1/3 of 90", 30.0),
                    Pair("1/5 of 150", 30.0),
                    Pair("1/10 of 100", 10.0)
                )
                choices.random()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = "IDE Terminal Icon",
                tint = DarkPrimary,
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isLoginTab) "Welcome Back" else "Create Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.testTag("auth_heading")
            )

            Text(
                text = if (isLoginTab) "Code and learn directly inside terminal" else "Track levels and synchronize workspace online",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Sub Tab
            TabRow(
                selectedTabIndex = if (isLoginTab) 0 else 1,
                containerColor = Color(0xFF151D30),
                contentColor = DarkPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .height(48.dp),
                indicator = {}
            ) {
                Tab(
                    selected = isLoginTab,
                    onClick = { isLoginTab = true },
                    text = { Text("Login", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = !isLoginTab,
                    onClick = { isLoginTab = false },
                    text = { Text("Register", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Fields
            if (!isLoginTab) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = DarkPrimary,
                        focusedBorderColor = DarkPrimary,
                        unfocusedLabelColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = DarkPrimary,
                    focusedBorderColor = DarkPrimary,
                    unfocusedLabelColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = DarkPrimary,
                    focusedBorderColor = DarkPrimary,
                    unfocusedLabelColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Captcha row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isCaptchaVerified) {
                            val challenge = generateChallenge()
                            captchaQuestion = challenge.first
                            captchaTargetAnswer = challenge.second
                            userCaptchaInput = ""
                            showCaptchaDialog = true
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30)),
                border = BorderStroke(1.dp, if (isCaptchaVerified) Color.Green else Color.Gray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isCaptchaVerified,
                            onCheckedChange = {
                                if (it && !isCaptchaVerified) {
                                    val challenge = generateChallenge()
                                    captchaQuestion = challenge.first
                                    captchaTargetAnswer = challenge.second
                                    userCaptchaInput = ""
                                    showCaptchaDialog = true
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Green, checkmarkColor = Color.Black)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCaptchaVerified) "Human Verification Passed!" else "Prove you are not a robot",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = if (isCaptchaVerified) Icons.Filled.CheckCircle else Icons.Filled.Security,
                        contentDescription = "verification badge",
                        tint = if (isCaptchaVerified) Color.Green else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isLoginTab) {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please key in credentials", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    } else {
                        if (username.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // Check availability
                        if (!viewModel.isUsernameAvailable(username)) {
                            Toast.makeText(context, "Username @$username is already taken in our cloud registry!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                    }

                    if (!isCaptchaVerified) {
                        Toast.makeText(context, "Please click to solve Math Captcha first to verify you are human!", Toast.LENGTH_LONG).show()
                        val challenge = generateChallenge()
                        captchaQuestion = challenge.first
                        captchaTargetAnswer = challenge.second
                        userCaptchaInput = ""
                        showCaptchaDialog = true
                        return@Button
                    }

                    isSigningIn = true
                    if (isLoginTab) {
                        viewModel.performLogin(
                            emailOrUsername = email, // Email text-field carries email/username on login tab
                            password = password,
                            onSuccess = {
                                isSigningIn = false
                                Toast.makeText(context, "Welcome back, dev session restored! 🚀", Toast.LENGTH_SHORT).show()
                                onNavToMain()
                            },
                            onError = { errMsg ->
                                isSigningIn = false
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        viewModel.performSignup(
                            username = username,
                            email = email,
                            password = password,
                            onSuccess = {
                                isSigningIn = false
                                Toast.makeText(context, "Welcome to online dev space, profile initialized! 🎉", Toast.LENGTH_SHORT).show()
                                onNavToMain()
                            },
                            onError = { errMsg ->
                                isSigningIn = false
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_submit_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isLoginTab) "Continue to Console" else "Sign Up & Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick bypass guest login
            TextButton(
                onClick = {
                    viewModel.setLoginState(true)
                    onNavToMain()
                }
            ) {
                Text(
                    text = "Continue as Developer Guest Mode",
                    color = DarkPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showCaptchaDialog) {
        AlertDialog(
            onDismissRequest = { showCaptchaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Security, contentDescription = "Captcha", tint = DarkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prove you are not a robot", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Please solve this random math equation:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = captchaQuestion,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = userCaptchaInput,
                        onValueChange = { userCaptchaInput = it },
                        label = { Text("Your Answer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkPrimary,
                            focusedLabelColor = DarkPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = userCaptchaInput.trim().toDoubleOrNull()
                        if (parsed != null && Math.abs(parsed - captchaTargetAnswer) < 0.01) {
                            isCaptchaVerified = true
                            showCaptchaDialog = false
                            Toast.makeText(context, "Human verification successful!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Incorrect answer. Verification failed!", Toast.LENGTH_SHORT).show()
                            // Fresh challenge
                            val challenge = generateChallenge()
                            captchaQuestion = challenge.first
                            captchaTargetAnswer = challenge.second
                            userCaptchaInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black)
                ) {
                    Text("Verify Answer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptchaDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF151D30)
        )
    }
}

// ==========================================
// CENTRAL BOTTOM NAVIGATION ENUM
// ==========================================
enum class MisterCodesHub {
    DASHBOARD,
    EDITOR,
    ASSISTANT,
    LEARNING,
    PROJECTS
}

// ==========================================
// 4. HOME DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: MisterCodesViewModel,
    onNavigateHub: (MisterCodesHub) -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val allProjects by viewModel.projects.collectAsState()
    val allChallenges by viewModel.challenges.collectAsState()

    val completedChallengesCount = allChallenges.count { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        // TOP CUSTOM HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${profile?.username ?: "Developer"}",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(
                    text = "Welcome back to your workspace.",
                    style = TextStyle(fontSize = 13.sp, color = Color.Gray)
                )
            }

            // Small glowing level badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF151D30), CircleShape)
                    .border(1.5.dp, DarkPrimary, CircleShape)
                    .clickable { onNavigateToProfile() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "L${profile?.level ?: 1}",
                    color = DarkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // STATS / PROGRESS CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.MilitaryTech, contentDescription = "XP Medal", tint = DarkPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${formatXp(profile?.xp ?: 0.0)} XP earned",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            // Streak count indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${profile?.currentStreak ?: 0} Days",
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Level progress bar
                        val xpProgress = (((profile?.xp ?: 10.0) % 100.0) / 100.0).toFloat()
                        Text(
                            text = "Next Level Progression (${(xpProgress * 100f).toInt()}%)",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = DarkPrimary,
                            trackColor = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Challenges Solved: $completedChallengesCount / ${allChallenges.size}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // CORE HUB ACTIONS QUICK GRID
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(
                        text = "Quick Command Centers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardQuickCard(
                            title = "AI Coach",
                            subtitle = "Ask Assistant",
                            icon = Icons.Outlined.Chat,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateHub(MisterCodesHub.ASSISTANT) }
                        )
                        DashboardQuickCard(
                            title = "IDE Workspace",
                            subtitle = "Code & Compile",
                            icon = Icons.Outlined.Terminal,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateHub(MisterCodesHub.EDITOR) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardQuickCard(
                            title = "Academy",
                            subtitle = "Interactive Tasks",
                            icon = Icons.Outlined.MilitaryTech,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateHub(MisterCodesHub.LEARNING) }
                        )
                        DashboardQuickCard(
                            title = "Community",
                            subtitle = "Discover Snippets",
                            icon = Icons.Outlined.Group,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCommunity
                        )
                    }
                }
            }

            // RECENT FILES SEC
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Workspace Projects",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        TextButton(onClick = { onNavigateHub(MisterCodesHub.PROJECTS) }) {
                            Text("Manage Files", color = DarkPrimary, fontSize = 13.sp)
                        }
                    }

                    if (allProjects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0xFF151D30), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No projects yet. Click to construct one!", color = Color.Gray)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allProjects) { proj ->
                                Card(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable {
                                            viewModel.onProjectSelected(proj)
                                            onNavigateHub(MisterCodesHub.EDITOR)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(DarkPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(proj.language, color = DarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            proj.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "Edit workspace contents",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // DEVICE CONFIGURATION TELEMETRY DIAGNOSTICS CARD
            item {
                val context = LocalContext.current
                
                // Determine RAM details
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager?.getMemoryInfo(memInfo)
                val totalRamGb = memInfo.totalMem.toDouble() / (1024L * 1024 * 1024)
                val parsedRam = Math.round(totalRamGb * 10.0) / 10.0
                val ramOk = parsedRam >= 4.0
                
                // Determine storage space details
                val stat = StatFs(Environment.getDataDirectory().path)
                val totalStorageGb = (stat.blockCountLong.toDouble() * stat.blockSizeLong) / (1024L * 1024 * 1024)
                val parsedStorage = Math.round(totalStorageGb * 10.0) / 10.0
                val storageOk = parsedStorage >= 64.0
                
                // Determine Android 11 condition
                val sdkVersion = Build.VERSION.SDK_INT
                val android11Ok = sdkVersion >= 30 // SDK 30 is Android 11

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
                    border = BorderStroke(0.5.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DEVICE TELEMETRY AUDIT",
                                color = DarkPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (ramOk && android11Ok) Color.Green.copy(alpha = 0.15f) 
                                        else Color.Yellow.copy(alpha = 0.15f), 
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (ramOk && android11Ok) "OPTIMAL RUNTIME" else "COMPATIBLE STATUS",
                                    color = if (ramOk && android11Ok) Color.Green else Color.Yellow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // RAM spec row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Memory, 
                                    contentDescription = "Ram spec", 
                                    tint = if (ramOk) Color.Green else Color.Yellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("System Memory (RAM)", color = Color.White, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$parsedRam GB detected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (ramOk) ">= 4GB target passed ✓" else "Target < 4GB warning",
                                    color = if (ramOk) Color.Green else Color.Yellow,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Storage spec row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Storage, 
                                    contentDescription = "Storage spec", 
                                    tint = if (storageOk) Color.Green else Color.Yellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Internal Disk Storage", color = Color.White, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$parsedStorage GB detected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (storageOk) ">= 64GB passed ✓" else "Compatible runtime scale",
                                    color = if (storageOk) Color.Green else Color.Yellow,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Android Version row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Android, 
                                    contentDescription = "OS Version spec", 
                                    tint = if (android11Ok) Color.Green else Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Android Operating System", color = Color.White, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val osVersionLabel = "Android " + when (Build.VERSION.SDK_INT) {
                                    30 -> "11"
                                    31 -> "12"
                                    32 -> "12L"
                                    33 -> "13"
                                    34 -> "14"
                                    35 -> "15"
                                    else -> "Pre-11"
                                }
                                Text("$osVersionLabel (SDK $sdkVersion)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (android11Ok) "Android 11+ targets reached ✓" else "Requires Android 11 upgrade",
                                    color = if (android11Ok) Color.Green else Color.Red,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // EXTRA CONTROLS UTILITY ROW
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Preferences", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure Prefs", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = "Account Profile", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My Account", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardQuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = DarkPrimary, modifier = Modifier.size(24.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

// ==========================================
// 5. CODE EDITOR PAGE
// ==========================================
@Composable
fun CodeEditorScreen(
    viewModel: MisterCodesViewModel,
    onNavigateToConsole: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    val selectedProj by viewModel.currentSelectedProject.collectAsState()
    val editorCodeText by viewModel.editorCodeText.collectAsState()
    val editorLanguage by viewModel.editorLanguage.collectAsState()
    val fontSizeState by viewModel.editorFontSize.collectAsState()

    val availableLangs = listOf("Python", "Java", "C", "C++", "JavaScript", "HTML", "CSS", "PHP", "Kotlin", "Go", "Rust")
    var expandLangSpinner by remember { mutableStateOf(false) }

    // Search replace local drawer
    var showSearchRow by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var replaceKeyword by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isWorkspaceSecureLocked by viewModel.isWorkspaceSecureLocked.collectAsState()
    var passcodeFieldInput by remember { mutableStateOf("") }
    val activePasscode by viewModel.workspacePasscode.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.isWorkspaceSecureLocked.value = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        // EDITOR TOP HUB
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedProj?.title ?: "Draft Editor",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selectedProj == null) "Local unsaved terminal" else "Workspace persistence active",
                    fontSize = 11.sp,
                    color = DarkPrimary
                )
            }

            // Language Selector Button
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Button(
                    onClick = { expandLangSpinner = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30), contentColor = DarkPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("editor_lang_selector")
                ) {
                    Text(editorLanguage, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "dropdown", modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = expandLangSpinner,
                    onDismissRequest = { expandLangSpinner = false },
                    modifier = Modifier.background(Color(0xFF151D30))
                ) {
                    availableLangs.forEach { lg ->
                        DropdownMenuItem(
                            text = { Text(lg, color = Color.White) },
                            onClick = {
                                viewModel.onLanguageChanged(lg)
                                expandLangSpinner = false
                            }
                        )
                    }
                }
            }

            // Shortcut Search
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showSearchRow = !showSearchRow },
                    modifier = Modifier.testTag("editor_search_toggle")
                ) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Find Replace", tint = Color.LightGray)
                }

                IconButton(
                    onClick = {
                        if (isWorkspaceSecureLocked) {
                            // Already locked, tap again to focus decryption vault
                            Toast.makeText(context, "Enter passcode below to unlock space! 🔐", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.isWorkspaceSecureLocked.value = true
                            Toast.makeText(context, "File locked with password protection! 🔐", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isWorkspaceSecureLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = "File lock status",
                        tint = if (isWorkspaceSecureLocked) Color.Red else DarkPrimary
                    )
                }
            }
        }

        // FIND AND REPLACE DRAWER ROW
        AnimatedVisibility(visible = showSearchRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151D30))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = { Text("Find symbol...", fontSize = 12.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = DarkPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = replaceKeyword,
                    onValueChange = { replaceKeyword = it },
                    placeholder = { Text("Replace with...", fontSize = 12.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = DarkPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (searchKeyword.isNotEmpty()) {
                            val updated = editorCodeText.replace(searchKeyword, replaceKeyword)
                            viewModel.onEditorCodeChanged(updated)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 🚀 AI CODE GENERATION COMPANION ROW
        var aiCodePrompt by remember { mutableStateOf("") }
        val aiLoadingState by viewModel.aiLoading.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C101B))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Generate Code via Google Gemini ✨",
                color = DarkPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = aiCodePrompt,
                    onValueChange = { aiCodePrompt = it },
                    placeholder = { Text("e.g. create binary search in $editorLanguage", fontSize = 12.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (aiCodePrompt.isNotEmpty()) {
                            IconButton(onClick = { aiCodePrompt = "" }) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "clear prompt", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                Button(
                    onClick = {
                        if (aiCodePrompt.isBlank()) {
                            Toast.makeText(context, "Please enter a coding prompt first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            viewModel.generateCodeFromPrompt(aiCodePrompt, editorLanguage)
                            Toast.makeText(context, "AI code auto-inserted!", Toast.LENGTH_SHORT).show()
                            aiCodePrompt = ""
                        }
                    },
                    enabled = !aiLoadingState,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (aiLoadingState) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = "Gen", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // AI ASSISTANT SHORTCUTS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFF0F1524))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiShortcutChip(label = "Explain Code", icon = Icons.Outlined.Lightbulb, onClick = {
                viewModel.requestAiShortcuts("EXPLAIN")
                onNavigateToAssistant()
            })
            AiShortcutChip(label = "Optimize Complex", icon = Icons.Outlined.OfflineBolt, onClick = {
                viewModel.requestAiShortcuts("OPTIMIZE")
                onNavigateToAssistant()
            })
            AiShortcutChip(label = "Fix Compilation Bugs", icon = Icons.Outlined.BugReport, onClick = {
                viewModel.requestAiShortcuts("FIX_BUGS")
                onNavigateToAssistant()
            })
            AiShortcutChip(label = "Add Comments", icon = Icons.Outlined.Comment, onClick = {
                viewModel.requestAiShortcuts("COMMENTS")
                onNavigateToAssistant()
            })
            AiShortcutChip(label = "Convert Lang", icon = Icons.Outlined.Translate, onClick = {
                viewModel.requestAiShortcuts("CONVERT")
                onNavigateToAssistant()
            })
        }

        // ACTIVE EDITOR FIELD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF05080E))
        ) {
            if (isWorkspaceSecureLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF090D1C))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Security Vault",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PASSWORD PROTECTED FILE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "This editor workspace is encrypted under passcode $activePasscode to prevent key leaks on decompilation.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passcodeFieldInput,
                        onValueChange = { passcodeFieldInput = it },
                        label = { Text("Enter Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = Color.Red
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (passcodeFieldInput.trim() == activePasscode.trim()) {
                                viewModel.isWorkspaceSecureLocked.value = false
                                passcodeFieldInput = ""
                                Toast.makeText(context, "Access Granted! File Decrypted. 🔓", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Access Denied! Incorrect workspace passcode.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("DECRYPT & UNLOCK WORKSPACE", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val highlighted = highlightCode(editorCodeText, true)

            // Split line numbering sidebar and raw layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Line count sidebar generator
                val linesCount = editorCodeText.split("\n").size.coerceAtLeast(1)
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .width(42.dp)
                        .background(Color(0xFF0C101B))
                        .padding(top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..linesCount) {
                        Text(
                            text = "$i",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeState.sp,
                            modifier = Modifier.padding(end = 8.dp, bottom = 2.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Interactive styled edit block consisting of transparent input and highlighted visual
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    if (editorCodeText.isEmpty()) {
                        Text(
                            text = "Write code here...",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = fontSizeState.sp
                        )
                    }
                    
                    // Behind drawing highlighted
                    Text(
                        text = highlighted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSizeState.sp,
                        lineHeight = (fontSizeState + 5).sp
                    )

                    // Overlay input transparent layer
                    BasicTextField(
                        value = editorCodeText,
                        onValueChange = { viewModel.onEditorCodeChanged(it) },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeState.sp,
                            color = Color.Transparent,
                            lineHeight = (fontSizeState + 5).sp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code_editor_field"),
                        cursorBrush = Brush.verticalGradient(listOf(DarkPrimary, DarkPrimary))
                    )
                }
            }

            // Quick Execution Action Floating Button
            FloatingActionButton(
                onClick = {
                    viewModel.runActiveCode()
                    onNavigateToConsole()
                },
                containerColor = DarkPrimary,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("editor_compile_fab")
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Run compiler execution")
            }
        }
        }
    }
}

@Composable
fun AiShortcutChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2436))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = DarkPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 6. OUTPUT CONSOLE (TERMINAL VIEW)
// ==========================================
@Composable
fun ConsoleScreen(
    viewModel: MisterCodesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    val editorLanguage by viewModel.editorLanguage.collectAsState()
    val isCompiling by viewModel.isCompiling.collectAsState()
    val outputResult by viewModel.consoleOutputResult.collectAsState()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020408))
            .statusBarsPadding()
    ) {
        // TOP CONSOLE NAVIGATION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compilation Terminal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .background(DarkPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(editorLanguage.uppercase(), color = DarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // TERMINAL OUTPUT VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF080C14))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
        ) {
            if (isCompiling) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DarkPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("AI Compiler sandboxing...", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "MisterCodes Sandbox Environment [v1.0.4]\nInitializing terminal wrapper...\n",
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )

                    if (outputResult == null) {
                        Text(
                            text = "Terminal idle. Launch code utilizing the play fab inside the Editor HUD to monitor sandboxed executions.",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("console_idle_label")
                        )
                    } else {
                        val result = outputResult!!
                        if (result.hasErrors) {
                            Text(
                                text = ">> Compilation failed! Stacktrace logs:\n",
                                fontFamily = FontFamily.Monospace,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = result.errors,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            Text(
                                text = ">> Compilation success. Run console log output:\n",
                                fontFamily = FontFamily.Monospace,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = result.output,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 16.dp).testTag("terminal_output")
                            )
                        }
                    }
                }
            }
        }

        // TOOLBAR CONTEXTS
        if (outputResult != null && !isCompiling) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val textToCopy = outputResult?.let { if (it.hasErrors) it.errors else it.output } ?: ""
                        if (textToCopy.isNotEmpty()) {
                            clipboard.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "Copied log output to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30), contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Logs", fontSize = 13.sp)
                }

                // AI coach intervention button
                Button(
                    onClick = {
                        val extra = outputResult?.let { if (it.hasErrors) it.errors else it.output } ?: ""
                        viewModel.onChatInputChanged("My code compiled and gave the following result / output. Can you explain this log or correct any bugs?\n\nCode:\n```\n${viewModel.editorCodeText.value}\n```\nResult Logs:\n```\n$extra\n```")
                        viewModel.sendChatPrompt()
                        onNavigateToAssistant()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Psychology, contentDescription = "coach", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Coach Help", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 7. AI ASSISTANT PANEL
// ==========================================
@Composable
fun AiAssistantScreen(viewModel: MisterCodesViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val chatInputText by viewModel.chatInputText.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // Auto-scroll on chat refresh
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        // AI HEADER INFO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(DarkPrimary.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, DarkPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.SmartButton, contentDescription = "ai emblem", tint = DarkPrimary)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Mister Codes AI Coach", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("Powered by Google Gemini 3.5", color = DarkPrimary, fontSize = 11.sp)
            }
        }

        // CONVERSATION CHAT STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF05080E)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Filled.Psychology, contentDescription = "empty coach", tint = Color.DarkGray, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your Mobile Coding Copilot",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap on 'Explain Code', 'Fix Compilation Bugs' shortcuts inside the Editor, or insert a custom prompt below to begin.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Suggested prompts grid
                        val prompts = listOf(
                            "Teach me Recursion in simple terms",
                            "Explain CSS flexbox vs grid layouts",
                            "Convert Python Loops into Go structures"
                        )
                        prompts.forEach { p ->
                            Button(
                                onClick = {
                                    viewModel.onChatInputChanged(p)
                                    viewModel.sendChatPrompt()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151D30), contentColor = Color.LightGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text(p, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                items(chatHistory) { msg ->
                    ChatBubbleItem(msg = msg, onCopy = {
                        clipboard.setText(AnnotatedString(it))
                        Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                    })
                }

                if (aiLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(DarkSecondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Filled.Android, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(color = DarkPrimary, modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI typing...", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // FOOTER INPUT PANEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInputText,
                onValueChange = { viewModel.onChatInputChanged(it) },
                placeholder = { Text("Ask Coach (e.g. explain variables)", fontSize = 14.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = DarkPrimary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_assistant_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.sendChatPrompt() },
                modifier = Modifier
                    .size(48.dp)
                    .background(DarkPrimary, RoundedCornerShape(12.dp))
                    .testTag("ai_assistant_send_btn"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
            ) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = "Send text", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ChatBubbleItem(msg: ChatMessage, onCopy: (String) -> Unit) {
    val isUser = msg.sender == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E3B5E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Android, contentDescription = "ai", tint = DarkPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isUser) Color(0xFF1B2436) else Color(0xFF0F1524))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = msg.text, fontSize = 14.sp, color = Color.White)
                    
                    // Code highlight layout
                    if (msg.text.contains("```")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                val regex = "```.*?\\n(.*?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
                                val code = regex.find(msg.text)?.groupValues?.get(1) ?: msg.text
                                onCopy(code)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = DarkPrimary),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "copy code block", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code block", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Just now",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = "me", tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ==========================================
// 8. LEARNING ACADEMY
// ==========================================
@Composable
fun LearningScreen(viewModel: MisterCodesViewModel, onNavigateToEditor: () -> Unit) {
    val challenges by viewModel.challenges.collectAsState()
    val activeChallenge by viewModel.activeChallenge.collectAsState()
    val quizSelectedOption by viewModel.quizSelectedOption.collectAsState()
    val quizResultLabel by viewModel.quizResultLabel.collectAsState()
    val academicProjects by viewModel.academicProjects.collectAsState()
    val roadmaps by viewModel.roadmaps.collectAsState()

    var activeLearnTab by remember { mutableStateOf(0) } // 0: Roadmaps, 1: Quiz & Challenges
    val coderLanguages = listOf("Python", "Kotlin", "JavaScript", "HTML", "Go", "Rust")
    var selectedLangFilter by remember { mutableStateOf("Python") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        if (activeChallenge != null) {
            // DETAILED ACTIVE CHALLENGE SCREEN
            val chall = activeChallenge!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectActiveChallenge(chall.copy(isCompleted = false)) /* clears active */ }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(chall.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(DarkPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "DIFFICULTY: ${chall.difficulty} | LANGUAGE: ${chall.language}",
                        color = DarkPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = chall.description,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (chall.questionType == "QUIZ") {
                    // QUIZ RENDERING LAYOUT
                    val options = remember(chall) {
                        try {
                            val array = org.json.JSONArray(chall.optionsJson)
                            List(array.length()) { array.getString(it) }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    Text("Make your selection:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    options.forEachIndexed { idx, opt ->
                        val isSelected = quizSelectedOption == idx
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) DarkPrimary else Color.DarkGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) DarkPrimary.copy(alpha = 0.08f) else Color(0xFF151D30))
                                .clickable { viewModel.onQuizOptionSelected(idx) }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.onQuizOptionSelected(idx) },
                                    colors = RadioButtonDefaults.colors(selectedColor = DarkPrimary, unselectedColor = Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(opt, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (quizResultLabel != null) {
                        Text(
                            text = quizResultLabel!!,
                            color = if (quizResultLabel!!.startsWith("Correct")) Color.Green else Color.Red,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.verifyQuizAnswer() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify My Answer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // CODING PROBLEM INTERACTIVE REDIRECTOR
                    Text(
                        text = "Write your script below. The driver should produce: ${chall.testCasesJson}",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Open in advanced editor redirector card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Inject template code inside Editor ViewModel State and Route
                                val localProj = CodeProject(
                                    title = "Challenge: ${chall.title}",
                                    language = chall.language,
                                    code = chall.templateCode
                                )
                                viewModel.onProjectSelected(localProj)
                                // Trigger active challenge mapping
                                onNavigateToEditor()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = "open", tint = DarkPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load and Solve in Code Editor", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tapping here loads the coding sandbox. Write, compile and satisfy test cases inside our full-screen editor workspace, then return here to complete milestones.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Manual mock submit if solved
                    Button(
                        onClick = {
                            viewModel.submitCodingChallengeSolution(chall.templateCode)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Evaluate Sandbox Logs", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // MAIN ACADEMY HOMEPAGE LISTINGS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mister Codes Academy", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(imageVector = Icons.Filled.School, contentDescription = "academy", tint = DarkPrimary, modifier = Modifier.size(28.dp))
            }

            // Tabs
            TabRow(
                selectedTabIndex = activeLearnTab,
                containerColor = Color(0xFF151D30),
                contentColor = DarkPrimary,
                modifier = Modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = activeLearnTab == 0,
                    onClick = { activeLearnTab = 0 },
                    text = { Text("Quizzes", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeLearnTab == 1,
                    onClick = { activeLearnTab = 1 },
                    text = { Text("Algorithm Roadmaps", fontWeight = FontWeight.Bold) }
                )
            }

            // Language Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(coderLanguages) { langName ->
                    val isFilterSelected = selectedLangFilter == langName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFilterSelected) DarkPrimary else Color(0xFF151D30))
                            .clickable { selectedLangFilter = langName }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = langName,
                            color = if (isFilterSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                if (activeLearnTab == 0) {
                    val filtered = challenges.filter { it.language == selectedLangFilter }
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No tutorials or quiz challenges yet in $selectedLangFilter.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(filtered) { ch ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { viewModel.selectActiveChallenge(ch) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .background(DarkPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = ch.difficulty.uppercase(),
                                                color = DarkPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(ch.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(ch.questionType, color = Color.Gray, fontSize = 11.sp)
                                    }

                                    // Completion checkbox marker
                                    if (ch.isCompleted) {
                                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "done", tint = Color.Green, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "solve", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: ROADMAPS STATIC EXERCISES
                    item {
                        Text(
                            text = "Core Developer Paths Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "All paths start at 0% Done. Tap complete steps to increment milestones progressively!",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(roadmaps) { rm ->
                        RoadmapCard(
                            techName = rm.techName,
                            description = rm.description,
                            progress = rm.progress,
                            onClick = { viewModel.incrementRoadmapProgress(rm.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Daily Academic & Algorithmic Projects",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "Complete daily algorithm assignments to gain 50 - 200 XP instantly!",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.8f)
                        )
                    }
                    
                    items(academicProjects) { proj ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = proj.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Level: ${proj.difficulty} | Reward: ${proj.xpReward} XP",
                                            color = DarkPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.completeAcademicProject(proj.id) },
                                        enabled = !proj.isCompleted,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (proj.isCompleted) Color.DarkGray else DarkPrimary,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (proj.isCompleted) {
                                            Icon(imageVector = Icons.Filled.Check, contentDescription = "done", modifier = Modifier.size(16.dp), tint = Color.LightGray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Completed", fontSize = 11.sp, color = Color.LightGray)
                                        } else {
                                            Text("Code & Run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(proj.description, color = Color.LightGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = proj.progress,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = DarkPrimary,
                                        trackColor = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${(proj.progress * 100).toInt()}% Prototyped",
                                        color = DarkPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoadmapCard(techName: String, description: String, progress: Float, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(techName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (progress >= 1.0f) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "completed", tint = Color.Green, modifier = Modifier.size(20.dp))
                } else {
                    Text("Tap to continue", color = DarkPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress >= 1.0f) Color.Green else DarkPrimary,
                    trackColor = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("${(progress * 100).toInt()}% Done", color = if (progress >= 1.0f) Color.Green else DarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 9. PROJECT WORKSPACE (PROJECTS CRUD)
// ==========================================
@Composable
fun ProjectsScreen(viewModel: MisterCodesViewModel, onNavigateToEditor: () -> Unit) {
    val allProjects by viewModel.projects.collectAsState()

    var showCreateForm by remember { mutableStateOf(false) }
    var projectTitleInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Python") }

    val availableLangs = listOf("Python", "Java", "C", "C++", "JavaScript", "HTML", "CSS", "PHP", "Kotlin", "Go", "Rust")
    var isDropDownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saved Workspaces", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            IconButton(
                onClick = { showCreateForm = !showCreateForm },
                modifier = Modifier
                    .background(DarkPrimary, CircleShape)
                    .size(36.dp)
                    .testTag("create_project_btn"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
            ) {
                Icon(imageVector = if (showCreateForm) Icons.Filled.Close else Icons.Filled.Add, contentDescription = "Add project icon")
            }
        }

        // SLIDE ON PROJECT CONSTRUCTION FORM
        AnimatedVisibility(visible = showCreateForm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Construct New Project", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = projectTitleInput,
                        onValueChange = { projectTitleInput = it },
                        label = { Text("Project / File Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DarkPrimary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Language Selector Dropdown inside form
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isDropDownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Language: $selectedLanguage")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "dropdown")
                        }

                        DropdownMenu(
                            expanded = isDropDownExpanded,
                            onDismissRequest = { isDropDownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF151D30))
                        ) {
                            availableLangs.forEach { lg ->
                                DropdownMenuItem(
                                    text = { Text(lg, color = Color.White) },
                                    onClick = {
                                        selectedLanguage = lg
                                        isDropDownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (projectTitleInput.isBlank()) return@Button
                            viewModel.onCreateNewProject(projectTitleInput, selectedLanguage)
                            projectTitleInput = ""
                            showCreateForm = false
                            onNavigateToEditor()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_create_proj")
                    ) {
                        Text("Create Workspace", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // WORKSPACE LIST
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            if (allProjects.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxHeight(0.6f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Outlined.FolderOff, contentDescription = "empty", tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No active workspace templates found", color = Color.Gray, fontSize = 14.sp)
                        Text("Tap button above to create Python/Kotlin project file.", color = Color.DarkGray, fontSize = 11.sp)
                    }
                }
            } else {
                items(allProjects) { p ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                viewModel.onProjectSelected(p)
                                onNavigateToEditor()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(DarkPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(p.language, color = DarkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                Text(p.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Click to open in IDE Sandbox tool", color = Color.Gray, fontSize = 11.sp)
                            }

                            // Delete Action
                            IconButton(
                                onClick = { viewModel.onDeleteProject(p.id) },
                                modifier = Modifier.testTag("delete_proj_${p.id}")
                            ) {
                                Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = "delete", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. COMMUNITY SHARINGS PANEL
// ==========================================
@Composable
fun CommunityScreen(
    viewModel: MisterCodesViewModel,
    onBack: () -> Unit,
    onNavigateToOtherProfile: (String) -> Unit
) {
    val snippets by viewModel.snippets.collectAsState()

    var showPostForm by remember { mutableStateOf(false) }
    var snipTitle by remember { mutableStateOf("") }
    var snipLang by remember { mutableStateOf("Kotlin") }
    var snipDesc by remember { mutableStateOf("") }
    var snipCode by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        // COMMUNITY TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Public Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = { showPostForm = !showPostForm },
                colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(imageVector = if (showPostForm) Icons.Filled.Close else Icons.Filled.Share, contentDescription = "share", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Post Snippet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // DIALOG COMPOSABLE DRAWER FOR SNIPPET POSTING
        AnimatedVisibility(visible = showPostForm) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151D30))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Share Code in Community Hub", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
                item {
                    OutlinedTextField(
                        value = snipTitle,
                        onValueChange = { snipTitle = it },
                        label = { Text("Snippet Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = DarkPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = snipDesc,
                        onValueChange = { snipDesc = it },
                        label = { Text("Details Description") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = DarkPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = snipCode,
                        onValueChange = { snipCode = it },
                        label = { Text("Raw Code block") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = DarkPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (snipTitle.isBlank() || snipCode.isBlank()) {
                                Toast.makeText(context, "Fill in Title and Code", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.shareSnippetInCommunity(snipTitle, snipLang, snipCode, snipDesc)
                            snipTitle = ""
                            snipDesc = ""
                            snipCode = ""
                            showPostForm = false
                            Toast.makeText(context, "Snippet shared with other coders success!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Publish Snip Feed", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SNIPPETS FEED
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(snippets) { sm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                ) {
                    var expandComments by remember { mutableStateOf(false) }
                    var commentTextInput by remember { mutableStateOf("") }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToOtherProfile(sm.author) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(DarkSecondary.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sm.author.take(1).uppercase(),
                                        color = DarkSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(sm.author, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(
                                        text = sm.language,
                                        color = DarkPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Bookmark toggle Button
                            IconButton(onClick = { viewModel.toggleSnippetBookmark(sm.id) }) {
                                Icon(
                                    imageVector = if (sm.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "bookmark",
                                    tint = if (sm.isBookmarked) DarkPrimary else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = sm.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = sm.description,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CODE PREVIEW WRAPPER CARD
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C111C))
                                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            val highlighted = highlightCode(sm.code, true)
                            Text(
                                text = highlighted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleSnippetLike(sm.id) }) {
                                    Icon(
                                        imageVector = if (sm.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "like count",
                                        tint = if (sm.isLiked) Color.Red else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text("${sm.upvotes} likes", color = Color.Gray, fontSize = 12.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { expandComments = !expandComments }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Chat, contentDescription = "comments", tint = if (expandComments) DarkPrimary else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${sm.commentsCount} comments", color = if (expandComments) DarkPrimary else Color.Gray, fontSize = 12.sp)
                            }
                        }

                        // Inline expanded comment pane
                        AnimatedVisibility(visible = expandComments) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val commentsList = if (sm.commentsJson.isBlank()) {
                                    emptyList()
                                } else {
                                    sm.commentsJson.split("|||")
                                }

                                if (commentsList.isEmpty()) {
                                    Text(
                                        text = "Be the first to share your opinion & feedback on this compilation! ✍️",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                } else {
                                    commentsList.forEach { rawComment ->
                                        val parts = rawComment.split(":")
                                        if (parts.size >= 2) {
                                            val cAuthor = parts[0]
                                            val cText = parts.subList(1, parts.size).joinToString(":")
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(DarkPrimary.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = cAuthor.take(1).uppercase(),
                                                        color = DarkPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(text = "@$cAuthor", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(text = cText, color = Color.LightGray, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = commentTextInput,
                                        onValueChange = { commentTextInput = it },
                                        placeholder = { Text("Write a response...", fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = DarkPrimary,
                                            unfocusedBorderColor = Color.DarkGray
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (commentTextInput.isNotBlank()) {
                                                viewModel.addCommentToSnippet(sm.id, commentTextInput)
                                                commentTextInput = ""
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = DarkPrimary)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Send, contentDescription = "submit comment", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 11. PROFILE SETTINGS
// ==========================================
@Composable
fun ProfileScreen(viewModel: MisterCodesViewModel, onBack: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    val dailyMissionsList by viewModel.dailyMissions.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    val pickPfpLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            var sizeInBytes: Long = 0
            try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        sizeInBytes = cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                // simple fallback
            }
            val maxBytes = 3 * 1024 * 1024 // 3 MB
            if (sizeInBytes > maxBytes) {
                Toast.makeText(context, "Image is larger than 3MB. Please select a smaller photo!", Toast.LENGTH_LONG).show()
            } else {
                viewModel.updateUserProfileAvatar(it.toString())
                Toast.makeText(context, "Profile picture updated successfully! 📸", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(profile) {
        profile?.let {
            usernameInput = it.username
            emailInput = it.email
            bioInput = it.bio
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("User Profile Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            TextButton(
                onClick = {
                    if (isEditing) {
                        // Double check username uniqueness
                        if (!viewModel.isUsernameAvailable(usernameInput) && usernameInput.trim().lowercase() != profile?.username?.trim()?.lowercase()) {
                            Toast.makeText(context, "Username already taken! Try another tag.", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        if (usernameInput.isBlank()) {
                            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        viewModel.updateProfileDetails(usernameInput, emailInput, bioInput)
                        Toast.makeText(context, "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                }
            ) {
                Text(
                    text = if (isEditing) "SAVE" else "EDIT PROFILE",
                    color = DarkPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // USER CARD
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(DarkPrimary.copy(alpha = 0.12f), CircleShape)
                            .border(2.dp, DarkPrimary, CircleShape)
                            .clickable {
                                pickPfpLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val pfpSeed = profile?.avatarSeed ?: ""
                        if (pfpSeed.startsWith("content://") || pfpSeed.startsWith("file://")) {
                            AsyncImage(
                                model = pfpSeed,
                                contentDescription = "avatar",
                                modifier = Modifier
                                    .size(98.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "avatar",
                                tint = DarkPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap avatar to choose photo from gallery (< 3MB)",
                        fontSize = 10.sp,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("MisterCodes @username") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = DarkPrimary, focusedBorderColor = DarkPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { /* fixed and immutable as requested */ },
                            label = { Text("Email Address (Fixed)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Gray,
                                unfocusedTextColor = Color.Gray,
                                focusedLabelColor = Color.Gray,
                                focusedBorderColor = Color.LightGray.copy(0.3f),
                                unfocusedBorderColor = Color.LightGray.copy(0.3f)
                            ),
                            enabled = false, // Email is fixed and cannot be changed!
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            label = { Text("Developer Bio / Tagline") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = DarkPrimary, focusedBorderColor = DarkPrimary),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "@" + (profile?.username ?: "CoderGuest"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(profile?.email ?: "guest@mistercodes.com", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val creationDateStr = remember(profile?.accountCreatedOn) {
                            val sdf = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(profile?.accountCreatedOn ?: System.currentTimeMillis()))
                        }
                        Text("Account Created On: $creationDateStr", color = Color.Gray.copy(alpha = 0.8f), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            profile?.bio ?: "No bio description set yet.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // LEVEL PROGRESSION LARGE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DEVELOPER ACCREDITATION", color = DarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Level ${profile?.level ?: 1}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Total XP Balance: ${formatXp(profile?.xp ?: 0.0)}", color = Color.LightGray, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatItem(title = "Streaks", count = "${profile?.currentStreak ?: 0} Days")
                            ProfileStatItem(title = "Solved Tasks", count = "${profile?.solvedChallengesCount ?: 0}")
                            ProfileStatItem(title = "XP Ranking", status = "Top 13%")
                        }
                    }
                }
            }

            // MILESTONES SEC
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Achievement Milestones", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    MilestoneRow(title = "Console Pioneer", desc = "Compile your first sandbox project", unlocked = profile?.consolePioneered == true)
                    MilestoneRow(title = "Algorithm Novice", desc = "Answer 3 academy questions correctly", unlocked = (profile?.solvedChallengesCount ?: 0) >= 3)
                    MilestoneRow(title = "AI Companion", desc = "Ask Gemini assistance to solve syntax error log", unlocked = profile?.aiGenerated == true)
                    MilestoneRow(title = "Community Contributor", desc = "Share snippet compilation with developer base", unlocked = profile?.sharedSnippetPosted == true)
                }
            }

            // DYNAMIC DAILY MISSIONS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Daily achievement Missions", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Claim missions to receive high XP rewards!", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(dailyMissionsList) { mission ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mission.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (mission.isBadaProject) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("EXCLUSIVE (HIGH XP) ⭐", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(mission.description, color = Color.LightGray, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reward: ${mission.xpReward} XP", color = DarkPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (mission.isCompleted) {
                            Button(
                                onClick = { /* already claimed */ },
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.DarkGray,
                                    disabledContainerColor = Color.DarkGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "claimed", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Claimed", fontSize = 11.sp, color = Color.LightGray)
                            }
                        } else if (mission.isReadyToClaim) {
                            Button(
                                onClick = { viewModel.claimDailyMission(mission.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkPrimary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("CLAIM", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val toastMsg = when (mission.id) {
                                        1 -> "Go run and compile some sandbox code in the Editor!"
                                        2 -> "Go to Community snippets and write response comments!"
                                        3 -> "Go attempt academy quizzes under Academy section!"
                                        4 -> "Complete one of the 0% Academic Projects in Learning Academy!"
                                        5 -> "Go ask Mister Codes AI for a helpful code review!"
                                        else -> "Complete the mission criteria to unlock!"
                                    }
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E2A47),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text("GO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Logouts
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.setLoginState(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f), contentColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.Logout, contentDescription = "logout", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out & Destroy Local Session", fontWeight = FontWeight.Bold)
                }
            }

            // 50% opaque signature copyright
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "app made and design by @slooserr",
                        color = Color.LightGray.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Copyright 2026 / All Rights Reserved",
                        color = Color.LightGray.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(title: String, count: String = "", status: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(count.ifEmpty { status }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun MilestoneRow(title: String, desc: String, unlocked: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (unlocked) DarkPrimary.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = "lock",
                    tint = if (unlocked) DarkPrimary else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (unlocked) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
            }
            if (unlocked) {
                Box(
                    modifier = Modifier
                        .background(Color.Green.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("UNLOCKED", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 12. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: MisterCodesViewModel, onBack: () -> Unit) {
    val fontValue by viewModel.editorFontSize.collectAsState()
    val isDarkState by viewModel.isDarkMode.collectAsState()
    val autoSaveState by viewModel.autoSaveEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings & Configuration", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TEXT EDITOR PREFERENCE
            item {
                Text("Editor Sandbox Workspace Configuration", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // AUTOSAVE TOGGLE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Continuous Sync Auto-Save", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Saves keystrokes on project modifications", color = Color.Gray, fontSize = 11.sp)
                            }

                            Switch(
                                checked = autoSaveState,
                                onCheckedChange = { viewModel.autoSaveEnabled.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = DarkPrimary, checkedTrackColor = DarkPrimary.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // BRAND DISCLOSURES SEC
            item {
                Text("Licensing & Operational Security", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B233A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Security, contentDescription = "shield", tint = DarkPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracted Key APK Disclosure Warning", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // FOOTER INFO
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Mister Codes IDE | v1.0.4 Release build", color = Color.DarkGray, fontSize = 11.sp)
                    Text("Code Anywhere. Learn Everywhere.", color = Color.DarkGray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun OtherProfileScreen(
    viewModel: MisterCodesViewModel,
    author: String,
    onBack: () -> Unit
) {
    val snippets by viewModel.snippets.collectAsState()
    val authorSnippets = snippets.filter { it.author.trim().lowercase() == author.trim().lowercase() }
    
    // Custom simulated attributes for each matching user to make it look hyper-realistic!
    val (bio, streak, levels, xp) = remember(author) {
        when (author.trim().lowercase()) {
            "ssdf" -> quadrupleOf(
                "Algorithmic wizard and competitive programmer. Former systems specialist.",
                "7 Days 🔥",
                "Level 5 - Expert",
                "1,450 XP"
            )
            "stylequeen" -> quadrupleOf(
                "UX/UI design expert turned mobile compiler architect. Coding beautiful Material 3 schemas.",
                "12 Days 🔥",
                "Level 8 - Master",
                "2,800 XP"
            )
            "flowmaster" -> quadrupleOf(
                "Passionate about stream processing, Kotlin Coroutines, and parallel systems architecture.",
                "0 Days ❄️ (Streak broken)",
                "Level 4 - Adv.",
                "930 XP"
            )
            "algosmaster" -> quadrupleOf(
                "Data structures tutor. Solving 3 compiler challenges a day.",
                "2 Days 🔥",
                "Level 3 - Int.",
                "450 XP"
            )
            else -> quadrupleOf(
                "Active developer contributing to public repository community boards.",
                "3 Days 🔥",
                "Level 2 - Novice",
                "280 XP"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .statusBarsPadding()
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Inspecting Developer Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // DEVELOPER HEADER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(DarkSecondary.copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, DarkSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = author.take(2).uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "@$author",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bio,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Activity Streak", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(streak, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Developer Rank", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(levels, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("XP Score", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(xp, color = DarkPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // SIMULATED GITHUB & NETWORKING STATUS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1220)),
                    border = BorderStroke(0.5.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MAPPED SERVER STATUS (MISTER-CODES CLOUD)", color = DarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Public, contentDescription = "Online", tint = Color.Green, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Firebase Realtime Registry", color = Color.White, fontSize = 13.sp)
                            }
                            Text("Synced", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "Upload status", tint = DarkSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google Cloud Storage Repo", color = Color.White, fontSize = 13.sp)
                            }
                            Text("Repository Connected", color = DarkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // AUTHOR SHARED PROJECTS BLOCK
            item {
                Text(
                    text = "Public Projects & Repositories (${authorSnippets.size})",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (authorSnippets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF151D30), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No public snippets available.", color = Color.Gray)
                    }
                }
            } else {
                items(authorSnippets) { snippet ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(snippet.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Box(
                                    modifier = Modifier
                                        .background(DarkPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(snippet.language, color = DarkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (snippet.description.isNotEmpty()) {
                                Text(
                                    text = snippet.description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action button to load their snippets directly into the main editor!
                            Button(
                                onClick = {
                                    viewModel.onLanguageChanged(snippet.language)
                                    viewModel.onEditorCodeChanged(snippet.code)
                                    onBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1524), contentColor = DarkPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Code, contentDescription = "Import", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Code into Workspace Editor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Quadruple helper data structure
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun <A, B, C, D> quadrupleOf(a: A, b: B, c: C, d: D): Quadruple<A, B, C, D> = Quadruple(a, b, c, d)
