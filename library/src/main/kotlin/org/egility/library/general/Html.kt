/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

/**
 * Created by mbrickman on 18/07/18.
 */
interface Element {
    fun render(builder: StringBuilder, indent: String)
}

class TextElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

abstract class Tag(val name: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        if (name.isEmpty()) {
            for (c in children) {
                c.render(builder, indent)
            }
        } else {
            builder.append("$indent<$name${renderAttributes()}>\n")
            for (c in children) {
                c.render(builder, indent + "  ")
            }
            builder.append("$indent</$name>\n")
        }
    }

    private fun renderAttributes(): String? {
        val builder = StringBuilder()
        for (a in attributes.keys) {
            builder.append(" $a=\"${attributes[a]}\"")
        }
        return builder.toString()
    }


    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
}

class HTML() : TagWithText("html") {
    fun head(init: Head.() -> Unit) = initTag(Head(), init)

    fun body(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head() : TagWithText("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)
}

class Title() : TagWithText("title")

abstract class BodyTag(name: String) : TagWithText(name) {
    fun b(init: B.() -> Unit) = initTag(B(), init)
    fun p(init: P.() -> Unit) = initTag(P(), init)
    fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    fun h2(init: H2.() -> Unit) = initTag(H2(), init)
    fun h3(init: H3.() -> Unit) = initTag(H3(), init)
    fun h4(init: H4.() -> Unit) = initTag(H4(), init)
    fun ul(init: UL.() -> Unit) = initTag(UL(), init)

    fun table(init: TABLE.() -> Unit) = initTag(TABLE(), init)
    fun tr(init: TR.() -> Unit) = initTag(TR(), init)
    fun th(init: TH.() -> Unit) = initTag(TH(), init)
    fun td(init: TD.() -> Unit) = initTag(TD(), init)

    fun hr(init: HR.() -> Unit) = initTag(HR(), init)
    fun u(init: U.() -> Unit) = initTag(U(), init)

    fun block(init: BLOCK.() -> Unit) = initTag(BLOCK(), init)

    fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }

    fun button(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
        a.attributes.put("class", "button-link")
        a.attributes.put("style", """
        color: #ffffff !important;
        text-decoration: none !important;
        text-underline: none;
        word-wrap: break-word;
        font-size: 16px;
        font-weight: 700;
        text-transform: none;
        line-height: 16px;
        text-align: center;
        background-color: #4fbdbd;
        border-radius: 1px;
        padding: 1px 10px;
        border: 10px solid #4fbdbd";
        padding-top: 100px;
        """.trimIndent())
    }
}

class Body() : BodyTag("body")
class UL() : BodyTag("ul") {
    fun li(init: LI.() -> Unit) = initTag(LI(), init)
}

class B() : BodyTag("b")
class LI() : BodyTag("li")
class P() : BodyTag("p")
class H1() : BodyTag("h1")
class H2() : BodyTag("h2")
class H3() : BodyTag("h3")
class H4() : BodyTag("h4")

class TABLE() : BodyTag("table")
class TR() : BodyTag("tr")
class TD() : BodyTag("td")
class TH() : BodyTag("th")

class HR() : BodyTag("hr")
class U() : BodyTag("u")

class BLOCK() : BodyTag("")

class A() : BodyTag("a") {
    public var href: String
        get() = attributes["href"]!!
        set(value) {
            attributes["href"] = value
        }
}

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}

fun block(init: BLOCK.() -> Unit): BLOCK {
    val block = BLOCK()
    block.init()
    return block
}