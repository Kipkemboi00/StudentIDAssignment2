package au.edu.vu.studentidassignment2

import android.app.Application
import au.edu.vu.studentidassignment2.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AssignmentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AssignmentApplication)
            modules(appModule)
        }
    }
}
