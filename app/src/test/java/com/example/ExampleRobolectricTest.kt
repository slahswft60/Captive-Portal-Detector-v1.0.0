package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Captive Portal", appName)
  }

  @Test
  fun `build foreground notification succeeds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    NotificationHelper.createNotificationChannels(context)
    val helper = NotificationHelper(context)
    val notif = helper.buildForegroundNotification("مراقبة الشبكة نشطة")
    assertNotNull(notif)
  }
}

