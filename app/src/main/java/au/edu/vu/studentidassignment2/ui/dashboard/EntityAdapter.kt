package au.edu.vu.studentidassignment2.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.databinding.ItemEntityBinding
import au.edu.vu.studentidassignment2.domain.Entity
import au.edu.vu.studentidassignment2.ui.fieldLabel
import au.edu.vu.studentidassignment2.ui.fieldValue
import au.edu.vu.studentidassignment2.ui.titleField

class EntityAdapter(private val onClick: (Entity) -> Unit) : ListAdapter<Entity, EntityAdapter.Holder>(Diff) {
    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemEntityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position), position)

    inner class Holder(private val binding: ItemEntityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: Entity, position: Int) {
            val context = binding.root.context
            val fields = entity.summaryFields
            val titleField = entity.titleField()
            val title = titleField?.value ?: context.getString(R.string.entity_fallback, position + 1)
            binding.title.text = title
            binding.number.text = context.getString(R.string.item_number, position + 1)
            binding.kind.text = titleField?.let { fieldLabel(it.name) } ?: context.getString(R.string.entity_label)
            val badgeColor = when (position % 3) {
                0 -> R.color.primary
                1 -> R.color.lime
                else -> R.color.secondary
            }
            binding.number.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, badgeColor))
            binding.number.setTextColor(ContextCompat.getColor(context, if (position % 3 == 1) R.color.primary else R.color.on_primary))
            binding.summary.text = fields.filter { it !== titleField }.take(3)
                .joinToString("\n") { "${fieldLabel(it.name)}: ${context.fieldValue(it.value)}" }
                .ifBlank { context.getString(R.string.no_summary) }
            binding.root.contentDescription = "${context.getString(R.string.open_entity, title)}. ${binding.summary.text}"
            binding.root.setOnClickListener { onClick(entity) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Entity>() {
        override fun areItemsTheSame(oldItem: Entity, newItem: Entity) = oldItem.rawJson == newItem.rawJson
        override fun areContentsTheSame(oldItem: Entity, newItem: Entity) = oldItem == newItem
    }
}
