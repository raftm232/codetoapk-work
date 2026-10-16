package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.storage.FileSaver
import org.junit.Assert.assertEquals
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
    assertEquals("أداة التشفير", appName)
  }

  @Test
  fun `verify file mime types`() {
    assertEquals("text/plain", FileSaver.getMimeType("main.lua"))
    assertEquals("application/zip", FileSaver.getMimeType("scripts.zip"))
    assertEquals("application/octet-stream", FileSaver.getMimeType("game.457"))
    assertEquals("application/octet-stream", FileSaver.getMimeType("data.ffsz"))
  }
}
