package jp.shiguredo.sora.sample.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.shiguredo.sora.sample.R
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView

data class Feature (
        val title:       String,
        val description: String
) {}

class FeatureListAdapter(
        private val features: List<Feature>
) : RecyclerView.Adapter<FeatureListAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        return ViewHolder(FeatureListItemUI().createView(AnkoContext.create(parent!!.context)))
    }

    override fun getItemCount(): Int {
        return features.size
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.let {
            it.bind(features[position])
            it.view.setOnClickListener { _ ->
                listener?.onItemClick(position)
            }
        }
    }

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        val title:       TextView = view.findViewById(R.id.feature_title) as TextView
        val description: TextView = view.findViewById(R.id.feature_description) as TextView

        fun bind(feature: Feature) {
            title.text = feature.title
            description.text = feature.description
        }
    }

}

class FeatureListItemUI : AnkoComponent<Context> {

    override fun createView(ui: AnkoContext<Context>): View  = with(ui) {

        return verticalLayout {

            padding = dip(4)
            lparams(width = matchParent, height = wrapContent)

            cardView {

                lparams(width = matchParent, height = wrapContent)

                radius = dip(10).toFloat()
                useCompatPadding = true

                verticalLayout {

                    padding = dip(10)

                    textView {
                        textSize = 20f
                        id = R.id.feature_title
                        padding = dip(2)
                    }

                    textView {
                        textSize = 12f
                        id = R.id.feature_description
                        padding = dip(2)
                    }
                }

            }

        }
    }

}