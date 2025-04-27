
package impl.ui.collector.components.editors
/*
import CollectorItem
import net.minecraft.inventory.ContainerLock
import impl.ui.collector.components.EditorRegistry
import win.ninegang.ninetools.impl.ui.collector.utils.CollectorTextBox
import impl.ui.collector.utils.CollectorToggleButton



class ContainerLockEditor(
    registry: EditorRegistry,
    x: Double,
    y: Double,
    initialValue: ContainerLock?
) : BaseComponentEditor(registry) {
    override val componentType: String = "minecraft:lock"

    private var lockKey: String = initialValue?.key() ?: ""
    private var isEnabled: Boolean = initialValue != null && initialValue != ContainerLock.EMPTY

    private lateinit var textBox: CollectorTextBox
    private lateinit var enableToggle: CollectorToggleButton

    init {
        initializeDimensions(x, y)

        textBox = CollectorTextBox(
            x = x + 5.0,
            y = y + 18.0,
            width = width - 10.0,
            label = "Lock Key",
            placeholder = "Enter key to lock container...",
            initialText = lockKey
        ) { newText ->
            lockKey = newText

            if (isEnabled) {
                if (lockKey.isEmpty()) {
                    isEnabled = false
                    enableToggle.state = false
                    notifyValueChanged(null)
                } else {
                    notifyValueChanged(ContainerLock(lockKey))
                }
            }
        }
        editorComponents.add(textBox)

        enableToggle = CollectorToggleButton(
            x = x + 5.0,
            y = y + 40.0,
            label = "Enable",
            description = "Enable/disable container lock",
            state = isEnabled
        ) { enabled ->
            isEnabled = enabled

            if (enabled) {
                if (lockKey.isEmpty()) {
                    lockKey = "LockKey"
                    textBox.setText(lockKey)
                }
                notifyValueChanged(ContainerLock(lockKey))
            } else {
                notifyValueChanged(null)
            }
        }
        editorComponents.add(enableToggle)
    }

    override fun initializeItem(collectorItem: CollectorItem) {
        super.initializeItem(collectorItem)

        textBox.setText(lockKey)
        enableToggle.state = isEnabled

        if (isEnabled && lockKey.isNotEmpty()) {
            notifyValueChanged(ContainerLock(lockKey))
        } else {
            notifyValueChanged(null)
        }
    }
}*/
