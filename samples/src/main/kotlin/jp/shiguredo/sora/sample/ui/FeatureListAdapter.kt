package jp.shiguredo.sora.sample.ui

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.shiguredo.sora.sample.R
import android.view.LayoutInflater


data class Feature (
        val title:       String,
        val description: String
)

class FeatureListAdapter(
        private val features: List<Feature>
) : RecyclerView.Adapter<FeatureListAdapter.ViewHolder>() {

    fun interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val v = layoutInflater.inflate(R.layout.feature_list_item, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return features.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.let {
            it.bind(features[position])
            it.view.setOnClickListener { listener?.onItemClick(position) }
        }
    }

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val title:       TextView = view.findViewById(R.id.featureTitle) as TextView
        private val description: TextView = view.findViewById(R.id.featureDescription) as TextView

        fun bind(feature: Feature) {
            title.text = feature.title
            description.text = feature.description
        }
    }

}
