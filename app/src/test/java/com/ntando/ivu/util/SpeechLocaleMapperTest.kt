package com.ntando.ivu.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SpeechLocaleMapperTest {

    @Test
    fun `maps language codes to South African locales correctly`() {
        assertEquals(Locale("en", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("en"))
        assertEquals(Locale("en", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("english"))
        assertEquals(Locale("zu", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("zu"))
        assertEquals(Locale("zu", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("isizulu"))
        assertEquals(Locale("af", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("af"))
        assertEquals(Locale("af", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("afrikaans"))
    }

    @Test
    fun `unknown or blank codes fall back to en-ZA locale`() {
        assertEquals(Locale("en", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("unknown"))
        assertEquals(Locale("en", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode(""))
        assertEquals(Locale("en", "ZA"), SpeechLocaleMapper.getLocaleForLanguageCode("  "))
    }
}
