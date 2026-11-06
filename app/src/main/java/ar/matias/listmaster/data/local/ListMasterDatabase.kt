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
    version = 4,
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

        /** Las tareas ahora pueden tener una descripción. Solo agrega una columna: no toca los datos. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN description TEXT")
            }
        }

        /** Las tareas ahora pueden tener prioridad. Solo agrega una columna: no toca los datos. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT")
            }
        }

        fun create(context: Context): ListMasterDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ListMasterDatabase::class.java,
                NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
