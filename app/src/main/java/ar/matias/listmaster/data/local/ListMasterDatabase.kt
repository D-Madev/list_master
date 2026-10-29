package ar.matias.listmaster.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        TaskEntity::class,
        ListEntity::class,
        ListItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ListMasterDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun listItemDao(): ListItemDao

    companion object {
        const val NAME = "list_master.db"

        fun create(context: Context): ListMasterDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ListMasterDatabase::class.java,
                NAME,
            ).build()
    }
}
