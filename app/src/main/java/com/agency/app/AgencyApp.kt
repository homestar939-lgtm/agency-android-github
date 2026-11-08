package com.agency.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * نقطة تجميع الاعتماديات.
 *
 * كل الوحدات (قاعدة البيانات، المستودعات، الجلسة) تُبنى مرة واحدة وتُشارَك
 * بين الشاشات، فلا تُفتح قاعدة بيانات ثانية ولا تُكرَّر الجلسة.
 */
@HiltAndroidApp
class AgencyApp : Application()