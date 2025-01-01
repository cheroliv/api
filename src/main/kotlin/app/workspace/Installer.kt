package app.workspace

import app.Constants.EMPTY_STRING
import app.Loggers
import app.Loggers.w
import java.awt.EventQueue.invokeLater
import java.nio.file.Path
import javax.swing.*
import javax.swing.UIManager.getInstalledLookAndFeels

object Installer {
    @JvmStatic
    fun main(args: Array<String>) = try {
        getInstalledLookAndFeels()
            .find { it.name == "Nimbus" }
            ?.className
            ?.run(UIManager::setLookAndFeel)
    } catch (ex: Exception) {
        when (ex) {
            is ClassNotFoundException,
            is InstantiationException,
            is IllegalAccessException,
            is UnsupportedLookAndFeelException -> w(EMPTY_STRING, ex)
            // Rethrow unknown exceptions
            else -> throw ex
        }
    }.run { invokeLater { run(Installer::GUI).run { isVisible = true } } }

    class GUI(
        private val selectedPaths: MutableMap<String, Path?> = HashMap(),
        private var currentInstallationType: Workspace.InstallationType = Workspace.InstallationType.ALL_IN_ONE,
        private val communicationPathLabel: JLabel = JLabel("Communication").apply { toolTipText = "" },
        private val communicationPathTextField: JTextField = JTextField(),
        private val configurationPathLabel: JLabel = JLabel("Configuration").apply { toolTipText = "" },
        private val configurationPathTextField: JTextField = JTextField(),
        private val educationPathLabel: JLabel = JLabel("Education").apply { toolTipText = "" },
        private val educationPathTextField: JTextField = JTextField(),
        private val jobPathLabel: JLabel = JLabel("Job").apply { toolTipText = "" },
        private val jobPathTextField: JTextField = JTextField(),
        private val officePathLabel: JLabel = JLabel("Office").apply { toolTipText = "" },
        private val officePathTextField: JTextField = JTextField(),
        private val titleLabel: JLabel = JLabel("School installer"),
        private val workspacePathLabel: JLabel = JLabel("Path"),
        private val workspacePathTextField: JTextField = JTextField(),
        private val workspaceTypePanel: JPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Installation type")
        },
        private val workspaceTypeSelectorPanel: JPanel = JPanel(),
        private val workspaceTopPanel: JPanel = JPanel(),
        private val workspacePathPanel: JPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Workspace")
        },
        private val workspaceEntriesPanel: JPanel = JPanel(),
        private val splitWorkspaceRadioButton: JRadioButton = JRadioButton("Separated folders")
            .apply { isSelected = false },
        private val allInOneWorkspaceRadioButton: JRadioButton = JRadioButton("All-in-one").apply { isSelected = true },
        private val browseCommunicationPathButton: JButton = JButton(),
        private val browseConfigurationPathButton: JButton = JButton(),
        private val browseEducationPathButton: JButton = JButton(),
        private val browseOfficePathButton: JButton = JButton(),
        private val browseWorkspacePathButton: JButton = JButton(),
        private val browseJobPathButton: JButton = JButton(),
        private val createWorkspaceButton: JButton = JButton("Create"),
        private val installationTypeGroup: ButtonGroup = ButtonGroup().apply {
            add(allInOneWorkspaceRadioButton)
            add(splitWorkspaceRadioButton)
        },
    ) : JFrame("School Project Installer") {
        init {
            initUI().let { "Init, currentInstallationType : $currentInstallationType".run(Loggers::i) }
        }

        private fun GUI.clearSpecificPaths() {
            officePathTextField.text = ""
            educationPathTextField.text = ""
            communicationPathTextField.text = ""
            configurationPathTextField.text = ""
            jobPathTextField.text = ""

            selectedPaths.remove("office")
            selectedPaths.remove("education")
            selectedPaths.remove("communication")
            selectedPaths.remove("configuration")
            selectedPaths.remove("job")
        }

        private fun GUI.handleCreateWorkspace() {
            when {
                workspacePathTextField.text.isEmpty() -> {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please select a app.workspace directory",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                    return
                }

                else -> try {
                    "Creating app.workspace... : $currentInstallationType".run(Loggers::i)
                    if (currentInstallationType == Workspace.InstallationType.SEPARATED_FOLDERS) arrayOf(
                        "office",
                        "education",
                        "communication",
                        "configuration",
                        "job"
                    ).forEach {
                        check(selectedPaths.containsKey(it) && selectedPaths[it] != null) {
                            "All paths must be selected for separated folders installation"
                        }
                    }
                    Workspace.WorkspaceConfig(
                        basePath = selectedPaths["workspace"]!!,
                        type = currentInstallationType,
                        subPaths = selectedPaths.map { (key, value) -> key to value!! }.toMap()
                    ).run(WorkspaceManager::createWorkspace)
                    JOptionPane.showMessageDialog(
                        this,
                        "Workspace created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error creating app.workspace: " + e.message,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }

        private fun GUI.selectDirectory(
            pathKey: String,
            textField: JTextField
        ) = JFileChooser().run {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Directory"
            when (JFileChooser.APPROVE_OPTION) {
                showOpenDialog(this) -> selectedFile.toPath().run {
                    selectedPaths[pathKey] = this
                    textField.text = toString()
                }
            }
        }

        private fun GUI.handleInstallationTypeChange(type: Workspace.InstallationType) {
            "currentInstallationType : $currentInstallationType".run(Loggers::i)
            currentInstallationType = type
            "Installation type changed to $type".run(Loggers::i)
            setWorkspaceEntriesVisibility(type == Workspace.InstallationType.SEPARATED_FOLDERS)
            if (type == Workspace.InstallationType.ALL_IN_ONE) clearSpecificPaths()
        }


        private fun GUI.addListeners(): GUI {
            splitWorkspaceRadioButton.addActionListener { handleInstallationTypeChange(Workspace.InstallationType.SEPARATED_FOLDERS) }
            allInOneWorkspaceRadioButton.addActionListener { handleInstallationTypeChange(Workspace.InstallationType.ALL_IN_ONE) }
            browseCommunicationPathButton.addActionListener {
                selectDirectory("communication", communicationPathTextField)
            }
            browseConfigurationPathButton.addActionListener {
                selectDirectory("configuration", configurationPathTextField)
            }
            browseEducationPathButton.addActionListener { selectDirectory("education", educationPathTextField) }
            browseOfficePathButton.addActionListener { selectDirectory("office", officePathTextField) }
            browseWorkspacePathButton.addActionListener { selectDirectory("workspace", workspacePathTextField) }
            browseJobPathButton.addActionListener { selectDirectory("job", jobPathTextField) }
            createWorkspaceButton.addActionListener { handleCreateWorkspace() }
            installationTypeGroup.selection.addActionListener {
            }
            return this
        }

        private fun GUI.setWorkspaceEntriesVisibility(
            visible: Boolean
        ): GUI = setOf(
            officePathLabel,
            officePathTextField,
            browseOfficePathButton,
            educationPathLabel,
            educationPathTextField,
            browseEducationPathButton,
            communicationPathLabel,
            communicationPathTextField,
            browseCommunicationPathButton,
            configurationPathLabel,
            configurationPathTextField,
            browseConfigurationPathButton,
            jobPathLabel,
            jobPathTextField,
            browseJobPathButton
        ).map { it.isVisible = visible }
            .run { this@setWorkspaceEntriesVisibility }

        internal fun GUI.initUI() {
            name = "setupFrame" // NOI18N
            defaultCloseOperation = EXIT_ON_CLOSE
            setWorkspaceEntriesVisibility(false)
            mutableSetOf(
                browseEducationPathButton,
                browseOfficePathButton,
                browseCommunicationPathButton,
                browseWorkspacePathButton,
                browseConfigurationPathButton,
                browseJobPathButton,
            ).onEach { "Select directory".run(it::setText) }
            workspaceTypePanel.apply panel@{
                run(::GroupLayout).run {
                    this@panel.layout = this
                    setHorizontalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGap(0, 924, Short.MAX_VALUE.toInt())
                            .addGroup(
                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(
                                        GroupLayout.Alignment.TRAILING, createSequentialGroup()
                                            .addContainerGap()
                                            .addComponent(
                                                workspaceTypeSelectorPanel,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE.toInt()
                                            )
                                            .addContainerGap()
                                    )
                            )
                            .addGroup(
                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(
                                        createSequentialGroup()
                                            .addContainerGap()
                                            .addComponent(
                                                workspaceEntriesPanel,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE.toInt()
                                            )
                                            .addContainerGap()
                                    )
                            )
                    )
                    setVerticalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGap(0, 344, Short.MAX_VALUE.toInt())
                            .addGroup(
                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(
                                        createSequentialGroup()
                                            .addContainerGap()
                                            .addComponent(
                                                workspaceTypeSelectorPanel,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE
                                            )
                                            .addContainerGap(263, Short.MAX_VALUE.toInt())
                                    )
                            )
                            .addGroup(
                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(
                                        GroupLayout.Alignment.TRAILING, createSequentialGroup()
                                            .addContainerGap(69, Short.MAX_VALUE.toInt())
                                            .addComponent(
                                                workspaceEntriesPanel,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE
                                            )
                                            .addContainerGap()
                                    )
                            )
                    )
                }
            }
            contentPane.apply panel@{
                run(::GroupLayout).run {
                    this@panel.layout = this
                    setHorizontalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addComponent(
                                        workspaceTopPanel,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()
                                    )
                                    .addContainerGap()
                            )
                            .addComponent(
                                workspacePathPanel,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()
                            )
                            .addComponent(
                                workspaceTypePanel,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()
                            )
                    )
                    setVerticalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addComponent(
                                        workspaceTopPanel,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE
                                    )
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(
                                        workspacePathPanel,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE
                                    )
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(
                                        workspaceTypePanel,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE
                                    )
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                            )
                    )
                }
            }
            workspaceTypeSelectorPanel.apply panel@{
                run(::GroupLayout).run {
                    this@panel.layout = this
                    setHorizontalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(allInOneWorkspaceRadioButton)
                                    .addGap(18, 18, 18)
                                    .addComponent(splitWorkspaceRadioButton)
                                    .addContainerGap(508, Short.MAX_VALUE.toInt())
                            )
                    )
                    setVerticalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(
                                        createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(splitWorkspaceRadioButton)
                                            .addComponent(allInOneWorkspaceRadioButton)
                                    )
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                            )
                    )
                }
            }
            workspaceTopPanel.apply panel@{
                run(::GroupLayout).run {
                    this@panel.layout = this
                    setHorizontalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(titleLabel)
                                    .addPreferredGap(
                                        LayoutStyle.ComponentPlacement.RELATED,
                                        GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE.toInt()
                                    ).addComponent(createWorkspaceButton)
                            )
                    )
                    setVerticalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(
                                        createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(
                                                titleLabel,
                                                GroupLayout.PREFERRED_SIZE,
                                                43,
                                                GroupLayout.PREFERRED_SIZE
                                            )
                                            .addComponent(createWorkspaceButton)
                                    )
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                            )
                    )
                }
            }
            workspacePathPanel.apply panel@{
                border = BorderFactory.createTitledBorder("Workspace")
                run(::GroupLayout).run {
                    this@panel.layout = this
                    setHorizontalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(
                                        workspacePathLabel,
                                        GroupLayout.PREFERRED_SIZE, 52,
                                        GroupLayout.PREFERRED_SIZE
                                    )
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(workspacePathTextField)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(browseWorkspacePathButton)
                                    .addContainerGap()
                            )
                    )
                    setVerticalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(
                                createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(
                                        createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(
                                                workspacePathTextField,
                                                GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE
                                            )
                                            .addComponent(browseWorkspacePathButton)
                                            .addComponent(workspacePathLabel)
                                    )
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                            )
                    )
                }
            }
            workspaceEntriesPanel.apply panel@{
                run(::GroupLayout).run {
                    this@panel.layout = this
                    setHorizontalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGap(0, 912, Short.MAX_VALUE.toInt())
                            .addGroup(
                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(
                                        createSequentialGroup()
                                            .addContainerGap()
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                    .addComponent(
                                                        officePathLabel,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        educationPathLabel,
                                                        GroupLayout.Alignment.TRAILING,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        communicationPathLabel,
                                                        GroupLayout.Alignment.TRAILING,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        configurationPathLabel,
                                                        GroupLayout.Alignment.TRAILING,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        jobPathLabel,
                                                        GroupLayout.Alignment.TRAILING,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        190,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                            )
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addComponent(
                                                        officePathTextField,
                                                        GroupLayout.Alignment.TRAILING,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        475,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(educationPathTextField)
                                                    .addComponent(communicationPathTextField)
                                                    .addComponent(configurationPathTextField)
                                                    .addComponent(jobPathTextField)
                                            )
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addComponent(browseEducationPathButton)
                                                    .addComponent(
                                                        browseOfficePathButton,
                                                        GroupLayout.Alignment.TRAILING
                                                    )
                                                    .addComponent(browseCommunicationPathButton)
                                                    .addComponent(browseConfigurationPathButton)
                                                    .addComponent(browseJobPathButton)
                                            )
                                            .addContainerGap()
                                    )
                            )
                    )
                    setVerticalGroup(
                        createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGap(0, 269, Short.MAX_VALUE.toInt())
                            .addGroup(
                                createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(
                                        createSequentialGroup()
                                            .addGap(3, 3, 3)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                    .addComponent(
                                                        officePathLabel,
                                                        GroupLayout.PREFERRED_SIZE, 42,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                                    .addComponent(
                                                        browseOfficePathButton,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        officePathTextField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                            )
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                    .addComponent(
                                                        browseEducationPathButton,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        educationPathTextField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                                    .addComponent(
                                                        educationPathLabel,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        42,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                            )
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                    .addComponent(
                                                        browseCommunicationPathButton,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        communicationPathTextField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                                    .addComponent(
                                                        communicationPathLabel,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        42,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                            )
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                    .addComponent(
                                                        browseConfigurationPathButton,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        configurationPathTextField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                                    .addComponent(
                                                        configurationPathLabel,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        42,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                            )
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addGroup(
                                                createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                    .addComponent(
                                                        browseJobPathButton,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        Short.MAX_VALUE.toInt()
                                                    )
                                                    .addComponent(
                                                        jobPathTextField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                                    .addComponent(
                                                        jobPathLabel,
                                                        GroupLayout.PREFERRED_SIZE, 42,
                                                        GroupLayout.PREFERRED_SIZE
                                                    )
                                            )
                                            .addGap(3, 3, 3)
                                    )
                            )
                    )
                }
            }
            addListeners().pack()
        }
    }
}