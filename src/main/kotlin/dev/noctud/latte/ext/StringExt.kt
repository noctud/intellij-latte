package dev.noctud.latte.ext

fun String.capitalized(): String {
    return replaceFirstChar { it.titlecase() }
}

fun String.decapitalized(): String {
    return replaceFirstChar { it.lowercase() }
}

fun String.isCapitalized(): Boolean {
    return this == capitalized()
}
