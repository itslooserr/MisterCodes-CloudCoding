package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class MisterCodesRepository private constructor(context: Context) {

    private val db: MisterCodesDatabase = Room.databaseBuilder(
        context.applicationContext,
        MisterCodesDatabase::class.java,
        "mister_codes_database"
    ).fallbackToDestructiveMigration()
        .build()

    private val dao = db.dao()
    private val gemini = GeminiHelper()
    private val hasInitialized = AtomicBoolean(false)

    // Flows
    val allProjects: Flow<List<CodeProject>> = dao.getAllProjectsFlow()
    val allChallenges: Flow<List<TutorialChallenge>> = dao.getAllChallengesFlow()
    val allSharedSnippets: Flow<List<SharedSnippet>> = dao.getAllSharedSnippetsFlow()
    val userProfile: Flow<UserProfile?> = dao.getUserProfileFlow()

    companion object {
        @Volatile
        private var INSTANCE: MisterCodesRepository? = null

        fun getInstance(context: Context): MisterCodesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MisterCodesRepository(context).also { INSTANCE = it }
            }
        }
    }

    // Initialize/Pre-populate sample data
    suspend fun prepaidLaunchInitialization() {
        if (hasInitialized.getAndSet(true)) return

        // 1. Prepopulation of User Profile
        val existingProfile = dao.getUserProfile()
        if (existingProfile == null) {
            saveProfile(
                UserProfile(
                    id = 1,
                    username = "MisterCoder",
                    email = "mistercodes@codes.com",
                    avatarSeed = "seed_dev_7",
                    xp = 10.0,
                    level = 1,
                    currentStreak = 1,
                    solvedChallengesCount = 0,
                    isLoggedIn = false
                )
            )
        }

        // 2. Prepopulate Projects Workspace if empty
        val currentProj = dao.getAllProjects()
        if (currentProj.isEmpty()) {
            dao.insertProject(
                CodeProject(
                    title = "HelloWorld",
                    language = "Python",
                    code = "print(\"Hello, Mister Codes!\")\n\n# Let's write code here!\nfor i in range(5):\n    print(f\"Iteration {i+1}\")\n"
                )
            )
            dao.insertProject(
                CodeProject(
                    title = "SimpleServer",
                    language = "Go",
                    code = "package main\n\nimport (\n\t\"fmt\"\n\t\"net/http\"\n)\n\nfunc main() {\n\tfmt.Println(\"Server starting on :8080\")\n\thttp.HandleFunc(\"/\", func(w http.ResponseWriter, r *http.Request) {\n\t\tfmt.Fprintf(w, \"Hello from Go IDE!\")\n\t})\n}\n"
                )
            )
            dao.insertProject(
                CodeProject(
                    title = "CounterScreen",
                    language = "Kotlin",
                    code = "import androidx.compose.runtime.*\nimport androidx.compose.material3.*\n\n@Composable\nfun Counter() {\n    var count by remember { mutableStateOf(0) }\n    Button(onClick = { count++ }) {\n        Text(\"Clicked \\\$count times\")\n    }\n}\n"
                )
            )
        }

        // 3. Prepopulate Challenges / Learning Center if empty
        val listAllChall = dao.getAllChallengesFlow().firstOrNull() ?: emptyList()
        if (listAllChall.isEmpty()) {
            dao.insertChallenges(getSampleChallenges())
        }

        // 4. Prepopulate Community snippets if empty
        val snippets = dao.getAllSharedSnippetsFlow().firstOrNull() ?: emptyList()
        if (snippets.isEmpty()) {
            dao.insertSharedSnippets(getSampleSnippets())
        }

        // 5. Update active day streak
        updateStreakStamp()
    }

    // Project Actions
    suspend fun saveProject(project: CodeProject) {
        if (project.id == 0) {
            dao.insertProject(project)
        } else {
            dao.updateProject(project)
        }
    }

    suspend fun createNewProject(title: String, language: String, fileTemplate: String): Long {
        val project = CodeProject(
            title = title,
            language = language,
            code = fileTemplate
        )
        return dao.insertProject(project)
    }

    suspend fun deleteProject(id: Int) {
        dao.deleteProjectById(id)
    }

    suspend fun getProjectById(id: Int): CodeProject? {
        return dao.getProjectById(id)
    }

    // Solutions / Tutorials Actions
    suspend fun submitChallengeSolution(challengeId: Int, solution: String, isCompleted: Boolean) {
        // Find challenge and update
        allChallenges.firstOrNull()?.find { it.id == challengeId }?.let { original ->
            val updated = original.copy(
                userSolution = solution,
                isCompleted = isCompleted
            )
            dao.updateChallenge(updated)

            // Reward XP if freshly completed
            if (isCompleted && !original.isCompleted) {
                val profile = dao.getUserProfile() ?: UserProfile()
                val updatedXp = profile.xp + 50.0
                saveProfile(
                    profile.copy(
                        xp = updatedXp,
                        level = (updatedXp / 100).toInt() + 1,
                        solvedChallengesCount = profile.solvedChallengesCount + 1,
                        currentStreak = if (profile.currentStreak == 0) 1 else profile.currentStreak
                    )
                )
            }
        }
    }

    // Community Snips
    suspend fun shareSnippet(title: String, language: String, code: String, description: String) {
        val user = dao.getUserProfile() ?: UserProfile()
        dao.insertSharedSnippet(
            SharedSnippet(
                title = title,
                language = language,
                code = code,
                description = description,
                author = user.username,
                avatarSeed = user.avatarSeed,
                upvotes = 1,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleSnippetBookmark(snippetId: Int) {
        allSharedSnippets.firstOrNull()?.find { it.id == snippetId }?.let { original ->
            dao.updateSharedSnippet(original.copy(isBookmarked = !original.isBookmarked))
        }
    }

    suspend fun addCommentToSnippet(snippetId: Int, author: String, text: String) {
        allSharedSnippets.firstOrNull()?.find { it.id == snippetId }?.let { original ->
            val cleanAuthor = author.ifBlank { "AnonDev" }
            val cleanText = text.replace(":", " ").replace("|", " ").trim()
            if (cleanText.isNotBlank()) {
                val newCommentSegment = "$cleanAuthor:$cleanText"
                val updatedJson = if (original.commentsJson.isBlank()) {
                    newCommentSegment
                } else {
                    original.commentsJson + "|||" + newCommentSegment
                }
                val updatedCount = original.commentsCount + 1
                dao.updateSharedSnippet(original.copy(commentsJson = updatedJson, commentsCount = updatedCount))
            }
        }
    }

    suspend fun toggleSnippetLike(snippetId: Int) {
        allSharedSnippets.firstOrNull()?.find { it.id == snippetId }?.let { original ->
            val isCurrentlyLiked = original.isLiked
            val up = if (isCurrentlyLiked) original.upvotes - 1 else original.upvotes + 1
            dao.updateSharedSnippet(original.copy(isLiked = !isCurrentlyLiked, upvotes = up))
        }
    }

    // Unified profile sync saver
    suspend fun saveProfile(profile: UserProfile) {
        dao.insertUserProfile(profile)
        if (profile.email.isNotBlank()) {
            dao.insertRegisteredUser(
                RegisteredUser(
                    email = profile.email,
                    username = profile.username,
                    bio = profile.bio,
                    avatarSeed = profile.avatarSeed,
                    xp = profile.xp,
                    level = profile.level,
                    currentStreak = profile.currentStreak,
                    solvedChallengesCount = profile.solvedChallengesCount,
                    isLoggedIn = profile.isLoggedIn,
                    aiGenerated = profile.aiGenerated,
                    sharedSnippetPosted = profile.sharedSnippetPosted,
                    consolePioneered = profile.consolePioneered
                )
            )
        }
    }

    // Profile updates
    suspend fun updateProfile(username: String, email: String, avatarSeed: String) {
        val profile = dao.getUserProfile() ?: UserProfile()
        saveProfile(
            profile.copy(
                username = username,
                email = email,
                avatarSeed = avatarSeed,
                lastActiveTime = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateProfileWithDetails(username: String, email: String, bio: String, avatarSeed: String) {
        val profile = dao.getUserProfile() ?: UserProfile()
        val updatedProfile = profile.copy(
            username = username,
            email = email,
            bio = bio,
            avatarSeed = avatarSeed,
            lastActiveTime = System.currentTimeMillis()
        )
        saveProfile(updatedProfile)

        val existingUser = dao.getRegisteredUser(email)
        if (existingUser != null) {
            dao.insertRegisteredUser(existingUser.copy(
                username = username,
                bio = bio,
                avatarSeed = avatarSeed
            ))
        }
    }

    suspend fun updateStreakStamp() {
        val profile = dao.getUserProfile()
        if (profile != null) {
            val now = System.currentTimeMillis()
            val lastActive = profile.lastActiveTime
            val diffMs = now - lastActive
            val oneDayMs = 24 * 60 * 60 * 1000L

            val updatedStreak = when {
                diffMs > 2 * oneDayMs -> {
                    // broken streak! start over from 1 as per requirements
                    1
                }
                diffMs in (18 * 60 * 60 * 1000L)..(2 * oneDayMs) -> {
                    // consecutive day! increase streak
                    profile.currentStreak + 1
                }
                else -> {
                    // active short window or initialized state, keep current streak
                    if (profile.currentStreak == 0) 1 else profile.currentStreak
                }
            }
            saveProfile(
                profile.copy(
                    currentStreak = updatedStreak,
                    lastActiveTime = now
                )
            )
        }
    }

    suspend fun registerNewUser(username: String, email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim()
        val existing = dao.getRegisteredUser(cleanEmail)
        if (existing != null) {
            return false // Already registered, cannot overwrite
        }

        // Setup brand new registration starting with Level 1, XP 10, Streak 1 and everything locked
        val newUser = RegisteredUser(
            email = cleanEmail,
            username = cleanUsername,
            password = password,
            bio = "Developer working in $cleanUsername workspace. AI compiler integration active.",
            avatarSeed = "seed_user_" + cleanUsername.length,
            xp = 10.0,
            level = 1,
            currentStreak = 1,
            solvedChallengesCount = 0,
            isLoggedIn = true,
            aiGenerated = false,
            sharedSnippetPosted = false,
            consolePioneered = false
        )
        dao.insertRegisteredUser(newUser)

        val profile = UserProfile(
            id = 1,
            username = newUser.username,
            email = newUser.email,
            avatarSeed = newUser.avatarSeed,
            xp = newUser.xp,
            level = newUser.level,
            currentStreak = newUser.currentStreak,
            solvedChallengesCount = newUser.solvedChallengesCount,
            isLoggedIn = true,
            bio = newUser.bio,
            aiGenerated = newUser.aiGenerated,
            sharedSnippetPosted = newUser.sharedSnippetPosted,
            consolePioneered = newUser.consolePioneered
        )
        saveProfile(profile)
        return true
    }

    suspend fun performLogin(emailOrUsername: String, password: String): Boolean {
        val cleanInput = emailOrUsername.trim()
        val cleanEmail = cleanInput.lowercase()
        // Try finding by email first
        var existing = dao.getRegisteredUser(cleanEmail)
        if (existing == null) {
            // Then try finding by username
            existing = dao.getRegisteredUserByUsername(cleanInput)
        }

        if (existing != null) {
            // Verify password match
            if (existing.password != password) {
                return false
            }
            val loadedProfile = UserProfile(
                id = 1,
                username = existing.username,
                email = existing.email,
                avatarSeed = existing.avatarSeed,
                xp = existing.xp,
                level = existing.level,
                currentStreak = if (existing.currentStreak <= 0) 1 else existing.currentStreak,
                solvedChallengesCount = existing.solvedChallengesCount,
                isLoggedIn = true,
                bio = existing.bio,
                aiGenerated = existing.aiGenerated,
                sharedSnippetPosted = existing.sharedSnippetPosted,
                consolePioneered = existing.consolePioneered
            )
            saveProfile(loadedProfile)
            return true
        } else {
            return false
        }
    }

    suspend fun toggleLoginState(isLoggedIn: Boolean) {
        val profile = dao.getUserProfile() ?: UserProfile()
        if (!isLoggedIn) {
            // Save current stats to registry before logging off session
            if (profile.email.isNotBlank()) {
                val reg = RegisteredUser(
                    email = profile.email,
                    username = profile.username,
                    bio = profile.bio,
                    avatarSeed = profile.avatarSeed,
                    xp = profile.xp,
                    level = profile.level,
                    currentStreak = profile.currentStreak,
                    solvedChallengesCount = profile.solvedChallengesCount,
                    isLoggedIn = false,
                    aiGenerated = profile.aiGenerated,
                    sharedSnippetPosted = profile.sharedSnippetPosted,
                    consolePioneered = profile.consolePioneered
                )
                dao.insertRegisteredUser(reg)
            }
        }
        val nextProfile = profile.copy(isLoggedIn = isLoggedIn)
        dao.insertUserProfile(nextProfile)
    }

    // --- AI / SIMULATOR COMPILER CALLS ---

    suspend fun askAssistant(prompt: String, systemInstruction: String? = null): String {
        return gemini.getAiResponse(prompt, systemInstruction)
    }

    /**
     * Executes code via AI simulator compiler or a quick local mock matching default outputs.
     * Sending the actual selected language and raw code to Gemini forces a real sandbox experience.
     */
    suspend fun executeCodeWithCompileSimulator(language: String, code: String): CompilerOutput {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local simple simulation for demo environments who haven't populated APIs yet
            return simulateLocally(language, code)
        }

        val prompt = """
            You are a real-time terminal sandboxed online compiler and runner. 
            Compile and run the following $language code.
            Analyze syntax, semantic structures, and potential compile or runtime errors.
            Return a strictly formatted JSON response and nothing else.
            
            Format:
            {
              "success": true/false,
              "hasErrors": true/false,
              "errors": "Any compiler or runtime errors or stack traces here. Blank if successful.",
              "consoleOutput": "The complete mock console output, simulation standard out, print statements, loops, HTML render descriptions, and variables of this script."
            }

            CODE TO RUN:
            ```
            $code
            ```
        """.trimIndent()

        val systemPrompt = "You are a code executor. Return only a single valid JSON block containing success, hasErrors, errors, and consoleOutput."

        return try {
            val rawJson = gemini.getAiResponse(prompt, systemPrompt, isJson = true)
            val cleanJson = extractJson(rawJson)
            val json = JSONObject(cleanJson)
            CompilerOutput(
                success = json.optBoolean("success", true),
                hasErrors = json.optBoolean("hasErrors", false),
                errors = json.optString("errors", ""),
                output = json.optString("consoleOutput", "No output generated.")
            )
        } catch (e: Exception) {
            CompilerOutput(
                success = false,
                hasErrors = true,
                errors = "Compilation timeout or response error: ${e.message}",
                output = ""
            )
        }
    }

    private fun extractJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun simulateLocally(language: String, code: String): CompilerOutput {
        // Instant response if no API key is supplied
        if (code.contains("print") && code.contains("\"Hello")) {
            return CompilerOutput(true, false, "", "Hello, Mister Codes!\nIteration 1\nIteration 2\nIteration 3\nIteration 4\nIteration 5\n\n[Process completed successfully]")
        }
        if (code.isBlank()) {
            return CompilerOutput(false, true, "Syntax Error: Code block is empty.", "")
        }

        val simulatedOutputs = mapOf(
            "Python" to ">> Running script...\n\nHello, world!\nProcess finished with exit code 0",
            "JavaScript" to ">> Node.js v18.0\n\nSuccessfully executed. Output: 15\nDone.",
            "HTML" to ">> Rendering Live View...\n[HTML layout compiled successfully in 12ms]",
            "CSS" to ">> Applying style sheet rules...\n0 errors, 4 selectors compiled.",
            "Kotlin" to ">> Kotlin Compiler v2.0\nWelcome to Kotlin playground!\nProcess complete.",
            "Java" to ">> compiled Main.java\nRunning... Hello World Class.\nExit 0",
            "Go" to ">> building go app\nRunning... Server starting on :8080",
            "Rust" to ">> cargo build --release\n   Compiling runner v0.1.0\n    Finished release [optimized] target(s)\n     Running `target/release/runner`"
        )

        return CompilerOutput(
            success = true,
            hasErrors = false,
            errors = "",
            output = simulatedOutputs[language] ?: "Source code compiled successfully.\nNo local outputs mapped.\n(Add your GEMINI_API_KEY in the Secrets panel to evaluate this $language code in real time!)"
        )
    }

    // Mock presets generators
    private fun getSampleChallenges(): List<TutorialChallenge> {
        return listOf(
            TutorialChallenge(
                title = "Reverse a string",
                language = "Python",
                difficulty = "Easy",
                description = "Write a function `reverse_string(s)` that takes a string and returns it reversed. Example: 'hello' becomes 'olleh'.",
                questionType = "CODING",
                templateCode = "def reverse_string(s: str) -> str:\n    # Write your solver code here\n    pass\n\n# Driver check\nprint(reverse_string(\"hello\"))\n",
                testCasesJson = "olleh"
            ),
            TutorialChallenge(
                title = "What is Kotlin Null-Safety?",
                language = "Kotlin",
                difficulty = "Easy",
                description = "Which of the following syntaxes declares a nullable String variable in Kotlin that compiles perfectly?",
                questionType = "QUIZ",
                optionsJson = "[\"var name: String = null\", \"var name: String? = null\", \"val name: String = Null()\", \"String name = null;\"]",
                correctAnswer = "1" // index 1
            ),
            TutorialChallenge(
                title = "JS closures",
                language = "JavaScript",
                difficulty = "Medium",
                description = "What does a Javascript closure bundle together?",
                questionType = "QUIZ",
                optionsJson = "[\"A function of class and window object\", \"A function along with its lexical environment references\", \"The arguments stack list and standard out objects\", \"A callback parameters list and error scopes\"]",
                correctAnswer = "1"
            ),
            TutorialChallenge(
                title = "Fibonacci sequence",
                language = "Python",
                difficulty = "Medium",
                description = "Write a function `fib(n)` returning the n-th Fibonacci number. Assume fib(0) = 0 and fib(1) = 1.",
                questionType = "CODING",
                templateCode = "def fib(n: int) -> int:\n    # Calculate n-th fibonacci\n    if n <= 1: return n\n    return fib(n-1) + fib(n-2)\n\nprint(fib(6))\n",
                testCasesJson = "8"
            ),
            TutorialChallenge(
                title = "Go select block",
                language = "Go",
                difficulty = "Hard",
                description = "What does the select statement do in Go?",
                questionType = "QUIZ",
                optionsJson = "[\"Chooses between switch arguments in constant scopes\", \"Blocks on multiple channel operations, selecting the one that is ready\", \"Evaluates file system variables statically during builds\", \"Implements mutex lock releases automatically\"]",
                correctAnswer = "1"
            ),
            TutorialChallenge(
                title = "Python Generator Yields",
                language = "Python",
                difficulty = "Medium",
                description = "What is the primary operational difference between the yield and return keyword statements inside Python functions?",
                questionType = "QUIZ",
                optionsJson = "[\"return exits the execution scope immediately, while yield pauses execution retaining local state variables\", \"yield is slower and only works for list index structures\", \"return produces nested tuples while yield prints tracebacks\", \"yield cannot be referenced inside loops or generator chains\"]",
                correctAnswer = "0"
            ),
            TutorialChallenge(
                title = "Kotlin Coroutines Dispatchers",
                language = "Kotlin",
                difficulty = "Hard",
                description = "In Jetpack Compose or modern Android apps, which Dispatcher reference is recommended for blocking I/O (e.g. SQLite database, heavy local file cache operations, or web requests)?",
                 questionType = "QUIZ",
                 optionsJson = "[\"Dispatchers.Main\", \"Dispatchers.Default\", \"Dispatchers.IO\", \"Dispatchers.Unconfined\"]",
                 correctAnswer = "2"
            ),
            TutorialChallenge(
                title = "Rust Reference Pointer Constraints",
                language = "Rust",
                difficulty = "Hard",
                description = "What is the primary compile-time borrow checker rule governing safe references to a resource in the Rust compiler memory management paradigm?",
                questionType = "QUIZ",
                optionsJson = "[\"Any number of mutable pointers or pointers with static durations are permitted concurrently\", \"Either any number of immutable references or exactly one mutable reference at any given time\", \"Pointers can coexist without lock scopes if marked unsafe\", \"References must be explicitly cleaned up on garbage collector exits\"]",
                correctAnswer = "1"
            )
        )
    }

    private fun getSampleSnippets(): List<SharedSnippet> {
        return listOf(
            SharedSnippet(
                title = "Elegant Glassmorphism",
                language = "CSS",
                code = ".card {\n  background: rgba(255, 255, 255, 0.1);\n  backdrop-filter: blur(10px);\n  border: 1px solid rgba(255, 255, 255, 0.2);\n  border-radius: 12px;\n  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);\n}",
                description = "A beautiful design system standard style for overlays and layout surfaces. Feel free to clone or modify!",
                author = "StyleQueen",
                avatarSeed = "royal_designer",
                upvotes = 34,
                commentsCount = 3
            ),
            SharedSnippet(
                title = "Coroutine debounce flow",
                language = "Kotlin",
                code = "fun <T> Flow<T>.debounceCustom(waitMs: Long): Flow<T> = flow {\n  var lastTime = 0L\n  collect { value ->\n    val now = System.currentTimeMillis()\n    if (now - lastTime >= waitMs) {\n      emit(value)\n      lastTime = now\n    }\n  }\n}",
                description = "Compact hand-rolled throttle/debounce filter that doesn't waste resources. Useful for custom text fields search triggers.",
                author = "FlowMaster",
                avatarSeed = "jet_engineer",
                upvotes = 85,
                commentsCount = 12
            ),
            SharedSnippet(
                title = "Python Binary Search Helper",
                language = "Python",
                code = "def binary_search(arr, target):\n    low, high = 0, len(arr) - 1\n    while low <= high:\n        mid = (low + high) // 2\n        if arr[mid] == target:\n            return mid\n        elif arr[mid] < target:\n            low = mid + 1\n        else:\n            high = mid - 1\n    return -1",
                description = "A standard O(log n) sorting helper, cleanly written with double range constraints.",
                author = "AlgosMaster",
                avatarSeed = "binary_coder",
                upvotes = 42,
                commentsCount = 7
            )
        )
    }
}

data class CompilerOutput(
    val success: Boolean,
    val hasErrors: Boolean,
    val errors: String,
    val output: String
)
