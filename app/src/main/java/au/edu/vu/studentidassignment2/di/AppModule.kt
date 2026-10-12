package au.edu.vu.studentidassignment2.di

import au.edu.vu.studentidassignment2.config.AppConfig
import au.edu.vu.studentidassignment2.data.ApiAssignmentRepository
import au.edu.vu.studentidassignment2.data.AssignmentApi
import au.edu.vu.studentidassignment2.data.AssignmentRepository
import au.edu.vu.studentidassignment2.data.DashboardParser
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardViewModel
import au.edu.vu.studentidassignment2.ui.details.DetailsViewModel
import au.edu.vu.studentidassignment2.ui.login.LoginViewModel
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single<AssignmentApi> { get<Retrofit>().create(AssignmentApi::class.java) }
    single { DashboardParser() }
    single<AssignmentRepository> { ApiAssignmentRepository(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { DetailsViewModel(get(), get()) }
}
