/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import org.egility.android.tools.androidApplication
import org.egility.granite.activities.PanicDialog
import org.egility.library.api.ApiUtils.API_PORT
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import java.io.File

/**
 * Created by mbrickman on 17/12/15.
 */

fun showLateEntryStats() {
    val sql = """
        SELECT
            SUM(IF(type = $ITEM_LATE_ENTRY_PAID, quantity, 0)) AS lateEntries,
            SUM(IF(type = $ITEM_LATE_ENTRY_DISCRETIONARY, quantity, 0)) AS discretionary,
            SUM(IF(type = $ITEM_LATE_ENTRY_STAFF, quantity, 0)) AS staff,
            SUM(IF(type = $ITEM_LATE_ENTRY_UKA, quantity, 0)) AS rep,
            SUM(IF(type = $ITEM_LATE_ENTRY_TRANSFER, quantity, 0)) AS transfers,
            SUM(IF(type = 150 && promised <> 0, amount, 0)) AS accountPayments,
            SUM(cash) AS cash,
            SUM(cheque) AS cheques,
            SUM(promised) AS inPost
        FROM
            competitionLedger
        WHERE
            idCompetition = ${Competition.current.id} AND
            DATE(dateCreated) = ${today.sqlDate}
    """
    val query = DbQuery(sql)
    if (query.found()) {
        var stats = "Late Entries: ${query.getInt("lateEntries")}"
        stats += "\nDiscretionary: ${query.getInt("discretionary")}"
        stats += "\nStaff: ${query.getInt("staff")}"
        stats += "\nTransfers: ${query.getInt("transfers")}"
        stats += "\nAccountPayments: ${query.getInt("accountPayments").money}"
        stats += "\nCash: ${query.getInt("cash").money}"
        stats += "\nCheques: ${query.getInt("cheques").money}"
        stats += "\nTotal (Cash + Cheques): ${(query.getInt("cheques") + query.getInt("cash")).money}"
        Global.services.popUp("Show Stats", stats)
    }
}

fun updateApp() {
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val granite = File(downloads, "granite.apk")
    if (granite.exists()) {
        granite.delete()
    }

    val url = "http://${Global.databaseHost}:${API_PORT}/v1.0/granite"
    val request = DownloadManager.Request(Uri.parse(url))
    request.setDescription("e-gility update")
    request.setTitle("downloading")
    request.allowScanningByMediaScanner()
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "granite.apk")
    val manager = androidApplication.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(File(downloads, "granite.apk")), "application/vnd.android.package-archive")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            androidApplication.startActivity(intent)
        }
    }

    androidApplication.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    manager.enqueue(request)
}

fun downloadPdf(pdfFile: String) {
    val fileName = "temp.pdf"

    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val localFile = File(downloads, fileName)
    if (localFile.exists()) {
        localFile.delete()
    }

    val url = "http://${Global.services.acuHostname}:${API_PORT}/v1.0/pdf/$pdfFile"
    val request = DownloadManager.Request(Uri.parse(url))
    request.setDescription("Getting Report")
    request.setTitle("downloading")
    request.allowScanningByMediaScanner()
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    val manager = androidApplication.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setPackage("com.boyue.adobereader")
            intent.setDataAndType(Uri.fromFile(File(downloads, fileName)), "application/pdf")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            androidApplication.startActivity(intent)
        }
    }
    androidApplication.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    manager.enqueue(request)
}

fun updateAppfromCard() {
    val intent = Intent(Intent.ACTION_VIEW)
    val map = System.getenv();
    val sdcard = System.getenv("SECOND_VOLUME_STORAGE");
    val apk = File(sdcard, "granite.apk")

    intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive")
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    androidApplication.startActivity(intent)
}

fun displayPanicDialog() {
    val current = androidApplication.currentActivity
    val intent = Intent(current, PanicDialog::class.java)
    current.startActivity(intent)
}
