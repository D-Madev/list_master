package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.MAX_ITEM_TITLE_LENGTH
import ar.matias.listmaster.domain.model.MAX_TITLE_LENGTH

object Validation {

    fun title(raw: String, maxLength: Int = MAX_TITLE_LENGTH): String {
        val clean = raw.trim()
        require(clean.isNotEmpty()) { "El título no puede estar vacío." }
        require(clean.length <= maxLength) {
            "El título no puede superar los $maxLength caracteres."
        }
        return clean
    }

    fun itemTitle(raw: String): String = title(raw, MAX_ITEM_TITLE_LENGTH)
}
