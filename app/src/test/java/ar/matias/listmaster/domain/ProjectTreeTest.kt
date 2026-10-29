package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTreeTest {

    private val base = Instant.parse("2026-10-29T12:00:00Z")

    private fun project(id: String, parentId: String? = null) = Project(
        id = id,
        title = id,
        status = ProjectStatus.ACTIVE,
        parentId = parentId,
        priority = null,
        createdAt = base.plusSeconds(id.hashCode().toLong()),
        updatedAt = base,
    )

    private val chain = listOf(
        project("root"),
        project("level1", "root"),
        project("level2", "level1"),
        project("level3", "level2"),
    )

    @Test
    fun `la raiz es nivel cero`() {
        assertEquals(0, ProjectTree.depthOf("root", chain))
    }

    @Test
    fun `los subproyectos se numeran desde uno`() {
        assertEquals(1, ProjectTree.depthOf("level1", chain))
        assertEquals(2, ProjectTree.depthOf("level2", chain))
        assertEquals(3, ProjectTree.depthOf("level3", chain))
    }

    @Test
    fun `el tercer nivel no puede tener subproyectos`() {
        assertTrue(ProjectTree.canHaveSubproject("root", chain))
        assertTrue(ProjectTree.canHaveSubproject("level2", chain))
        assertFalse(ProjectTree.canHaveSubproject("level3", chain))
    }

    @Test
    fun `un proyecto inexistente no tiene profundidad`() {
        assertEquals(-1, ProjectTree.depthOf("fantasma", chain))
        assertFalse(ProjectTree.canHaveSubproject("fantasma", chain))
    }

    @Test
    fun `un ciclo no tiene profundidad valida`() {
        val cyclic = listOf(
            project("a", "b"),
            project("b", "a"),
        )
        assertEquals(-1, ProjectTree.depthOf("a", cyclic))
    }

    @Test
    fun `detecta un padre que es descendiente`() {
        assertTrue(ProjectTree.wouldCreateCycle("root", "level3", chain))
        assertTrue(ProjectTree.wouldCreateCycle("root", "root", chain))
        assertFalse(ProjectTree.wouldCreateCycle("level3", "root", chain))
        assertFalse(ProjectTree.wouldCreateCycle("root", null, chain))
    }

    @Test
    fun `los ancestros van de la raiz al padre`() {
        assertEquals(
            listOf("root", "level1", "level2"),
            ProjectTree.ancestorsOf("level3", chain).map { it.id },
        )
    }

    @Test
    fun `el subarbol incluye todos los descendientes`() {
        assertEquals(
            setOf("root", "level1", "level2", "level3"),
            ProjectTree.subtreeIds("root", chain),
        )
        assertEquals(setOf("level2", "level3"), ProjectTree.subtreeIds("level2", chain))
    }

    @Test
    fun `los hijos se ordenan por creacion`() {
        val children = ProjectTree.childrenOf("root", chain)
        assertEquals(listOf("level1"), children.map { it.id })
    }
}
