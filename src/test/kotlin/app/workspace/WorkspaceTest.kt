@file:Suppress("RedundantUnitReturnType")

package app.workspace

import app.workspace.Log.i
import app.workspace.Workspace.InstallationType.ALL_IN_ONE
import app.workspace.Workspace.WorkspaceEntry
import app.workspace.Workspace.WorkspaceEntry.CollaborationEntry.Collaboration
import app.workspace.Workspace.WorkspaceEntry.CommunicationEntry.Communication
import app.workspace.Workspace.WorkspaceEntry.ConfigurationEntry.Configuration
import app.workspace.Workspace.WorkspaceEntry.CoreEntry.Education
import app.workspace.Workspace.WorkspaceEntry.CoreEntry.Education.EducationEntry.*
import app.workspace.Workspace.WorkspaceEntry.DashboardEntry.Dashboard
import app.workspace.Workspace.WorkspaceEntry.JobEntry.Job
import app.workspace.Workspace.WorkspaceEntry.JobEntry.Job.HumanResourcesEntry.Position
import app.workspace.Workspace.WorkspaceEntry.JobEntry.Job.HumanResourcesEntry.Resume
import app.workspace.Workspace.WorkspaceEntry.OfficeEntry.Office
import app.workspace.Workspace.WorkspaceEntry.OfficeEntry.Office.LibraryEntry.*
import app.workspace.Workspace.WorkspaceEntry.OrganisationEntry.Organisation
import app.workspace.Workspace.WorkspaceEntry.PortfolioEntry.Portfolio
import app.workspace.Workspace.WorkspaceEntry.PortfolioEntry.Portfolio.PortfolioProject
import app.workspace.Workspace.WorkspaceEntry.PortfolioEntry.Portfolio.PortfolioProject.ProjectBuild
import app.workspace.WorkspaceManager.WorkspaceConstants.entries
import app.workspace.WorkspaceManager.workspace
import org.apache.commons.lang3.SystemUtils.USER_HOME_KEY
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.test.*

/**
 *     1/ Workspace
 *         a. create a workspace
 *         b. add an entry to the workspace
 *         c. remove an entry from the workspace
 *         d. update an entry in the workspace
 *         e. find an entry in the workspace
 *     2/ WorkspaceEntry
 *         a. create an Education
 *         b. create an Office
 *         c. create a Job
 *         d. create a Configuration
 *         e. create a Communication
 *         f. create an Organisation
 *         g. create a Collaboration
 *         h. create a Dashboard
 *         i. create a Portfolio
 *     3/ name
 *     4/ office
 *     5/ cores
 *     6/ job
 *     7/ configuration
 *     8/ communication
 *     9/ organisation
 *     10/ collaboration
 *     11/ dashboard
 *     12/ portfolio
 */

class WorkspaceTest {
    @BeforeTest
    fun setUp() = Unit

    @AfterTest
    fun tearDown() = Unit

    private val workspace = Workspace(
        workspace = WorkspaceEntry(
            name = "fonderie",
            path = "${System.getProperty(USER_HOME_KEY)}/workspace/school",
            office = Office(
                books = Books(name = "books-collection"),
                datas = Datas(name = "datas"),
                formations = TrainingCatalogue(catalogue = "formations"),
                bizness = Profession("bizness"),
                notebooks = Notebooks(notebooks = "notebooks"),
                pilotage = Pilotage(name = "pilotage"),
                schemas = Schemas(name = "schemas"),
                slides = Slides(
                    path = "${
                        System.getProperty("user.home")
                    }/workspace/office/slides"
                ),
                sites = Sites(name = "sites"),
                path = "office"
            ),
            cores = mapOf(
                "education" to Education(
                    school = School(name = "talaria"),
                    student = Student(name = "olivier"),
                    teacher = Teacher(name = "cheroliv"),
                    educationTools = EducationTools(name = "edTools")
                ),
            ),
            job = Job(
                position = Position("Teacher"),
                resume = Resume(name = "CV")
            ),
            configuration = Configuration(configuration = "school-configuration"),
            communication = Communication(site = "static-website"),
            organisation = Organisation(organisation = "organisation"),
            collaboration = Collaboration(collaboration = "collaboration"),
            dashboard = Dashboard(dashboard = "dashboard"),
            portfolio = Portfolio(
                mutableMapOf(
                    "school" to PortfolioProject(
                        name = "name",
                        cred = "credential",
                        builds = mutableMapOf(
                            "training" to ProjectBuild(
                                name = "training"
                            )
                        )
                    )
                )
            ),
        )
    )

    @Test
    fun checkDisplayWorkspaceStructure(): Unit {
        workspace.toString().run(::println)
        workspace.displayWorkspaceStructure()
    }

    @Test
    fun `install workspace`(): Unit {
        Workspace.install(path = "${System.getProperty("user.home")}/workspace/school")
        // default type : AllInOneWorkspace
        // ExplodedWorkspace
    }

    @Test
    fun `test create workspace with ALL_IN_ONE config`(): Unit {
        val path = "build/workspace"
        val configFileName = "config.yaml"
        path.run(::File).apply {
            when {
                !exists() -> mkdirs().run(::assertTrue)
            }
        }.run {
            exists().run(::assertTrue)
            isDirectory.run(::assertTrue)
            Workspace.WorkspaceConfig(
                basePath = toPath(),
                type = ALL_IN_ONE,
            ).run(WorkspaceManager::createWorkspace)
            entries.forEach { "$this/$it".run(::File).exists().run(::assertTrue) }
            "$path/$configFileName".run(::File).exists().run(::assertTrue)
            deleteRecursively().run(::assertTrue)
        }
    }

    @Test
    fun `test create workspace with SEPARATED_FOLDERS config`(): Unit {
        val workspacePath = "build/workspace"
        val configFileName = "config.yaml"
        val subPaths = mutableMapOf<String, Path>()

        entries.mapIndexed { index, workspaceEntryPath ->
            (workspaceEntryPath to (workspacePath.run(::File)
                .parentFile
                .listFiles()?.get(index) ?: "build".run(::File)))
        }.forEach { it: Pair<String, File> ->
            "${it.second}/${it.first}"
                .run(::File)
                .apply {
                    subPaths[it.first] = toPath()
                    mkdir()
                }.isDirectory().run(::assertTrue)
        }

        workspacePath.run(::File)
            .apply { if (!exists()) mkdir().run(::assertTrue) }
            .run {
                exists().run(::assertTrue)
                isDirectory.run(::assertTrue)

                val config = Workspace.WorkspaceConfig(
                    basePath = toPath(),
                    type = Workspace.InstallationType.SEPARATED_FOLDERS,
                    subPaths = subPaths,
                    configFileName = configFileName
                ).run(WorkspaceManager::createWorkspace)

                "$this/$configFileName"
                    .run(::File)
                    .readText()
                    .apply { assertTrue(isNotBlank()) }
                    .run { "config :\n$this" }
                    .run(::i)
                assertEquals(
                    expected = config.subPaths["office"]!!.pathString,
                    actual = config.workspace.workspace.office.path
                )
                assertEquals(
                    expected = config.subPaths["education"]!!.pathString,
                    actual = (config.workspace.workspace.cores["education"] as Education).path
                )
                assertEquals(
                    expected = config.subPaths["communication"]!!.pathString,
                    actual = (config.workspace.workspace.communication as Communication).path
                )
                assertEquals(
                    expected = config.subPaths["configuration"]!!.pathString,
                    actual = (config.workspace.workspace.configuration as Configuration).path
                )
                assertEquals(
                    expected = config.subPaths["job"]!!.pathString,
                    actual = (config.workspace.workspace.job as Job).path
                )

                "$this/$configFileName".run(::File).exists().run(::assertTrue)
                deleteRecursively().run(::assertTrue)
            }
        subPaths.map { it.value.toAbsolutePath().toFile().deleteRecursively().run(::assertTrue) }
    }


}