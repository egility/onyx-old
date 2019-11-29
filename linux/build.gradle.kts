plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api(project(":library"))
    api("com.itextpdf:itextpdf:5.5.13")
    api("com.sun.mail:javax.mail:1.5.6")
    api("net.sourceforge.jexcelapi:jxl:2.6.12")
    api("org.apache.odftoolkit:simple-odf:0.8.2-incubating")
    api("org.apache.pdfbox:pdfbox-app:1.8.14")
    api( "org.bidib.com.pi4j:pi4j-core:1.2.M1")
    api("org.json:json:20140107")
    api("com.stripe:stripe-java:5.21.0")
    api("net.sourceforge.dynamicreports:dynamicreports-adhoc:5.0.0")
    api("net.sourceforge.dynamicreports:dynamicreports-core:5.0.0")
    api("org.jsoup:jsoup:1.12.1")
    api("com.pi4j:pi4j-core:1.2")
}