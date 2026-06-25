package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val codeSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class MisterCodesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MisterCodesRepository.getInstance(application)

    // --- Flows from Database ---
    val projects: StateFlow<List<CodeProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<TutorialChallenge>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snippets: StateFlow<List<SharedSnippet>> = repository.allSharedSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val registeredUsers: StateFlow<List<RegisteredUser>> = repository.allRegisteredUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<UserActivityLog>> = repository.allActivityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addActivityLog(actionType: String, description: String) {
        viewModelScope.launch {
            repository.addActivityLog(actionType, description)
        }
    }

    private val replacementMissionPool = listOf(
        DailyMission(10, "Refactor JVM Memory", "Trace garbage collector references and clean dangling pointers in Sandbox.", 140),
        DailyMission(11, "Optimize DB Indexes", "Write composite query indexes for heavy relational SQL tables.", 150),
        DailyMission(12, "Design Custom UI Kit", "Create custom Material 3 themed components and primary action elements.", 120),
        DailyMission(13, "Integrate AI Vision API", "Send a canvas mock layout to Mister Codes to explain visual UI errors.", 160),
        DailyMission(14, "Unit Test Stateflows", "Ensure concurrent ViewModel states emit correct success states.", 130),
        DailyMission(15, "Deploy Cloud Function", "Write a secure serverless microservice to serialize user metrics.", 140),
        DailyMission(16, "Write Advanced RegExp", "Configure secure filters using complex regular expression pattern matching.", 110),
        DailyMission(17, "Implement Binary Tree", "Traverse root nodes in-order recursively to find deep leaves.", 150),
        DailyMission(18, "Solve Matrix Rotation", "Perform in-place 90 degree matrix rotation on nested arrays.", 160),
        DailyMission(19, "Write Custom Shaders", "Design dark ambient glow gradients using Kotlin and Compose Canvas.", 130)
    )

    // --- UI Active States ---
    private val _currentSelectedProject = MutableStateFlow<CodeProject?>(null)
    val currentSelectedProject = _currentSelectedProject.asStateFlow()

    private val _editorCodeText = MutableStateFlow("")
    val editorCodeText = _editorCodeText.asStateFlow()

    private val _isMultiFileProject = MutableStateFlow(false)
    val isMultiFileProject = _isMultiFileProject.asStateFlow()

    private val _multiFileMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val multiFileMap = _multiFileMap.asStateFlow()

    private val _selectedFilePath = MutableStateFlow("")
    val selectedFilePath = _selectedFilePath.asStateFlow()

    private val _editorLanguage = MutableStateFlow("Python")
    val editorLanguage = _editorLanguage.asStateFlow()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling = _isCompiling.asStateFlow()

    private val _consoleOutputResult = MutableStateFlow<CompilerOutput?>(null)
    val consoleOutputResult = _consoleOutputResult.asStateFlow()

    // Passcode security rotating system flows
    private val _workspacePasscode = MutableStateFlow("")
    val workspacePasscode = _workspacePasscode.asStateFlow()

    // Interactive JavaScript Simulated Variables for Sandbox
    val userLevelSimulated = MutableStateFlow(55)
    val loginStreakSimulated = MutableStateFlow(6)
    val isFirstTimePurchase = MutableStateFlow(true)

    // Secure local workspace locking status
    val isWorkspaceSecureLocked = MutableStateFlow(true)

    // Dynamic Academic/Algorithm Projects
    data class AcademicProject(
        val id: Int,
        val title: String,
        val description: String,
        val difficulty: String,
        val xpReward: Int,
        val progress: Float = 0.0f,
        val isCompleted: Boolean = false
    )

    // Dynamic Achievement Missions
    data class DailyMission(
        val id: Int,
        val title: String,
        val description: String,
        val xpReward: Int,
        val isCompleted: Boolean = false,
        val isBadaProject: Boolean = false,
        val isReadyToClaim: Boolean = false
    )

    private val _academicProjects = MutableStateFlow<List<AcademicProject>>(emptyList())
    val academicProjects = _academicProjects.asStateFlow()

    data class TechRoadmap(
        val id: Int,
        val techName: String,
        val description: String,
        val progress: Float = 0.0f,
        val isCompleted: Boolean = false
    )

    private val _roadmaps = MutableStateFlow<List<TechRoadmap>>(emptyList())
    val roadmaps = _roadmaps.asStateFlow()

    private val _dailyMissions = MutableStateFlow<List<DailyMission>>(emptyList())
    val dailyMissions = _dailyMissions.asStateFlow()

    // AI Assistant state
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText = _chatInputText.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    // Learning center active states
    private val _activeChallenge = MutableStateFlow<TutorialChallenge?>(null)
    val activeChallenge = _activeChallenge.asStateFlow()

    private val _quizSelectedOption = MutableStateFlow<Int?>(null)
    val quizSelectedOption = _quizSelectedOption.asStateFlow()

    private val _quizResultLabel = MutableStateFlow<String?>(null)
    val quizResultLabel = _quizResultLabel.asStateFlow()

    // App Preferences state
    val isDarkMode = MutableStateFlow(true)
    val editorFontSize = MutableStateFlow(14)
    val autoSaveEnabled = MutableStateFlow(true)

    // Inspected Community Author
    val selectedOtherProfileAuthor = MutableStateFlow<String?>(null)

    private fun generateRandomPasscode(): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val special = "#$@!&*-"
        val pool = upper + lower + digits + special
        
        // Guarantee 1 uppercase, 1 lowercase, 1 digit, 1 special character, and 2 randoms to make exactly 6 characters
        val list = mutableListOf(
            upper.random(),
            lower.random(),
            digits.random(),
            special.random()
        )
        repeat(2) {
            list.add(pool.random())
        }
        return list.shuffled().joinToString("")
    }

    private fun sendPushNotification(title: String, message: String) {
        val context = getApplication<Application>().applicationContext
        val channelId = "mistercodes_notifications"
        val channelName = "Mister Codes Alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                channelName, 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Passcode rotation and Milestone notification updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            
        try {
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        } catch (e: SecurityException) {
            // Notification channels gracefully fallback
        }
    }

    init {
        _workspacePasscode.value = generateRandomPasscode()

        _academicProjects.value = listOf(
            AcademicProject(1, "Interactive Algorithmic Binary Finder", "Implement high-speed binary search algorithms on sorted arrays with dynamic visualizations.", "Medium", 120),
            AcademicProject(2, "Real-time API Proxy Middleware in Go", "Build light channels, rate limiters, and endpoint mapping for backend microservices.", "Hard", 185),
            AcademicProject(3, "Modern Jetpack Compose Edge-to-Edge Forms", "Design forms with adaptive window inset padding and input verification rules.", "Easy", 75),
            AcademicProject(4, "Fast Fourier Transform Signal Visualization", "A mathematical filter to decompose signal inputs into frequencies reactively.", "Hard", 200),
            AcademicProject(5, "Asynchronous Thread Pool Worker in C++", "Manage scheduling queues, race condition mutexes, and workers pool manually.", "Medium", 140)
        )

        _roadmaps.value = listOf(
            TechRoadmap(1, "Mobile Junior Dev Kotlin", "Step 1: Variables -> Step 2: Lists -> Step 3: Flows & Coroutines -> Step 4: UI Compose Components."),
            TechRoadmap(2, "Data Analyst Python", "Step 1: Numpy maths -> Step 2: Pandas DataFrames -> Step 3: Matplotlib plots -> Step 4: Gemini Prompting."),
            TechRoadmap(3, "Full Stack JavaScript Web Developer", "Step 1: DOM Elements -> Step 2: Fetch & Promises -> Step 3: Express REST API servers -> Step 4: Postgres database.")
        )

        _dailyMissions.value = listOf(
            DailyMission(1, "Compile First Project", "Verify your draft or custom sandbox layout without compile errors.", 110),
            DailyMission(2, "Engage with Community Feed", "View live submissions and add a real comment on your peer's repository.", 130),
            DailyMission(3, "Complete 2 Algorithm Challenges", "Answer multiple academy quizzes and solve the coding puzzles correctly.", 150),
            DailyMission(4, "Full Stack Sandbox Integration", "Configure an offline SQLite database query simulation dynamically. ⭐ EXCLUSIVE HIGH-XP STORY", 250, isBadaProject = true),
            DailyMission(5, "AI Code Review Assistance", "Ask Gemini AI to review an asynchronous Kotlin thread implementation. ⭐ EXCLUSIVE HIGH-XP STORY", 300, isBadaProject = true)
        )

        viewModelScope.launch {
            repository.prepaidLaunchInitialization()
            // If active projects exist, select first python project
            _editorLanguage.value = "Python"
            _editorCodeText.value = "print(\"Hello, Mister Codes!\")\n"
        }

        // Passcode rotation 5-minute schedule loop
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(300000) // 5 minutes
                val newPasscode = generateRandomPasscode()
                _workspacePasscode.value = newPasscode
                sendPushNotification(
                    "🔐 Dynamic Passcode Rotated",
                    "Secret workspace passcode updated: $newPasscode"
                )
            }
        }

        // Streak check and notification simulation (approx every 12 seconds for responsive verification)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(12000)
                val profile = userProfile.value
                if (profile != null && profile.isLoggedIn) {
                    val now = System.currentTimeMillis()
                    val diffHours = (now - profile.lastActiveTime) / (1000L * 60 * 60)
                    if (diffHours >= 17 && diffHours < 24) {
                        sendPushNotification(
                            "🔥 Don't break your streak!",
                            "Laxmi! You haven't programmed today. Open Mister Codes to protect your ${profile.currentStreak}-day streak!"
                        )
                    } else if (diffHours in 24..47) {
                        sendPushNotification(
                            "⏰ Streak Safety Warning",
                            "Only a few hours left! Don't lose your ${profile.currentStreak} days streak!"
                        )
                    } else if (diffHours >= 48) {
                        val lostStreak = profile.currentStreak
                        if (lostStreak > 0) {
                            sendPushNotification(
                                "💔 You lost your streak!",
                                "Oh no! You lost your $lostStreak days active streak. Log in now to rebuild it from day 1!"
                            )
                            repository.saveProfile(profile.copy(currentStreak = 1, lastActiveTime = now))
                        }
                    }
                }
            }
        }

        // Real-time developer active progression simulation
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // check every 5 seconds
                val profile = userProfile.value
                if (profile != null && profile.isLoggedIn) {
                    // Check premium tier expiration
                    val nowTime = System.currentTimeMillis()
                    if (profile.isPremium && profile.premiumTier in listOf(1, 2) && nowTime > profile.premiumExpiresAt) {
                        val expiredProfile = profile.copy(
                            isPremium = false,
                            isTrial = false,
                            trialEndsAt = 0L,
                            premiumTier = 0,
                            premiumExpiresAt = 0L,
                            premiumIsCanceled = false
                        )
                        repository.saveProfile(expiredProfile)
                        sendPushNotification(
                            "💔 Premium Tier Expired!",
                            "Your premium subscription/trial has ended. Upgrade again to unlock professional status & themes!"
                        )
                        continue
                    }

                    val oldXp = profile.xp
                    // Choose an engaging random increment from 0.08 up to 1.70 for real-time bursts
                    val randomIncrement = 0.08 + (Math.random() * (1.70 - 0.08))
                    val roundedIncrement = Math.round(randomIncrement * 100.0) / 100.0
                    
                    // Dynamic Premium XP multiplier bonus based on tier
                    val multiplier = if (profile.isPremium) {
                        when (profile.premiumTier) {
                            1 -> 1.05  // Trial +5%
                            2 -> 1.25  // Temporary +25%
                            3 -> 1.50  // Permanent +50%
                            else -> 1.25
                        }
                    } else 1.0
                    val finalIncrement = Math.round(roundedIncrement * multiplier * 100.0) / 100.0
                    
                    val newXp = oldXp + finalIncrement
                    val newLevel = (newXp / 100.0).toInt() + 1

                    // check milestones
                    val milestones = listOf(10.0, 100.0, 1000.0, 10000.0, 100000.0)
                    for (m in milestones) {
                        if (oldXp < m && newXp >= m) {
                            sendPushNotification(
                                "🎉 XP Milestone Reached!",
                                "Congratulations Lakshmi! Your Developer XP reached ${m.toInt()}! Keep leveling!"
                            )
                        }
                    }

                    val updatedProfile = profile.copy(
                        xp = newXp,
                        level = newLevel
                    )
                    repository.saveProfile(updatedProfile)
                }
            }
        }
    }

    // --- Business Actions ---

    fun parseMultiFileCode(codeText: String): Boolean {
        if (!codeText.trim().startsWith("{\"isMultiFile\":true")) {
            _isMultiFileProject.value = false
            _multiFileMap.value = emptyMap()
            _selectedFilePath.value = ""
            return false
        }
        return try {
            val obj = org.json.JSONObject(codeText)
            val filesObj = obj.getJSONObject("files")
            val filesMap = mutableMapOf<String, String>()
            val keys = filesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                filesMap[key] = filesObj.getString(key)
            }
            val activePath = obj.optString("selectedFilePath", "")
            _isMultiFileProject.value = true
            _multiFileMap.value = filesMap
            _selectedFilePath.value = activePath
            _editorCodeText.value = filesMap[activePath] ?: ""
            true
        } catch (e: Exception) {
            _isMultiFileProject.value = false
            _multiFileMap.value = emptyMap()
            _selectedFilePath.value = ""
            false
        }
    }

    fun onProjectSelected(project: CodeProject) {
        _currentSelectedProject.value = project
        _editorLanguage.value = project.language
        if (!parseMultiFileCode(project.code)) {
            _editorCodeText.value = project.code
        }
    }

    fun onEditorCodeChanged(newText: String) {
        _editorCodeText.value = newText
        val activeProj = _currentSelectedProject.value ?: return
        
        viewModelScope.launch {
            val codeToSave: String
            if (_isMultiFileProject.value) {
                val path = _selectedFilePath.value
                val updatedMap = _multiFileMap.value.toMutableMap()
                if (path.isNotEmpty()) {
                    updatedMap[path] = newText
                    _multiFileMap.value = updatedMap
                }
                
                val obj = org.json.JSONObject()
                obj.put("isMultiFile", true)
                obj.put("selectedFilePath", path)
                val filesObj = org.json.JSONObject()
                for ((k, v) in updatedMap) {
                    filesObj.put(k, v)
                }
                obj.put("files", filesObj)
                codeToSave = obj.toString()
            } else {
                codeToSave = newText
            }
            
            if (autoSaveEnabled.value) {
                repository.saveProject(activeProj.copy(code = codeToSave, lastModified = System.currentTimeMillis()))
            }
        }
    }

    fun onSelectFile(path: String) {
        val activeProj = _currentSelectedProject.value ?: return
        val currentText = _editorCodeText.value
        val pathBefore = _selectedFilePath.value
        
        val updatedMap = _multiFileMap.value.toMutableMap()
        if (pathBefore.isNotEmpty()) {
            updatedMap[pathBefore] = currentText
        }
        _multiFileMap.value = updatedMap
        _selectedFilePath.value = path
        _editorCodeText.value = updatedMap[path] ?: ""
        
        viewModelScope.launch {
            val obj = org.json.JSONObject()
            obj.put("isMultiFile", true)
            obj.put("selectedFilePath", path)
            val filesObj = org.json.JSONObject()
            for ((k, v) in updatedMap) {
                filesObj.put(k, v)
            }
            obj.put("files", filesObj)
            
            repository.saveProject(activeProj.copy(code = obj.toString(), lastModified = System.currentTimeMillis()))
        }
    }

    fun addNewFileToProject(filePath: String, templateContent: String = "") {
        val activeProj = _currentSelectedProject.value ?: return
        if (!_isMultiFileProject.value) {
            _isMultiFileProject.value = true
            val initialMap = mapOf(
                "main.py" to _editorCodeText.value,
                filePath to templateContent
            )
            _multiFileMap.value = initialMap
            _selectedFilePath.value = filePath
            _editorCodeText.value = templateContent
        } else {
            val updatedMap = _multiFileMap.value.toMutableMap()
            updatedMap[filePath] = templateContent
            _multiFileMap.value = updatedMap
            _selectedFilePath.value = filePath
            _editorCodeText.value = templateContent
        }
        
        viewModelScope.launch {
            val obj = org.json.JSONObject()
            obj.put("isMultiFile", true)
            obj.put("selectedFilePath", _selectedFilePath.value)
            val filesObj = org.json.JSONObject()
            for ((k, v) in _multiFileMap.value) {
                filesObj.put(k, v)
            }
            obj.put("files", filesObj)
            
            repository.saveProject(activeProj.copy(code = obj.toString(), lastModified = System.currentTimeMillis()))
        }
    }

    fun deleteFileFromProject(filePath: String) {
        val activeProj = _currentSelectedProject.value ?: return
        if (!_isMultiFileProject.value) return
        
        val updatedMap = _multiFileMap.value.toMutableMap()
        updatedMap.remove(filePath)
        _multiFileMap.value = updatedMap
        
        if (_selectedFilePath.value == filePath) {
            val nextPath = updatedMap.keys.firstOrNull() ?: ""
            _selectedFilePath.value = nextPath
            _editorCodeText.value = updatedMap[nextPath] ?: ""
        }
        
        viewModelScope.launch {
            val obj = org.json.JSONObject()
            obj.put("isMultiFile", true)
            obj.put("selectedFilePath", _selectedFilePath.value)
            val filesObj = org.json.JSONObject()
            for ((k, v) in _multiFileMap.value) {
                filesObj.put(k, v)
            }
            obj.put("files", filesObj)
            
            repository.saveProject(activeProj.copy(code = obj.toString(), lastModified = System.currentTimeMillis()))
        }
    }

    fun generateProjectStructure(templateName: String, customPrefix: String) {
        viewModelScope.launch {
            val files = mutableMapOf<String, String>()
            val selectedPath: String
            
            val cleanPrefix = if (customPrefix.isBlank()) "" else {
                if (customPrefix.trim().endsWith("/")) customPrefix.trim() else "${customPrefix.trim()}/"
            }
            val appPkg = if (customPrefix.isNotBlank()) customPrefix.trim().replace("/", ".") else "com.example.myapp"
            
            when (templateName) {
                "Android (Jetpack Compose MVVM)" -> {
                    val pfx = if (cleanPrefix.isEmpty()) "app/src/main/java/com/example/myapp/" else cleanPrefix
                    selectedPath = "${pfx}MainActivity.kt"
                    files["${pfx}MainActivity.kt"] = "package $appPkg\n\nimport android.os.Bundle\nimport androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\nimport androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Surface\nimport androidx.compose.ui.Modifier\nimport $appPkg.ui.screens.HomeScreen\nimport $appPkg.ui.theme.MyAppTheme\n\nclass MainActivity : ComponentActivity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        setContent {\n            MyAppTheme {\n                Surface(\n                    modifier = Modifier.fillMaxSize(),\n                    color = MaterialTheme.colorScheme.background\n                ) {\n                    HomeScreen()\n                }\n            }\n        }\n    }\n}\n"
                    files["${pfx}ui/theme/Theme.kt"] = "package $appPkg.ui.theme\n\nimport androidx.compose.foundation.isSystemInDarkTheme\nimport androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.darkColorScheme\nimport androidx.compose.material3.lightColorScheme\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun MyAppTheme(\n    darkTheme: Boolean = isSystemInDarkTheme(),\n    content: @Composable () -> Unit\n) {\n    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()\n    MaterialTheme(\n        colorScheme = colors,\n        content = content\n    )\n}\n"
                    files["${pfx}ui/screens/HomeScreen.kt"] = "package $appPkg.ui.screens\n\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.material3.*\nimport androidx.compose.runtime.*\nimport androidx.compose.ui.Alignment\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.unit.dp\n\n@Composable\nfun HomeScreen() {\n    Box(\n        modifier = Modifier.fillMaxSize(),\n        contentAlignment = Alignment.Center\n    ) {\n        Column(horizontalAlignment = Alignment.CenterHorizontally) {\n            Text(\"Welcome to Android Jetpack Compose!\", style = MaterialTheme.typography.headlineMedium)\n            Spacer(modifier = Modifier.height(16.dp))\n            Button(onClick = {}) {\n                Text(\"Explore App\")\n            }\n        }\n    }\n}\n"
                    files["${pfx}ui/viewmodel/HomeViewModel.kt"] = "package $appPkg.ui.viewmodel\n\nimport androidx.lifecycle.ViewModel\nimport kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n\nclass HomeViewModel : ViewModel() {\n    private val _uiState = MutableStateFlow(\"Idle\")\n    val uiState = _uiState.asStateFlow()\n}\n"
                    files["${pfx}data/model/User.kt"] = "package $appPkg.data.model\n\ndata class User(\n    val id: String,\n    val name: String,\n    val email: String\n)\n"
                    files["${pfx}data/repository/UserRepository.kt"] = "package $appPkg.data.repository\n\nimport $appPkg.data.model.User\n\nclass UserRepository {\n    suspend fun fetchUser(): User {\n        return User(\"1\", \"Mister Coder\", \"mister@coder.com\")\n    }\n}\n"
                }
                "React SPA (Boilerplate)" -> {
                    val pfx = if (cleanPrefix.isEmpty()) "src/" else cleanPrefix
                    selectedPath = "${pfx}App.js"
                    files["${pfx}index.js"] = "import React from 'react';\nimport ReactDOM from 'react-dom/client';\nimport './index.css';\nimport App from './App';\n\nconst root = ReactDOM.createRoot(document.getElementById('root'));\nroot.render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>\n);\n"
                    files["${pfx}App.js"] = "import React from 'react';\nimport Header from './components/Header';\nimport Dashboard from './components/Dashboard';\nimport { AuthProvider } from './context/AuthContext';\n\nfunction App() {\n  return (\n    <AuthProvider>\n      <div className=\"app-container\">\n        <Header />\n        <main className=\"main-content\">\n          <Dashboard />\n        </main>\n      </div>\n    </AuthProvider>\n  );\n}\n\nexport default App;\n"
                    files["${pfx}components/Header.jsx"] = "import React from 'react';\n\nfunction Header() {\n  return (\n    <header style={{ padding: '1rem', background: '#2c3e50', color: 'white' }}>\n      <h2>Developer Studio Dashboard</h2>\n    </header>\n  );\n}\n\nexport default Header;\n"
                    files["${pfx}components/Dashboard.jsx"] = "import React from 'react';\n\nfunction Dashboard() {\n  return (\n    <div style={{ padding: '2rem' }}>\n      <h3>Workspace Statistics</h3>\n      <p>Welcome back! Code compilation sandbox is active and running.</p>\n    </div>\n  );\n}\n\nexport default Dashboard;\n"
                    files["${pfx}context/AuthContext.js"] = "import React, { createContext, useState } from 'react';\n\nexport const AuthContext = createContext();\n\nexport function AuthProvider({ children }) {\n  const [user, setUser] = useState({ name: 'Guest Developer' });\n  return (\n    <AuthContext.Provider value={{ user, setUser }}>\n      {children}\n    </AuthContext.Provider>\n  );\n}\n"
                    files["public/index.html"] = "<!DOCTYPE html>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"utf-8\" />\n    <title>React Workspace Boilerplate</title>\n  </head>\n  <body>\n    <div id=\"root\"></div>\n  </body>\n</html>\n"
                    files["package.json"] = "{\n  \"name\": \"react-boilerplate-workspace\",\n  \"version\": \"1.0.0\",\n  \"dependencies\": {\n    \"react\": \"^18.2.0\",\n    \"react-dom\": \"^18.2.0\"\n  }\n}\n"
                }
                "Node.js Express & MongoDB" -> {
                    val pfx = if (cleanPrefix.isEmpty()) "src/" else cleanPrefix
                    selectedPath = "${pfx}app.js"
                    files["${pfx}app.js"] = "const express = require('express');\nconst connectDB = require('./config/db');\nconst userRoutes = require('./routes/userRoutes');\nconst { errorHandler } = require('./middleware/auth');\n\nconst app = express();\napp.use(express.json());\n\nconnectDB();\n\napp.use('/api/users', userRoutes);\napp.use(errorHandler);\n\nconst PORT = process.env.PORT || 5000;\napp.listen(PORT, () => console.log(`Server running on port \${PORT}`));\n"
                    files["${pfx}config/db.js"] = "const mongoose = require('mongoose');\n\nconst connectDB = async () => {\n  try {\n    console.log(\"MongoDB Connected to Cloud Sandbox...\");\n  } catch (err) {\n    console.error(err.message);\n    process.exit(1);\n  }\n};\n\nmodule.exports = connectDB;\n"
                    files["${pfx}models/User.js"] = "const mongoose = require('mongoose');\n\nconst UserSchema = new mongoose.Schema({\n  name: { type: String, required: true },\n  email: { type: String, required: true, unique: true },\n  password: { type: String, required: true }\n});\n\nmodule.exports = mongoose.model('User', UserSchema);\n"
                    files["${pfx}routes/userRoutes.js"] = "const express = require('express');\nconst router = express.Router();\nconst { registerUser } = require('../controllers/userController');\n\nrouter.post('/register', registerUser);\n\nmodule.exports = router;\n"
                    files["${pfx}controllers/userController.js"] = "const registerUser = async (req, res) => {\n  const { name, email, password } = req.body;\n  res.status(201).json({\n    success: true,\n    user: { name, email }\n  });\n};\n\nmodule.exports = { registerUser };\n"
                    files["${pfx}middleware/auth.js"] = "const protect = (req, res, next) => {\n  const token = req.headers.authorization;\n  if (!token) return res.status(401).json({ msg: 'Unauthorized' });\n  next();\n};\n\nconst errorHandler = (err, req, res, next) => {\n  res.status(500).json({ error: err.message });\n};\n\nmodule.exports = { protect, errorHandler };\n"
                }
                "FastAPI Python Backend" -> {
                    val pfx = if (cleanPrefix.isEmpty()) "app/" else cleanPrefix
                    selectedPath = "${pfx}main.py"
                    files["${pfx}main.py"] = "from fastapi import FastAPI\nfrom app.api import endpoints\nfrom app.core.config import settings\n\napp = FastAPI(title=\"MisterCodes Python API Service\", version=\"1.0.0\")\n\napp.include_router(endpoints.router, prefix=\"/api/v1\")\n\n@app.get(\"/\")\ndef root():\n    return {\"message\": \"Welcome to FastAPI sandbox backend.\"}\n"
                    files["${pfx}core/config.py"] = "class Settings:\n    PROJECT_NAME: str = \"FastAPI Sandbox\"\n    API_V1_STR: str = \"/api/v1\"\n    \nsettings = Settings()\n"
                    files["${pfx}api/endpoints.py"] = "from fastapi import APIRouter\nfrom app.models.schemas import User\n\nrouter = APIRouter()\n\n@router.get(\"/users/me\", response_model=User)\ndef read_user():\n    return {\"id\": \"101\", \"name\": \"Python Coder\", \"email\": \"python@coder.com\"}\n"
                    files["${pfx}models/schemas.py"] = "from pydantic import BaseModel\n\nclass User(BaseModel):\n    id: str\n    name: str\n    email: str\n"
                    files["${pfx}database/session.py"] = "def get_db():\n    print(\"Initializing sqlite engine session...\")\n    try:\n        yield \"SessionActive\"\n    finally:\n        print(\"Closing session.\")\n"
                    files["requirements.txt"] = "fastapi>=0.100.0\nuvicorn>=0.22.0\npydantic>=2.0\n"
                }
                "Go Clean Architecture" -> {
                    selectedPath = "cmd/server/main.go"
                    files["cmd/server/main.go"] = "package main\n\nimport (\n    \"fmt\"\n    \"net/http\"\n)\n\nfunc main() {\n    fmt.Println(\"Starting Clean Architecture Go Server on port 8080...\")\n    http.HandleFunc(\"/user\", func(w http.ResponseWriter, r *http.Request) {\n        w.Write([]byte(`{\"status\":\"success\",\"user\":\"Go Coder\"}`))\n    })\n    http.ListenAndServe(\":8080\", nil)\n}\n"
                    files["internal/domain/user.go"] = "package domain\n\ntype User struct {\n    ID    string `json:\"id\"`\n    Name  string `json:\"name\"`\n    Email string `json:\"email\"`\n}\n"
                    files["internal/repository/user_repo.go"] = "package repository\n\nimport \"internal/domain\"\n\ntype UserRepo struct{}\n\nfunc (r *UserRepo) FetchUser() domain.User {\n    return domain.User{ID: \"go_1\", Name: \"Golang Coder\", Email: \"go@coder.com\"}\n}\n"
                    files["internal/usecase/user_usecase.go"] = "package usecase\n\nimport (\n    \"internal/domain\"\n    \"internal/repository\"\n)\n\ntype UserUsecase struct {\n    repo repository.UserRepo\n}\n\nfunc (u *UserUsecase) GetUser() domain.User {\n    return u.repo.FetchUser()\n}\n"
                    files["internal/handler/user_handler.go"] = "package handler\n\nimport \"internal/usecase\"\n\ntype UserHandler struct {\n    uc usecase.UserUsecase\n}\n"
                    files["go.mod"] = "module go-clean-arch-microservice\n\ngo 1.20\n"
                }
                else -> {
                    selectedPath = "main.py"
                    files["main.py"] = "# Code entry-point\nprint('Default workspace')\n"
                }
            }

            val jsonObj = org.json.JSONObject()
            jsonObj.put("isMultiFile", true)
            jsonObj.put("selectedFilePath", selectedPath)
            
            val filesObj = org.json.JSONObject()
            for ((k, v) in files) {
                filesObj.put(k, v)
            }
            jsonObj.put("files", filesObj)

            val serializedCode = jsonObj.toString()

            val activeProj = _currentSelectedProject.value
            if (activeProj != null) {
                val updatedProj = activeProj.copy(code = serializedCode, language = when(templateName) {
                    "Android (Jetpack Compose MVVM)" -> "Kotlin"
                    "React SPA (Boilerplate)" -> "JavaScript"
                    "Node.js Express & MongoDB" -> "JavaScript"
                    "FastAPI Python Backend" -> "Python"
                    "Go Clean Architecture" -> "Go"
                    else -> "Python"
                }, lastModified = System.currentTimeMillis())
                repository.saveProject(updatedProj)
                onProjectSelected(updatedProj)
            } else {
                val newId = repository.createNewProject("Scaffolded $templateName", when(templateName) {
                    "Android (Jetpack Compose MVVM)" -> "Kotlin"
                    "React SPA (Boilerplate)" -> "JavaScript"
                    "Node.js Express & MongoDB" -> "JavaScript"
                    "FastAPI Python Backend" -> "Python"
                    "Go Clean Architecture" -> "Go"
                    else -> "Python"
                }, serializedCode)
                val created = repository.getProjectById(newId.toInt())
                if (created != null) {
                    onProjectSelected(created)
                }
            }
        }
    }

    fun onLanguageChanged(newLang: String) {
        _editorLanguage.value = newLang
    }

    fun onCreateNewProject(title: String, language: String) {
        viewModelScope.launch {
            val template = getTemplateForLanguage(language)
            val newId = repository.createNewProject(title, language, template)
            val created = repository.getProjectById(newId.toInt())
            if (created != null) {
                onProjectSelected(created)
            }
        }
    }

    fun onDeleteProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_currentSelectedProject.value?.id == id) {
                _currentSelectedProject.value = null
                _editorCodeText.value = ""
            }
        }
    }

    // Compiler Sandbox Sim Trigger
    fun runActiveCode() {
        _isCompiling.value = true
        _consoleOutputResult.value = null
        viewModelScope.launch {
            val currentLanguage = _editorLanguage.value
            val currentCode = _editorCodeText.value
            val output = repository.executeCodeWithCompileSimulator(currentLanguage, currentCode)
            _consoleOutputResult.value = output
            _isCompiling.value = false

            // Check roadmap milestones based on actual sandbox code execution
            checkRoadmapCodeExecution(currentLanguage, currentCode)

            // Mark Milestone 1 ("Console Pioneer")
            val profile = userProfile.value
            if (profile != null && !profile.consolePioneered) {
                repository.saveProfile(profile.copy(consolePioneered = true))
            }
            markMissionReadyToClaim(1)
            addActivityLog("COMPILE", "Compiled and executed $currentLanguage project code in Sandbox compiler simulator.")
        }
    }

    fun checkRoadmapCodeExecution(language: String, code: String) {
        val list = _roadmaps.value.map { rm ->
            if (rm.isCompleted) return@map rm
            
            var advanced = false
            var completedStepName = ""
            var nextStepHint = ""
            
            when (rm.id) {
                1 -> { // Mobile Junior Dev Kotlin
                    if (language.equals("Kotlin", ignoreCase = true)) {
                        when (rm.progress) {
                            0.0f -> {
                                if (code.contains("val ") || code.contains("var ")) {
                                    advanced = true
                                    completedStepName = "Step 1: Variables"
                                    nextStepHint = "Next: Write code using Lists (e.g., 'listOf' or 'List')"
                                }
                            }
                            0.25f -> {
                                if (code.contains("List", ignoreCase = true) || code.contains("listOf") || code.contains("Array")) {
                                    advanced = true
                                    completedStepName = "Step 2: Lists"
                                    nextStepHint = "Next: Write code using Flows & Coroutines (e.g., 'launch', 'delay', or 'Flow')"
                                }
                            }
                            0.50f -> {
                                if (code.contains("Flow") || code.contains("launch") || code.contains("delay") || code.contains("coroutine") || code.contains("GlobalScope")) {
                                    advanced = true
                                    completedStepName = "Step 3: Flows & Coroutines"
                                    nextStepHint = "Next: Write code using UI Compose Components (e.g., '@Composable' or 'Button')"
                                }
                            }
                            0.75f -> {
                                if (code.contains("@Composable") || code.contains("Compose") || code.contains("Button") || code.contains("Text")) {
                                    advanced = true
                                    completedStepName = "Step 4: UI Compose Components"
                                    nextStepHint = "Path Completed!"
                                }
                            }
                        }
                    }
                }
                2 -> { // Data Analyst Python
                    if (language.equals("Python", ignoreCase = true)) {
                        when (rm.progress) {
                            0.0f -> {
                                if (code.contains("numpy") || code.contains("np.")) {
                                    advanced = true
                                    completedStepName = "Step 1: Numpy maths"
                                    nextStepHint = "Next: Write code using Pandas DataFrames (e.g., 'pandas' or 'DataFrame')"
                                }
                            }
                            0.25f -> {
                                if (code.contains("pandas") || code.contains("pd.") || code.contains("DataFrame")) {
                                    advanced = true
                                    completedStepName = "Step 2: Pandas DataFrames"
                                    nextStepHint = "Next: Write code using Matplotlib plots (e.g., 'matplotlib' or 'plt.')"
                                }
                            }
                            0.50f -> {
                                if (code.contains("matplotlib") || code.contains("plt.")) {
                                    advanced = true
                                    completedStepName = "Step 3: Matplotlib plots"
                                    nextStepHint = "Next: Write code using Gemini Prompting (e.g., 'gemini', 'prompt', or 'ai')"
                                }
                            }
                            0.75f -> {
                                if (code.contains("gemini") || code.contains("prompt") || code.contains("ai")) {
                                    advanced = true
                                    completedStepName = "Step 4: Gemini Prompting"
                                    nextStepHint = "Path Completed!"
                                }
                            }
                        }
                    }
                }
                3 -> { // Full Stack JavaScript Web Developer
                    if (language.equals("JavaScript", ignoreCase = true) || language.equals("TypeScript", ignoreCase = true)) {
                        when (rm.progress) {
                            0.0f -> {
                                if (code.contains("document.") || code.contains("element") || code.contains("window")) {
                                    advanced = true
                                    completedStepName = "Step 1: DOM Elements"
                                    nextStepHint = "Next: Write asynchronous requests (e.g., 'fetch' or 'Promise')"
                                }
                            }
                            0.25f -> {
                                if (code.contains("fetch") || code.contains("Promise")) {
                                    advanced = true
                                    completedStepName = "Step 2: Fetch & Promises"
                                    nextStepHint = "Next: Create an Express REST API server (e.g., 'express' or 'app.get')"
                                }
                            }
                            0.50f -> {
                                if (code.contains("express") || code.contains("app.get") || code.contains("app.post")) {
                                    advanced = true
                                    completedStepName = "Step 3: Express REST API servers"
                                    nextStepHint = "Next: Query Postgres database (e.g., 'postgres', 'sql', or 'SELECT')"
                                }
                            }
                            0.75f -> {
                                if (code.contains("postgres") || code.contains("pg") || code.contains("sql") || code.contains("SELECT")) {
                                    advanced = true
                                    completedStepName = "Step 4: Postgres database"
                                    nextStepHint = "Path Completed!"
                                }
                            }
                        }
                    }
                }
            }
            
            if (advanced) {
                val nextProgress = rm.progress + 0.25f
                if (nextProgress >= 1.0f) {
                    sendPushNotification(
                        "🗺️ Roadmap Path Completed!",
                        "Congratulations! You fully completed the '${rm.techName}' path in your project sandbox!"
                    )
                    rm.copy(progress = 1.0f, isCompleted = true)
                } else {
                    sendPushNotification(
                        "🗺️ Roadmap Step Completed!",
                        "Successfully unlocked '$completedStepName' for '${rm.techName}'! $nextStepHint"
                    )
                    rm.copy(progress = nextProgress)
                }
            } else {
                rm
            }
        }
        _roadmaps.value = list
    }

    // AI Chat Assistant operations
    fun onChatInputChanged(text: String) {
        _chatInputText.value = text
    }

    fun sendChatPrompt(customPrompt: String? = null) {
        val userText = customPrompt ?: _chatInputText.value
        if (userText.isBlank()) return

        // Clear input box
        if (customPrompt == null) _chatInputText.value = ""

        val userMessage = ChatMessage("user", userText)
        _chatHistory.value = _chatHistory.value + userMessage
        _aiLoading.value = true

        viewModelScope.launch {
            val systemMsg = """
                You are 'Mister Codes AI' - a friendly, highly intelligent coding mentor and copilot.
                If requested, write clean, optimized, production-ready code with helpful explanations.
                Always encourage the developer. Explain algorithms in plain, accessible language with crisp design insights.
            """.trimIndent()

            val response = repository.askAssistant(userText, systemMsg)
            val aiMessage = ChatMessage("ai", response)
            _chatHistory.value = _chatHistory.value + aiMessage
            _aiLoading.value = false
            markMissionReadyToClaim(5)
        }
    }

    // Dedicated AI Assist shortcuts (Generate, Explain, Debug, Optimize, Convert)
    fun requestAiShortcuts(actionType: String, argLanguage: String = _editorLanguage.value) {
        val snippet = _editorCodeText.value
        val prompt = when (actionType) {
            "EXPLAIN" -> "Explain the following $argLanguage code simply, detailing the logic step-by-step:\n\n```$argLanguage\n$snippet\n```"
            "FIX_BUGS" -> "Find and correct any syntax or runtime bugs in the following $argLanguage code, providing the revised safe version first:\n\n```$argLanguage\n$snippet\n```"
            "OPTIMIZE" -> "Optimize this $argLanguage code for time/space complexity, explain what changed and show the optimal code:\n\n```$argLanguage\n$snippet\n```"
            "COMMENTS" -> "Analyze and add elegant docstrings, inline code definitions, and explanations to this $argLanguage snippet:\n\n```$argLanguage\n$snippet\n```"
            "CONVERT" -> "Convert this $argLanguage code into other programmers standard readable form in JavaScript, Python or modern Kotlin. The code is:\n\n```$argLanguage\n$snippet\n```"
            else -> "Give coding suggestions on my project:\n\n```$argLanguage\n$snippet\n```"
        }
        sendChatPrompt(prompt)
    }

    // Quiz action triggers
    fun selectActiveChallenge(challenge: TutorialChallenge) {
        _activeChallenge.value = challenge
        _quizSelectedOption.value = null
        _quizResultLabel.value = null
    }

    fun onQuizOptionSelected(index: Int) {
        _quizSelectedOption.value = index
    }

    fun verifyQuizAnswer() {
        val challenge = _activeChallenge.value ?: return
        val selected = _quizSelectedOption.value ?: return

        val correctIndex = challenge.correctAnswer.toIntOrNull() ?: 0
        if (selected == correctIndex) {
            _quizResultLabel.value = "Correct! +50 XP"
            viewModelScope.launch {
                repository.submitChallengeSolution(challenge.id, "Selected option: $selected", isCompleted = true)
                // Refresh active challenge state
                _activeChallenge.value = challenge.copy(isCompleted = true, userSolution = "Selected option: $selected")
                markMissionReadyToClaim(3)
                addActivityLog("QUIZ", "Successfully solved Academy MCQ Quiz: '${challenge.title}'")
            }
        } else {
            _quizResultLabel.value = "Incorrect. Try reading the question again!"
        }
    }

    // Solving coding problems
    fun submitCodingChallengeSolution(code: String) {
        val challenge = _activeChallenge.value ?: return
        _isCompiling.value = true
        viewModelScope.launch {
            // Run it against expected outputs
            val testCaseInput = challenge.testCasesJson
            val res = repository.executeCodeWithCompileSimulator(challenge.language, code)
            
            val isPassed = res.success && !res.hasErrors && (res.output.contains(testCaseInput) || testCaseInput.isBlank())
            
            _isCompiling.value = false
            if (isPassed) {
                _quizResultLabel.value = "All unit tests passed! Excellent coding! +50 XP"
                repository.submitChallengeSolution(challenge.id, code, isCompleted = true)
                _activeChallenge.value = challenge.copy(isCompleted = true, userSolution = code)
                markMissionReadyToClaim(3)
                addActivityLog("CHALLENGE", "Passed all unit tests and resolved coding problem: '${challenge.title}'")
            } else {
                _quizResultLabel.value = "Failed! Error details:\n${res.errors.ifEmpty { "Output mismatch. Output: " + res.output + " but expected: " + testCaseInput }}"
            }
        }
    }

    // Community Shares
    fun shareSnippetInCommunity(title: String, language: String, code: String, description: String) {
        viewModelScope.launch {
            repository.shareSnippet(title, language, code, description)

            // Mark Milestone 4 ("Community Contributor")
            val p = userProfile.value
            if (p != null && !p.sharedSnippetPosted) {
                repository.saveProfile(p.copy(sharedSnippetPosted = true))
            }
        }
    }

    fun toggleSnippetLike(snippetId: Int) {
        viewModelScope.launch {
            repository.toggleSnippetLike(snippetId)
            markMissionReadyToClaim(2)
        }
    }

    fun toggleSnippetBookmark(snippetId: Int) {
        viewModelScope.launch {
            repository.toggleSnippetBookmark(snippetId)
            markMissionReadyToClaim(2)
        }
    }

    fun addCommentToSnippet(snippetId: Int, text: String) {
        viewModelScope.launch {
            val user = userProfile.value ?: UserProfile()
            repository.addCommentToSnippet(snippetId, user.username, text)
            markMissionReadyToClaim(2)
        }
    }

    fun completeAcademicProject(id: Int) {
        viewModelScope.launch {
            val list = _academicProjects.value.map {
                if (it.id == id && !it.isCompleted) {
                    val nextProgress = it.progress + 0.25f
                    if (nextProgress >= 1.0f) {
                        val reward = it.xpReward
                        val profile = userProfile.value
                        if (profile != null) {
                            val multiplier = if (profile.isPremium) 1.25 else 1.0
                            val finalReward = Math.round(reward * multiplier * 100.0) / 100.0
                            val newXp = profile.xp + finalReward
                            val newLevel = (newXp / 100.0).toInt() + 1
                            repository.saveProfile(profile.copy(xp = newXp, level = newLevel))
                            sendPushNotification(
                                "🎓 Academic Project Completed!",
                                "You finished '${it.title}' and gained $finalReward XP!"
                            )
                        }
                        markMissionReadyToClaim(4)
                        it.copy(progress = 1.0f, isCompleted = true)
                    } else {
                        it.copy(progress = nextProgress)
                    }
                } else {
                    it
                }
            }
            _academicProjects.value = list
        }
    }

    fun incrementRoadmapProgress(id: Int) {
        val list = _roadmaps.value.map {
            if (it.id == id && !it.isCompleted) {
                val nextProgress = it.progress + 0.25f
                if (nextProgress >= 1.0f) {
                    sendPushNotification(
                        "🗺️ Roadmap Path Completed!",
                        "You fully completed the '${it.techName}' track! Great job!"
                    )
                    it.copy(progress = 1.0f, isCompleted = true)
                } else {
                    it.copy(progress = nextProgress)
                }
            } else {
                it
            }
        }
        _roadmaps.value = list
    }

    fun markMissionReadyToClaim(id: Int) {
        val list = _dailyMissions.value.map {
            if (it.id == id && !it.isCompleted) {
                if (!it.isReadyToClaim) {
                    sendPushNotification(
                        "🔓 Mission Ready!",
                        "You matched criteria for: '${it.title}'! Clime XP now."
                    )
                }
                it.copy(isReadyToClaim = true)
            } else {
                it
            }
        }
        _dailyMissions.value = list
    }

    fun claimDailyMission(id: Int) {
        viewModelScope.launch {
            var rewardClaimed = 0
            var missionTitle = ""
            val currentMissions = _dailyMissions.value
            val nextMissions = currentMissions.toMutableList()

            val idx = nextMissions.indexOfFirst { it.id == id && !it.isCompleted && it.isReadyToClaim }
            if (idx != -1) {
                val oldMission = nextMissions[idx]
                rewardClaimed = oldMission.xpReward
                missionTitle = oldMission.title

                // Find a replacement mission from pool whose ID is not already used in currentMissions
                val existingIds = nextMissions.map { it.id }.toSet()
                val candidate = replacementMissionPool.firstOrNull { it.id !in existingIds }
                    ?: DailyMission(
                        id = oldMission.id + 100,
                        title = "Extreme Coding Mastery",
                        description = "Optimize complex relational queries in the active database.",
                        xpReward = 180
                    )

                // Replace the mission at index with candidate
                nextMissions[idx] = candidate

                // Save Profile / Reward XP
                val profile = userProfile.value
                if (profile != null) {
                    val multiplier = if (profile.isPremium) 1.25 else 1.0
                    val finalReward = Math.round(rewardClaimed * multiplier * 100.0) / 100.0
                    val newXp = profile.xp + finalReward
                    val newLevel = (newXp / 100.0).toInt() + 1
                    repository.saveProfile(profile.copy(xp = newXp, level = newLevel))
                    sendPushNotification(
                        "🏆 Milestone Achieved!",
                        "Finished: '$missionTitle', claimed $finalReward XP! Replaced with: '${candidate.title}'"
                    )
                    addActivityLog(
                        "CLAIM_MISSION",
                        "Completed Daily Mission: '$missionTitle' and earned $finalReward XP. Replaced with '${candidate.title}'"
                    )
                }
            }
            _dailyMissions.value = nextMissions
        }
    }

    // Sign out & mock login
    fun setLoginState(isLoggedIn: Boolean) {
        viewModelScope.launch {
            repository.toggleLoginState(isLoggedIn)
            if (!isLoggedIn) {
                // Clear state
                _currentSelectedProject.value = null
                _editorCodeText.value = ""
            }
        }
    }

    fun isUsernameAvailable(usernameToTest: String): Boolean {
        val trimmed = usernameToTest.trim().lowercase()
        // Taken usernames (either reserved names or other simulated github-style users in community feed)
        val taken = listOf("ssdf", "stylequeen", "flowmaster", "algosmaster", "mistercoder")
        return !taken.contains(trimmed)
    }

    fun performSignup(username: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val ok = repository.registerNewUser(username, email, password)
            if (ok) {
                addActivityLog("AUTH_SIGNUP", "Created new developer account with username @$username")
                onSuccess()
            } else {
                onError("This Email is already registered! Please log in instead.")
            }
        }
    }

    fun performLogin(emailOrUsername: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val ok = repository.performLogin(emailOrUsername, password)
            if (ok) {
                addActivityLog("AUTH_LOGIN", "Logged into system terminal securely as $emailOrUsername")
                onSuccess()
            } else {
                onError("Incorrect username/email or password!")
            }
        }
    }

    fun updateProfileDetails(username: String, email: String, bio: String, avatarSeed: String? = null) {
        viewModelScope.launch {
            val curr = userProfile.value ?: UserProfile()
            val finalAvatar = avatarSeed ?: curr.avatarSeed
            repository.updateProfileWithDetails(username, email, bio, finalAvatar)
        }
    }

    fun updateUserProfileAvatar(uriString: String) {
        viewModelScope.launch {
            val curr = userProfile.value ?: UserProfile()
            repository.updateProfileWithDetails(curr.username, curr.email, curr.bio, uriString)
            addActivityLog("PFP_CHANGE", "Updated profile picture photo from device gallery")
        }
    }

    fun activatePremiumStatus() {
        viewModelScope.launch {
            val curr = userProfile.value ?: UserProfile()
            val updated = curr.copy(isPremium = true)
            repository.saveProfile(updated)
            addActivityLog("PREMIUM", "Activated Premium Profile package with dynamic theme, verified badge, and border animations!")
            sendPushNotification("👑 Premium Profile Activated!", "Congratulations! You are now a Verified Premium Developer! 🚀")
        }
    }

    fun generateCodeFromPrompt(prompt: String, language: String) {
        _aiLoading.value = true
        viewModelScope.launch {
            val systemMsg = """
                You are a real-world code generation compiler. 
                Generate valid code in the '$language' programming language for the prompt requested.
                Always include some basic inline code comments explaining the algorithm or components.
                Make the output fully complete, ready-to-run, and clean.
                Do NOT use markdown triple backticks like ```$language ... ```.
                Return ONLY the raw runnable code blocks.
            """.trimIndent()
            val response = repository.askAssistant(prompt, systemMsg)
            _editorCodeText.value = cleanMarkdownBlocks(response)
            _editorLanguage.value = language
            _aiLoading.value = false

            // Mark Milestone 3 ("AI Companion")
            val p = userProfile.value
            if (p != null && !p.aiGenerated) {
                repository.saveProfile(p.copy(aiGenerated = true))
            }
        }
    }

    private fun cleanMarkdownBlocks(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            val firstLineEnd = clean.indexOf('\n')
            if (firstLineEnd != -1) {
                clean = clean.substring(firstLineEnd + 1)
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length - 3)
            }
        }
        return clean.trim()
    }

    fun updateProfileAvatar(newSeed: String) {
        viewModelScope.launch {
            val curr = userProfile.value ?: UserProfile()
            repository.updateProfileWithDetails(curr.username, curr.email, curr.bio, newSeed)
        }
    }

    fun activatePremiumTier(tier: Int, transactionId: String, utr: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: UserProfile()
            val isFirstTime = isFirstTimePurchase.value
            
            val durationMs = when (tier) {
                1 -> 24 * 3600 * 1000L // 24 hours
                2 -> if (isFirstTime) 45L * 24 * 3600 * 1000L else 30L * 24 * 3600 * 1000L
                3 -> 365L * 24 * 3600 * 1000L
                else -> 0L
            }
            
            val expiresAt = System.currentTimeMillis() + durationMs
            
            // Give 1500 XP one-time bonus
            val updatedXp = profile.xp + 1500.0
            val updatedLevel = (updatedXp / 100.0).toInt() + 1
            
            val updatedProfile = profile.copy(
                isPremium = true,
                isTrial = (tier == 1),
                trialEndsAt = if (tier == 1) expiresAt else 0L,
                premiumTier = tier,
                premiumExpiresAt = expiresAt,
                premiumActivatedAt = System.currentTimeMillis(),
                premiumIsCanceled = false,
                xp = updatedXp,
                level = updatedLevel
            )
            repository.saveProfile(updatedProfile)
            
            val tierName = when (tier) {
                1 -> "Silver Trial Tier"
                2 -> "Blue Temporary Tier"
                3 -> "Gold Permanent Tier"
                else -> "Premium Tier"
            }
            
            sendPushNotification(
                "👑 $tierName Activated!",
                "Premium Active! Received +1500 XP bonus and professional features!"
            )
            addActivityLog(
                "PREMIUM_ACTIVATED",
                "Activated Premium ($tierName) - Trx: $transactionId, UTR: $utr. Received 1500 XP!"
            )
            
            // Toggle first-time purchase
            isFirstTimePurchase.value = false
        }
    }

    fun cancelPremiumTier() {
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            val tier = profile.premiumTier
            val activatedAt = profile.premiumActivatedAt
            val isFirstTime = isFirstTimePurchase.value
            
            when (tier) {
                1 -> {
                    // Trial: Non-refundable, cancel anytime. Deactivates trial immediately.
                    val updated = profile.copy(
                        isPremium = false,
                        isTrial = false,
                        premiumTier = 0,
                        premiumExpiresAt = 0L,
                        trialEndsAt = 0L,
                        premiumIsCanceled = false
                    )
                    repository.saveProfile(updated)
                    sendPushNotification("💔 Trial Cancelled", "Your Silver Trial premium tier has been cancelled. No refund is issued.")
                    addActivityLog("PREMIUM_CANCELLED", "Cancelled Silver Trial plan. Non-refundable.")
                }
                2 -> {
                    // Temporary: 25% refundable. Cancel anytime. Active until period ends.
                    val paidPrice = if (isFirstTime) 25.0 else 29.0
                    val refundAmt = paidPrice * 0.25
                    val updated = profile.copy(
                        premiumIsCanceled = true
                    )
                    repository.saveProfile(updated)
                    sendPushNotification("💔 Temporary Subscription Cancelled", "Cancelled Blue subscription. Refund of ₹${String.format("%.2f", refundAmt)} (25%) initiated. Subscription remains active until period ends.")
                    addActivityLog("PREMIUM_CANCELLED", "Cancelled Temporary Monthly subscription. 25% refund of ₹${String.format("%.2f", refundAmt)} initiated. Subscription remains active.")
                }
                3 -> {
                    // Permanent: Refundable within 3 months (90 days).
                    val diffDays = (System.currentTimeMillis() - activatedAt) / (24 * 3600 * 1000L)
                    if (diffDays <= 90) {
                        val refundAmt = 99.0 * 0.89
                        val updated = profile.copy(
                            isPremium = false,
                            premiumTier = 0,
                            premiumExpiresAt = 0L,
                            premiumIsCanceled = false
                        )
                        repository.saveProfile(updated)
                        sendPushNotification("💔 Permanent Tier Refunded", "Cancelled Golden plan within 3 months. Refund of ₹${String.format("%.2f", refundAmt)} (89%) initiated. Premium deactivated.")
                        addActivityLog("PREMIUM_CANCELLED", "Cancelled Permanent plan within 90 days. 89% refund of ₹${String.format("%.2f", refundAmt)} initiated. Premium deactivated.")
                    } else {
                        sendPushNotification("⚠️ Cancellation Failed", "Permanent plan can only be cancelled within the first 3 months (90 days).")
                    }
                }
            }
        }
    }

    fun reactivatePremiumTier() {
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            if (profile.premiumTier == 2 && profile.premiumIsCanceled) {
                val updated = profile.copy(
                    premiumIsCanceled = false
                )
                repository.saveProfile(updated)
                sendPushNotification("⚡ Subscription Reactivated", "Your temporary premium subscription is active again!")
                addActivityLog("PREMIUM_REACTIVATED", "Reactivated canceled temporary monthly subscription.")
            }
        }
    }

    fun updatePremiumProfile(
        github: String,
        instagram: String,
        gitProfile: String,
        linkedin: String,
        website: String,
        animation: String
    ) {
        viewModelScope.launch {
            val profile = userProfile.value ?: UserProfile()
            val updatedProfile = profile.copy(
                githubLink = github.trim(),
                instagramLink = instagram.trim(),
                gitProfileLink = gitProfile.trim(),
                linkedinLink = linkedin.trim(),
                websiteLink = website.trim(),
                selectedAnimation = animation
            )
            repository.saveProfile(updatedProfile)
            sendPushNotification("✨ Profile Updated!", "Your professional links and premium animations are updated successfully.")
        }
    }

    private fun getTemplateForLanguage(language: String): String {
        return when (language) {
            "Python" -> "def main():\n    print(\"Hello world!\")\n\nif __name__ == '__main__':\n    main()\n"
            "Java" -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Mister Codes!\");\n    }\n}\n"
            "C" -> "#include <stdio.h>\n\nint main() {\n    printf(\"Hello from C!\\n\");\n    return 0;\n}\n"
            "C++" -> "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello from C++!\" << endl;\n    return 0;\n}\n"
            "JavaScript" -> "const run = () => {\n    console.log(\"Hello from Node.js!\");\n    let val = 5 + 10;\n    console.log(\"Sum:\", val);\n};\n\nrun();\n"
            "HTML" -> "<!DOCTYPE html>\n<html>\n<head>\n    <style>body { font-family: sans-serif; background: #e0f7fa; color: #006064; text-align: center; }</style>\n</head>\n<body>\n    <h1>Hello from HTML Live preview!</h1>\n    <p>Code anywhere, learn everywhere.</p>\n</body>\n</html>\n"
            "CSS" -> "body {\n    background-color: #0d1117;\n    color: #ffffff;\n    font-family: 'JetBrains Mono', monospace;\n}\n"
            "PHP" -> "<?php\n\necho \"Hello from PHP backend runner!\\n\";\n"
            "Kotlin" -> "fun main() {\n    val greeting = \"Hello from Jetpack Kotlin!\"\n    println(greeting)\n}\n"
            "Go" -> "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Go fast, code simple.\")\n}\n"
            "Rust" -> "fn main() {\n    println!(\"Hello from safe Rust memory sandbox!\");\n}\n"
            else -> "// Enter your code here...\n"
        }
    }
}
