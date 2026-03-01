package com.Neo.permissionauditor.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.Neo.permissionauditor.model.AppPrivacyInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExportUtils {

    suspend fun exportToCsv(context: Context, uri: Uri, apps: List<AppPrivacyInfo>) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            // Write the Excel Header Row
            writer.write("App Name,Package Name,System App,Camera Granted,Mic Granted,Location Granted,Risk Level,1-Day Usage\n")
            
            // Write the App Data
            apps.forEach { app ->
                // Clean commas out of the app name so it doesn't break the CSV columns
                val cleanName = app.appName.replace(",", "")
                writer.write("$cleanName,${app.packageName},${app.isSystemApp},${app.isCameraGranted},${app.isMicrophoneGranted},${app.isLocationGranted},${app.riskLevel.name},${app.usage1Day}\n")
            }
        }
    }

    suspend fun exportToPdf(context: Context, uri: Uri, apps: List<AppPrivacyInfo>) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 12f }
        val titlePaint = Paint().apply { 
            textSize = 18f
            isFakeBoldText = true 
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // Standard A4 Size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 50f

        canvas.drawText("Permission Auditor PRO - Security Report", 50f, yPosition, titlePaint)
        yPosition += 40f

        apps.forEach { app ->
            // If we hit the bottom of the page, create a new one!
            if (yPosition > 780f) {
                document.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            canvas.drawText("App: ${app.appName} (${app.packageName})", 50f, yPosition, titlePaint)
            yPosition += 15f
            canvas.drawText("Risk: ${app.riskLevel.name} | Camera: ${if(app.isCameraGranted) "YES" else "NO"} | Mic: ${if(app.isMicrophoneGranted) "YES" else "NO"} | Location: ${if(app.isLocationGranted) "YES" else "NO"}", 60f, yPosition, paint)
            yPosition += 15f
            canvas.drawText("Screen Time (24h): ${app.usage1Day} | Total Permissions: ${app.totalPermissionsRequested}", 60f, yPosition, paint)
            yPosition += 25f // Space between apps
        }

        document.finishPage(page)

        // Save the document to the user's chosen location
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()
    }
}