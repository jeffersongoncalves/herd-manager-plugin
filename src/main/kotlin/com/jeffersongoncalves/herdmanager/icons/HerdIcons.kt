package com.jeffersongoncalves.herdmanager.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object HerdIcons {
    @JvmField
    val HERD: Icon = IconLoader.getIcon("/icons/herd.svg", HerdIcons::class.java)

    @JvmField
    val LINKED: Icon = IconLoader.getIcon("/icons/herd_linked.svg", HerdIcons::class.java)

    @JvmField
    val UNLINKED: Icon = IconLoader.getIcon("/icons/herd_unlinked.svg", HerdIcons::class.java)
}
