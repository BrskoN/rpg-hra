package com.example

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Installs a global uncaught-exception handler so a crash shows a readable, copyable stack trace
 * (CrashReportActivity) instead of the opaque system "app has stopped" dialog. Needed because
 * on-device testing has no adb/logcat access, so this is the only way to see what actually failed.
 */
class ChroniclesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashReportActivity.EXTRA_STACK_TRACE, writer.toString())
                }
                startActivity(intent)
            } finally {
                Process.killProcess(Process.myPid())
                exitProcess(1)
            }
        }
    }
}
