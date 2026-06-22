package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. ENTITIES ---

@Entity(tableName = "projects")
data class CodeProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val code: String,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "challenges")
data class TutorialChallenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val description: String,
    val questionType: String, // "QUIZ" or "CODING"
    val optionsJson: String = "", // JSON list of choices for quiz (e.g. ["A", "B", ...])
    val correctAnswer: String = "", // Correct option index or keyword
    val templateCode: String = "",
    val testCasesJson: String = "", // Simulated expected output for inputs
    val userSolution: String = "",
    val isCompleted: Boolean = false
)

@Entity(tableName = "shared_snippets")
data class SharedSnippet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val code: String,
    val description: String,
    val author: String,
    val avatarSeed: String, // To generate unique styling / colors
    val upvotes: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val isLiked: Boolean = false,
    val commentsJson: String = ""
)

@Entity(tableName = "registered_users")
data class RegisteredUser(
    @PrimaryKey val email: String,
    val username: String,
    val password: String = "",
    val bio: String = "",
    val avatarSeed: String = "",
    val xp: Double = 10.0,
    val level: Int = 1,
    val currentStreak: Int = 1,
    val solvedChallengesCount: Int = 0,
    val isLoggedIn: Boolean = false,
    val aiGenerated: Boolean = false,
    val sharedSnippetPosted: Boolean = false,
    val consolePioneered: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "MisterCoder",
    val email: String = "mistercodes@codes.com",
    val avatarSeed: String = "MisterCoder",
    val xp: Double = 10.0,
    val level: Int = 1,
    val currentStreak: Int = 1,
    val solvedChallengesCount: Int = 0,
    val isLoggedIn: Boolean = false,
    val bio: String = "Passionate developer. Building scalable fullstack algorithms & compiler systems.",
    val lastActiveTime: Long = System.currentTimeMillis(),
    val githubConnected: Boolean = true,
    val firebaseSynced: Boolean = true,
    val aiGenerated: Boolean = false,
    val sharedSnippetPosted: Boolean = false,
    val consolePioneered: Boolean = false,
    val accountCreatedOn: Long = System.currentTimeMillis() - 86400 * 1000L * 2
)

// --- 2. DATA ACCESS OBJECT (DAO) ---

@Dao
interface MisterCodesDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjectsFlow(): Flow<List<CodeProject>>

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    suspend fun getAllProjects(): List<CodeProject>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): CodeProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: CodeProject): Long

    @Update
    suspend fun updateProject(project: CodeProject)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Challenges
    @Query("SELECT * FROM challenges ORDER BY id ASC")
    fun getAllChallengesFlow(): Flow<List<TutorialChallenge>>

    @Query("SELECT * FROM challenges WHERE language = :language ORDER BY id ASC")
    fun getChallengesByLanguage(language: String): Flow<List<TutorialChallenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<TutorialChallenge>)

    @Update
    suspend fun updateChallenge(challenge: TutorialChallenge)

    // Snippets
    @Query("SELECT * FROM shared_snippets ORDER BY timestamp DESC")
    fun getAllSharedSnippetsFlow(): Flow<List<SharedSnippet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedSnippets(snippets: List<SharedSnippet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedSnippet(snippet: SharedSnippet): Long

    @Update
    suspend fun updateSharedSnippet(snippet: SharedSnippet)

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // Registered Users
    @Query("SELECT * FROM registered_users WHERE email = :email LIMIT 1")
    suspend fun getRegisteredUser(email: String): RegisteredUser?

    @Query("SELECT * FROM registered_users WHERE username = :username LIMIT 1")
    suspend fun getRegisteredUserByUsername(username: String): RegisteredUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisteredUser(user: RegisteredUser)
}

// --- 3. DATABASE CONVERTERS & HOLDER ---

@Database(
    entities = [
        CodeProject::class,
        TutorialChallenge::class,
        SharedSnippet::class,
        UserProfile::class,
        RegisteredUser::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MisterCodesDatabase : RoomDatabase() {
    abstract fun dao(): MisterCodesDao
}
