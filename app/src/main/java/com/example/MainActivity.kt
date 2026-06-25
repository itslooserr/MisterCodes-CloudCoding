package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRate()
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            val viewModel: MisterCodesViewModel = viewModel()
            val isDarkSystem by viewModel.isDarkMode.collectAsState()
            val userProfile by viewModel.userProfile.collectAsState()
            val isPremiumTheme = userProfile?.isPremium == true && userProfile?.premiumTier == 3

            MyApplicationTheme(darkTheme = isDarkSystem, dynamicColor = false, isPremium = isPremiumTheme) {
                val outerNavController = rememberNavController()

                NavHost(
                    navController = outerNavController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Splash Screen
                    composable("splash") {
                        SplashScreen(
                            viewModel = viewModel,
                            onNavigateToMain = {
                                outerNavController.navigate("main") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToAuth = {
                                outerNavController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToOnboarding = {
                                outerNavController.navigate("onboarding") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. Onboarding
                    composable("onboarding") {
                        OnboardingScreen(
                            onNavigateToAuth = {
                                outerNavController.navigate("auth") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 3. Auth (Login/Signup / Guest Mode)
                    composable("auth") {
                        AuthScreen(
                            viewModel = viewModel,
                            onNavToMain = {
                                outerNavController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 4. Main WORKSPACE (Dashboard + Tabs)
                    composable("main") {
                        MainHubContainer(
                            viewModel = viewModel,
                            onLogout = {
                                outerNavController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                
                val modes = display?.supportedModes
                if (!modes.isNullOrEmpty()) {
                    val highestMode = modes.maxWithOrNull(
                        compareBy<android.view.Display.Mode> { it.refreshRate }
                            .thenBy { it.physicalWidth }
                            .thenBy { it.physicalHeight }
                    )
                    
                    if (highestMode != null) {
                        val params = window.attributes
                        params.preferredDisplayModeId = highestMode.modeId
                        window.attributes = params
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun MainHubContainer(viewModel: MisterCodesViewModel, onLogout: () -> Unit) {
    var activeTab by remember { mutableStateOf(MisterCodesHub.DASHBOARD) }

    // Navigation stack within core views
    var activeSubView by remember { mutableStateOf("tabs") } // "tabs", "community", "profile", "settings", "console"

    val userProfile by viewModel.userProfile.collectAsState()

    // Automatic sign-out observer
    LaunchedEffect(userProfile) {
        if (userProfile != null && !userProfile!!.isLoggedIn) {
            onLogout()
        }
    }

    Scaffold(
        bottomBar = {
            if (activeSubView == "tabs") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    NavigationBarItem(
                        selected = activeTab == MisterCodesHub.DASHBOARD,
                        onClick = { activeTab = MisterCodesHub.DASHBOARD },
                        icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home index", modifier = Modifier.size(24.dp)) },
                        label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    NavigationBarItem(
                        selected = activeTab == MisterCodesHub.EDITOR,
                        onClick = { activeTab = MisterCodesHub.EDITOR },
                        icon = { Icon(imageVector = Icons.Filled.Terminal, contentDescription = "Editor tool", modifier = Modifier.size(24.dp)) },
                        label = { Text("Editor", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_tab_editor")
                    )

                    NavigationBarItem(
                        selected = activeTab == MisterCodesHub.ASSISTANT,
                        onClick = { activeTab = MisterCodesHub.ASSISTANT },
                        icon = { Icon(imageVector = Icons.Filled.Psychology, contentDescription = "AI assistant", modifier = Modifier.size(24.dp)) },
                        label = { Text("Coach", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_tab_assistant")
                    )

                    NavigationBarItem(
                        selected = activeTab == MisterCodesHub.LEARNING,
                        onClick = { activeTab = MisterCodesHub.LEARNING },
                        icon = { Icon(imageVector = Icons.Filled.School, contentDescription = "Academy lessons", modifier = Modifier.size(24.dp)) },
                        label = { Text("Academy", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_tab_academy")
                    )

                    NavigationBarItem(
                        selected = activeTab == MisterCodesHub.PROJECTS,
                        onClick = { activeTab = MisterCodesHub.PROJECTS },
                        icon = { Icon(imageVector = Icons.Filled.FolderCopy, contentDescription = "Files folder", modifier = Modifier.size(24.dp)) },
                        label = { Text("Files", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_tab_projects")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeSubView) {
                "community" -> {
                    CommunityScreen(
                        viewModel = viewModel,
                        onBack = { activeSubView = "tabs" },
                        onNavigateToOtherProfile = { author ->
                            viewModel.selectedOtherProfileAuthor.value = author
                            activeSubView = "other_profile"
                        }
                    )
                }
                "profile" -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { activeSubView = "tabs" }
                    )
                }
                "other_profile" -> {
                    val authorName = viewModel.selectedOtherProfileAuthor.collectAsState().value ?: ""
                    OtherProfileScreen(
                        viewModel = viewModel,
                        author = authorName,
                        onBack = { activeSubView = "community" }
                    )
                }
                "settings" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSubView = "tabs" }
                    )
                }
                "console" -> {
                    ConsoleScreen(
                        viewModel = viewModel,
                        onNavigateBack = { activeSubView = "tabs" },
                        onNavigateToAssistant = {
                            activeTab = MisterCodesHub.ASSISTANT
                            activeSubView = "tabs"
                        }
                    )
                }
                else -> {
                    // Main workspace tabs
                    when (activeTab) {
                        MisterCodesHub.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateHub = { tab -> activeTab = tab },
                            onNavigateToCommunity = { activeSubView = "community" },
                            onNavigateToProfile = { activeSubView = "profile" },
                            onNavigateToSettings = { activeSubView = "settings" }
                        )
                        MisterCodesHub.EDITOR -> CodeEditorScreen(
                            viewModel = viewModel,
                            onNavigateToConsole = { activeSubView = "console" },
                            onNavigateToAssistant = { activeTab = MisterCodesHub.ASSISTANT }
                        )
                        MisterCodesHub.ASSISTANT -> {
                            AiAssistantScreen(viewModel = viewModel)
                        }
                        MisterCodesHub.LEARNING -> LearningScreen(
                            viewModel = viewModel,
                            onNavigateToEditor = { activeTab = MisterCodesHub.EDITOR }
                        )
                        MisterCodesHub.PROJECTS -> {
                            ProjectsScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { activeTab = MisterCodesHub.EDITOR }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumLockPlaceholder(featureName: String, onNavigateToProfile: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151D30)),
            border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFFD700).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Feature Locked",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Premium Workspace Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "The $featureName is an advanced professional feature. Upgrade to Premium Profile to unlock full visual themes, premium styling, and 25% extra XP!",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onNavigateToProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Go to Profile to Activate 🚀", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
