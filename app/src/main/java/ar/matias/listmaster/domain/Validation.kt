package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.MAX_ICON_LENGTH
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

    /** Normaliza el ícono: vacío es "sin ícono" y se recorta a un emoji corto. */
    fun icon(raw: String?): String? {
        val clean = raw?.trim().orEmpty()
        if (clean.isEmpty()) return null
        require(clean.length <= MAX_ICON_LENGTH) {
            "El ícono no puede superar los $MAX_ICON_LENGTH caracteres."
        }
        return clean
    }
}
