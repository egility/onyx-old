/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

/**
 * Created by mbrickman on 30/07/18.
 */

import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfSmartCopy

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PDFDuplicateFontsRemover {

    fun normalizeFile(srcPdfFile: File): File {
        val pdfDocument = Document()
        var pdfReader: PdfReader? = null
        var fileOutputStream: FileOutputStream? = null
        val parentDir = ""

        val newPdfFile = srcPdfFile.name + "_" + UUID.randomUUID().toString()

        try {
            val tmpPdfFile = File(parentDir + newPdfFile)
            fileOutputStream = FileOutputStream(tmpPdfFile)
            val pdfSmartCopy = PdfSmartCopy(pdfDocument, fileOutputStream)
            pdfDocument.open()
            pdfReader = PdfReader(srcPdfFile.canonicalPath)

            // Where the magic happens
            for (i in 1..pdfReader.numberOfPages) {
                pdfSmartCopy.addPage(pdfSmartCopy.getImportedPage(pdfReader, i))
            }
            pdfDocument.close()
            return tmpPdfFile
        } finally {
            if (pdfReader != null) {
                pdfReader.close()
            }
            if (fileOutputStream != null) {
                fileOutputStream.close()
            }
            pdfReader = null
            fileOutputStream = null
        }
    }

}