package com.ziqiphyzhou.flashcard.card_delete.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.databinding.ItemDeleteBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

class DeleteAdapter(
    val onDeleteClicked: (DeleteCardViewState) -> Unit
) : RecyclerView.Adapter<DeleteAdapter.ViewHolder>() {

    private var data: List<DeleteCardViewState> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_delete, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(deleteList: List<DeleteCardViewState>) {
        this.data = deleteList.sortedBy { it.title }
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(deleteCardViewState: DeleteCardViewState) {
            val bind = ItemDeleteBinding.bind(itemView)

            bind.tvDeleteItemTitle.text = deleteCardViewState.title
            bind.tvDeleteItemBody.text = deleteCardViewState.body
            bind.buttonDeleteCardItem.setOnClickListener {
                onDeleteClicked(deleteCardViewState)
            }

        }

    }
}