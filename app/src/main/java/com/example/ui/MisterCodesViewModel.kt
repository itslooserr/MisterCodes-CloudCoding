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

    // --- UI Active States ---
    private val _currentSelectedProject = MutableStateFlow<CodeProject?>(null)
    val currentSelectedProject = _currentSelectedProject.asStateFlow()

    private val _editorCodeText = MutableStateFlow("")
    val editorCodeText = _editorCodeText.asStateFlow()

    private val _editorLanguage = MutableStateFlow("Python")
    val editorLanguage = _editorLanguage.asStateFlow()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling = _isCompiling.asStateFlow()

    private val _consoleOutputResult = MutableStateFlow<CompilerOutput?>(null)
    val consoleOutputResult = _consoleOutputResult.asStateFlow()

    // Passcode security rotating system flows
    private val _workspacePasscode = MutableStateFlow("")
    val workspacePasscode = _workspacePasscode.asStateFlow()

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
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
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
                    val oldXp = profile.xp
                    // Choose an engaging random increment from 0.08 up to 1.70 for real-time bursts
                    val randomIncrement = 0.08 + (Math.random() * (1.70 - 0.08))
                    val roundedIncrement = Math.round(randomIncrement * 100.0) / 100.0
                    val newXp = oldXp + roundedIncrement
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

    fun onProjectSelected(project: CodeProject) {
        _currentSelectedProject.value = project
        _editorCodeText.value = project.code
        _editorLanguage.value = project.language
    }

    fun onEditorCodeChanged(newText: String) {
        _editorCodeText.value = newText
        // Auto-save if enabled and a project is selected
        val activeProj = _currentSelectedProject.value
        if (autoSaveEnabled.value && activeProj != null) {
            viewModelScope.launch {
                repository.saveProject(activeProj.copy(code = newText, lastModified = System.currentTimeMillis()))
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
            val output = repository.executeCodeWithCompileSimulator(_editorLanguage.value, _editorCodeText.value)
            _consoleOutputResult.value = output
            _isCompiling.value = false

            // Mark Milestone 1 ("Console Pioneer")
            val profile = userProfile.value
            if (profile != null && !profile.consolePioneered) {
                repository.saveProfile(profile.copy(consolePioneered = true))
            }
            markMissionReadyToClaim(1)
        }
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
                            val newXp = profile.xp + reward
                            val newLevel = (newXp / 100.0).toInt() + 1
                            repository.saveProfile(profile.copy(xp = newXp, level = newLevel))
                            sendPushNotification(
                                "🎓 Academic Project Completed!",
                                "You finished '${it.title}' and gained $reward XP!"
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
            val list = _dailyMissions.value.map {
                if (it.id == id && !it.isCompleted && it.isReadyToClaim) {
                    val reward = it.xpReward
                    val profile = userProfile.value
                    if (profile != null) {
                        val newXp = profile.xp + reward
                        val newLevel = (newXp / 100.0).toInt() + 1
                        repository.saveProfile(profile.copy(xp = newXp, level = newLevel))
                        sendPushNotification(
                            "🏆 Milestone Achieved!",
                            "Mission unlocked: '${it.title}', claimed $reward XP!"
                        )
                    }
                    it.copy(isCompleted = true)
                } else {
                    it
                }
            }
            _dailyMissions.value = list
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
