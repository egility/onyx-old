package org.egility.library.general

import java.util.ArrayList



open class TreeNode<T: TreeNode<T>>(val parent: TreeNode<T>? = null): Collection<T> {

    val children = ArrayList<T>()

    init {
        parent?.children?.add(this as T)
    }

    override val size: Int
        get() = children.size

    override fun contains(element: T): Boolean {
        return children.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return children.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return children.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return children.iterator()
    }
    
    fun moveBefore(node: T) {
        if (parent?.contains(node) == true) {
            parent.children.remove(this)
            parent.children.add(parent.children.indexOf(node), this as T)
            
        }
    }


}