package ar.matias.listmaster

import android.app.Application
import ar.matias.listmaster.data.AppPreferences
import ar.matias.listmaster.data.ListMasterRepository
import ar.matias.listmaster.data.local.ListMasterDatabase
import ar.matias.listmaster.data.remote.WebSession

class ListMasterApplication : Application() {

    val database: ListMasterDatabase by lazy {
        ListMasterDatabase.create(this)
    }

    val repository: ListMasterRepository by lazy {
        ListMasterRepository(database)
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(this)
    }

    /** La sesión web vive en la app, no en una pantalla: sigue abierta aunque
     *  se navegue. */
    val webSession: WebSession by lazy {
        WebSession(this, repository)
    }
}
