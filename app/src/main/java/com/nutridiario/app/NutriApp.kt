package com.nutridiario.app

import android.app.Application

class NutriApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        LocalFoodDb.init(this)
    }
}
