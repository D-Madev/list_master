package ar.matias.listmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET done = :done, updated_at = :updatedAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, updatedAt: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<ListEntity>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getById(id: String): ListEntity?

    @Upsert
    suspend fun upsert(list: ListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lists: List<ListEntity>)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM lists")
    suspend fun deleteAll()
}

@Dao
interface ListItemDao {
    @Query("SELECT * FROM list_items ORDER BY list_id ASC, position ASC, id ASC")
    fun observeAll(): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items ORDER BY list_id ASC, position ASC, id ASC")
    suspend fun getAll(): List<ListItemEntity>

    @Query("SELECT * FROM list_items WHERE id = :id")
    suspend fun getById(id: String): ListItemEntity?

    @Upsert
    suspend fun upsert(item: ListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ListItemEntity>)

    @Query("SELECT COALESCE(MAX(position), -1) FROM list_items WHERE list_id = :listId")
    suspend fun maxPosition(listId: String): Int

    @Query("UPDATE list_items SET done = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("UPDATE list_items SET title = :title WHERE id = :id")
    suspend fun setTitle(id: String, title: String)

    @Query("DELETE FROM list_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM list_items")
    suspend fun deleteAll()
}
