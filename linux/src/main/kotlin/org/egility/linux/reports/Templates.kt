/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.builder.component.ComponentBuilder
import net.sf.dynamicreports.report.constant.*
import java.awt.Color
import java.util.*

val rootStyle = stl.style()
        .setPadding(2)
        .setVerticalTextAlignment(VerticalTextAlignment.BOTTOM)
        .setFontName("Arial")
val boldStyle = stl.style(rootStyle)
        .bold()
val bold12Style = stl.style(boldStyle)
        .setFontSize(12)
val root12Style = stl.style(rootStyle)
        .setFontSize(12)
val root18Style = stl.style(rootStyle)
        .setFontSize(16)
val root18StyleItalic = stl.style(rootStyle)
        .setFontSize(16)
        .italic()
val smallItalicStyle = stl.style(rootStyle)
        .italic()
        .setFontSize(8)


val italicStyle = stl.style(rootStyle)
        .italic()

val footerStyle = rootStyle
        .setVerticalTextAlignment(VerticalTextAlignment.TOP)
val footerBoldStyle = stl
        .style(footerStyle).bold()
val footerItalicStyle = stl
        .style(footerStyle)
        .italic()

val columnStyle = stl
        .style(rootStyle)
val columnStyleRight = stl
        .style(columnStyle)
        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
val columnStyleCenter = stl
        .style(columnStyle)
        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
val columnTitleStyle = stl
        .style(columnStyle)
        .setBorder(stl.pen1Point().setLineWidth(0.1f))
        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
        .setBackgroundColor(Color(0.90f, 0.90f, 0.90f))

val oddStyle = stl
        .simpleStyle()
        .setBackgroundColor(Color(0.95f, 0.95f, 0.95f))

val reportTemplate = template()
        .setPageFormat(PageType.A4, PageOrientation.PORTRAIT)
        .setPageMargin(margin().setLeft(44).setRight(18).setTop(18).setBottom(18)) //
        .setLocale(Locale.ENGLISH)
        .setColumnStyle(columnStyle)
        .setColumnTitleStyle(columnTitleStyle)
        .setSubtotalStyle(boldStyle)
        .highlightDetailOddRows()


val footerComponent: ComponentBuilder<*, *> = cmp.horizontalList().add(
        cmp.text("")
                .setStyle(footerItalicStyle)
                .setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
        cmp.pageXofY()
                .setStyle(footerItalicStyle)
                .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
        cmp.text("© e-gility systems")
                .setStyle(footerItalicStyle)
                .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
).setStyle(stl.style(rootStyle)
        .setTopBorder(stl.pen1Point()))

fun createTitleComponent(main: String, left: String, right: String): ComponentBuilder<*, *> {
    return cmp.verticalList().add(
            cmp.text(main)
                    .setStyle(italicStyle)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
            cmp.horizontalList().add(
                    cmp.text(left)
                            .setStyle(root18Style)
                            .setMinWidth(350)
                            .setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
                    cmp.text(right)
                            .setStyle(root18Style)
                            .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
            ).newRow()
                    .add(cmp.line())
                    .newRow()
                    .add(cmp.verticalGap(10))
    )
}

fun createTournamentTitleComponent(logoPath: String, classTitle: String, document: String): ComponentBuilder<*, *> {
    return cmp.verticalList().add(
            cmp.image(logoPath)
                    .setHorizontalImageAlignment(HorizontalImageAlignment.CENTER),
            cmp.filler().setHeight(10),
            cmp.text(classTitle)
                    .setStyle(root18Style)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
            cmp.filler().setHeight(10),
            cmp.text(document)
                    .setStyle(root18StyleItalic)
                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
    )
}

fun createTournamentFooterComponent(logoPath: String, website: String): ComponentBuilder<*, *> {
    val result = cmp.verticalList().setHeight(150).add(
            cmp.image(logoPath)
                    .setHorizontalImageAlignment(HorizontalImageAlignment.CENTER)
    )
    if (website.isNotEmpty()) {
        result.add(
                cmp.text(website)
                        .setStyle(root12Style)
                        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
        )
    }
    return result
}

