package impl.ui.collector

import impl.ui.collector.data.CollectorItem

abstract class CollectionView : UIComponent {
    abstract var items: List<CollectorItem>


    var onItemClick: ((CollectorItem) -> Unit)? = null
    var onItemHover: ((CollectorItem?, Double, Double) -> Unit)? = null


    protected var hoveredItem: CollectorItem? = null
    protected var mouseHoverX = 0.0
    protected var mouseHoverY = 0.0


    protected var scrollOffset = 0.0
    protected var maxScroll = 0.0
    protected val scrollSpeed = 15.0


    protected abstract fun calculateContentHeight(): Double


    protected fun updateScrollLimits() {
        val contentHeight = calculateContentHeight()
        maxScroll = (contentHeight - height).coerceAtLeast(0.0)
        scrollOffset = scrollOffset.coerceIn(0.0, maxScroll)
    }

    override fun onScroll(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (isMouseInBounds(mouseX, mouseY)) {
            scrollOffset = (scrollOffset - amount * scrollSpeed).coerceIn(0.0, maxScroll)
            return true
        }
        return false
    }

    protected fun isMouseInBounds(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height
    }


    abstract fun getSettingsComponent(): UIComponent?
}

enum class ViewType {
    GRID,
    LIST
}