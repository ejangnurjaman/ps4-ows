package com.truecarbide.ps4ows

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class Ps4HttpServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    private val toolkitDir = File(context.filesDir, "toolkit")

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        
        // Default to index.html for root
        val relativePath = if (uri == "/" || uri.isEmpty()) "index.html" else uri.removePrefix("/")
        val file = File(toolkitDir, relativePath)

        if (file.exists() && file.isFile) {
            val mimeType = getMimeType(relativePath)
            return try {
                newFixedLengthResponse(
                    Response.Status.OK,
                    mimeType,
                    FileInputStream(file),
                    file.length()
                )
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error serving file")
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found: $relativePath")
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> MIME_HTML
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".bin") -> "application/octet-stream"
            else -> MIME_PLAINTEXT
        }
    }
}
