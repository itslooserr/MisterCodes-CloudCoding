package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            val viewModel: MisterCodesViewModel = viewModel()
            val isDarkSystem by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkSystem, dynamicColor = false) {
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
                        MisterCodesHub.ASSISTANT -> AiAssistantScreen(
                            viewModel = viewModel
                        )
                        MisterCodesHub.LEARNING -> LearningScreen(
                            viewModel = viewModel,
                            onNavigateToEditor = { activeTab = MisterCodesHub.EDITOR }
                        )
                        MisterCodesHub.PROJECTS -> ProjectsScreen(
                            viewModel = viewModel,
                            onNavigateToEditor = { activeTab = MisterCodesHub.EDITOR }
                        )
                    }
                }
            }
        }
    }
}
