package au.edu.vu.studentidassignment2

import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.vu.studentidassignment2.data.DashboardParser
import au.edu.vu.studentidassignment2.domain.Dashboard
import au.edu.vu.studentidassignment2.ui.MainActivity
import au.edu.vu.studentidassignment2.ui.dashboard.DashboardViewModel
import com.google.android.material.appbar.MaterialToolbar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.ext.android.getViewModel

@RunWith(AndroidJUnit4::class)
class CollectionScreensTest {
    @Test fun cardsOpenReadableDetailsWithEveryField() {
        val entity = DashboardParser().parseEntity("""
            {"id":42,"bookTitle":"The garden","year":2024,"author":null,
             "metadata":{"category":"Botany"},"description":"A field guide to growing a garden."}
        """.trimIndent())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.getViewModel<DashboardViewModel>().openSession("test-topic", Dashboard(listOf(entity), 1))
                activity.showDashboard()
                activity.supportFragmentManager.executePendingTransactions()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.entities)
                assertEquals(1, list.adapter!!.itemCount)
                val card = list.findViewHolderForAdapterPosition(0)!!.itemView
                assertEquals("The garden", card.findViewById<TextView>(R.id.title).text.toString())
                assertFalse(card.findViewById<TextView>(R.id.summary).text.contains("field guide"))
                card.performClick()
                activity.supportFragmentManager.executePendingTransactions()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals("The garden", activity.findViewById<TextView>(R.id.heading).text.toString())
                assertNotNull(activity.findViewById<MaterialToolbar>(R.id.toolbar).navigationIcon)
                val facts = activity.findViewById<GridLayout>(R.id.fields)
                assertEquals(5, facts.childCount)
                assertEquals(activity.getString(R.string.no_value), facts.getChildAt(3).findViewById<TextView>(R.id.value).text.toString())
                val nested = facts.getChildAt(4)
                assertTrue(nested.findViewById<TextView>(R.id.value).text.contains("Botany"))
                val descriptions = activity.findViewById<LinearLayout>(R.id.descriptions)
                assertEquals(1, descriptions.childCount)
                assertEquals("A field guide to growing a garden.", descriptions.getChildAt(0).findViewById<TextView>(R.id.value).text.toString())
            }
        }
    }
}
