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
    version = 5,
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

        /**
         * Las listas ahora pueden anidarse (sublistas). SQLite no permite agregar
         * una clave foránea con ALTER TABLE, así que se recrea la tabla.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE lists_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        project_id TEXT,
                        parent_id TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY(project_id) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(parent_id) REFERENCES lists(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO lists_new (id, title, project_id, parent_id, created_at, updated_at)
                    SELECT id, title, project_id, NULL, created_at, updated_at FROM lists
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE lists")
                db.execSQL("ALTER TABLE lists_new RENAME TO lists")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lists_project_id ON lists(project_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lists_parent_id ON lists(parent_id)")
            }
        }

        fun create(context: Context): ListMasterDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ListMasterDatabase::class.java,
                NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
