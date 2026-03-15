package dev.noctud.latte.psi.elements;

import dev.noctud.latte.psi.LatteHtmlOpenTag;

public interface LatteHtmlTagContainerElement extends LattePsiElement {

    LatteHtmlOpenTag getHtmlOpenTag();

}
