package dev.noctud.latte.ext

import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass

fun PhpClass.isSubclassOf(fqn: String): Boolean {
    var current = superClass
    while (current != null) {
        if (current.fqn == fqn) {
            return true
        }
        current = current.superClass
    }

    return false
}

/** Every class extending this one, however far down. */
fun PhpClass.allSubclasses(): List<PhpClass> {
    val index = PhpIndex.getInstance(project)
    val out = mutableListOf<PhpClass>()
    fun collect(fqn: String) {
        for (subclass in index.getDirectSubclasses(fqn)) {
            out.add(subclass)
            collect(subclass.fqn)
        }
    }
    collect(fqn)

    return out
}
