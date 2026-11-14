package ar.matias.listmaster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.matias.listmaster.data.local.ListMasterDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Actualizar la app no puede borrar lo que el usuario ya cargó. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {

    @Test
    fun `migrar de la version 2 a la 5 conserva proyectos, tareas y listas`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)

        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        old.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `parent_id` TEXT, `priority` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, `icon` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`parent_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_parent_id` ON `projects` (`parent_id`)")
        old.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `done` INTEGER NOT NULL, `project_id` TEXT NOT NULL, `due_date` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_project_id` ON `tasks` (`project_id`)")
        old.execSQL("CREATE TABLE IF NOT EXISTS `lists` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `project_id` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_project_id` ON `lists` (`project_id`)")
        old.execSQL("CREATE TABLE IF NOT EXISTS `list_items` (`id` TEXT NOT NULL, `list_id` TEXT NOT NULL, `title` TEXT NOT NULL, `done` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`list_id`) REFERENCES `lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        old.execSQL("CREATE INDEX IF NOT EXISTS `index_list_items_list_id_position` ON `list_items` (`list_id`, `position`)")
        old.execSQL("INSERT INTO projects VALUES ('p1', 'Taekwondista', 'Activo', NULL, 'Alta', '2026-10-29T12:00:00Z', '2026-10-29T12:00:00Z', NULL)")
        old.execSQL("INSERT INTO tasks VALUES ('t1', 'Hacer a', 0, 'p1', NULL, '2026-10-29T12:00:00Z', '2026-10-29T12:00:00Z')")
        old.execSQL("INSERT INTO tasks VALUES ('t2', 'Hacer b', 1, 'p1', '2026-11-05', '2026-10-29T12:00:01Z', '2026-10-29T12:00:01Z')")
        old.execSQL("INSERT INTO lists VALUES ('l1', 'MCU', NULL, '2026-10-29T12:00:00Z', '2026-10-29T12:00:00Z')")
        old.execSQL("INSERT INTO list_items VALUES ('i1', 'l1', 'Iron Man', 1, 0)")
        old.version = 2
        old.close()

        val db = Room.databaseBuilder(context, ListMasterDatabase::class.java, name)
            .addMigrations(
                ListMasterDatabase.MIGRATION_1_2,
                ListMasterDatabase.MIGRATION_2_3,
                ListMasterDatabase.MIGRATION_3_4,
                ListMasterDatabase.MIGRATION_4_5,
            )
            .allowMainThreadQueries()
            .build()
        try {
            val data = ListMasterRepository(db).snapshot()
            assertEquals(listOf("Taekwondista"), data.projects.map { it.title })
            assertEquals(listOf("Hacer a", "Hacer b"), data.tasks.map { it.title })
            assertEquals(listOf(false, true), data.tasks.map { it.done })
            assertTrue(data.tasks.all { it.description.isEmpty() && it.priority == null })
            assertEquals("MCU", data.lists.single().title)
            assertEquals("Iron Man", data.lists.single().items.single().title)
            assertEquals(null, data.lists.single().parentId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
