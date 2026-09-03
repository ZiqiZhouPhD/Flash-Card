package com.ziqiphyzhou.flashcard.card_delete.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ziqiphyzhou.flashcard.databinding.FragmentDeleteBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteFragment : Fragment() {

    private lateinit var binding: FragmentDeleteBinding
    private val viewModel: DeleteViewModel by activityViewModels()
    private val adapter = DeleteAdapter(::onDeleteIconClicked, ::onEditIconClicked)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDeleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewDelete.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.recyclerViewDelete.adapter = adapter

        viewModel.viewState.observe(viewLifecycleOwner) { viewState ->
            updateUi(viewState)
        }

    }

    private fun updateUi(viewState: DeleteListViewState) {
        when (viewState) {
            is DeleteListViewState.Content -> {
                binding.recyclerViewDelete.isVisible = true
                adapter.setData(viewState.deleteList)
            }
            DeleteListViewState.Loading -> {
                binding.recyclerViewDelete.isVisible = false
            }
        }
    }

    private fun onDeleteIconClicked(viewState: DeleteCardViewState) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder
            .setTitle("Warning!")
            .setMessage("Do you want to delete \"${viewState.title}\" from the database?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteIconClicked(viewState.id, viewState.title)
                dialog.dismiss()
                view?.findFocus()?.clearFocus()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun onEditIconClicked(viewState: DeleteCardViewState) {
        (activity as DeleteActivity).editCard(viewState.id, viewState.title, viewState.body)
    }
}
