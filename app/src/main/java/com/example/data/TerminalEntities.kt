package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "terminal_commands")
data class TerminalCommand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val directory: String = ""
)

@Entity(tableName = "codex_messages")
data class CodexMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isCommandResult: Boolean = false,
    val commandExecuted: String? = null
)

@Dao
interface TerminalDao {
    @Query("SELECT * FROM terminal_commands ORDER BY timestamp DESC LIMIT 100")
    fun getRecentCommands(): Flow<List<TerminalCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: TerminalCommand)

    @Query("DELETE FROM terminal_commands")
    suspend fun clearHistory()
}

@Dao
interface CodexDao {
    @Query("SELECT * FROM codex_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<CodexMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CodexMessage)

    @Query("DELETE FROM codex_messages")
    suspend fun clearChat()
}

@Database(entities = [TerminalCommand::class, CodexMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun terminalDao(): TerminalDao
    abstract fun codexDao(): CodexDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "codex_ubuntu_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
