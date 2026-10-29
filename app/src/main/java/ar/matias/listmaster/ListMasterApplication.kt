package ar.matias.listmaster

import android.app.Application
import ar.matias.listmaster.data.AppPreferences
import ar.matias.listmaster.data.ListMasterRepository
import ar.matias.listmaster.data.local.ListMasterDatabase

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
}
