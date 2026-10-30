package ar.matias.listmaster.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        TaskEntity::class,
        ListEntity::class,
        ListItemEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ListMasterDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun listItemDao(): ListItemDao

    companion object {
        const val NAME = "list_master.db"

        /** Los proyectos ahora pueden tener un ícono o un emoji propio. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN icon TEXT")
            }
        }

        fun create(context: Context): ListMasterDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ListMasterDatabase::class.java,
                NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
