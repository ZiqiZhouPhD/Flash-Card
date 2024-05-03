package com.ziqiphyzhou.flashcard.card_delete.presentation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.databinding.FragmentDeleteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeleteFragment : Fragment() {

    private lateinit var binding: FragmentDeleteBinding
    private val viewModel: DeleteViewModel by activityViewModels()
    private val adapter = DeleteAdapter(::onDeleteIconClicked)

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

        viewModel.deleteCardSuccessMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
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
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.deleteIconClicked(viewState.id, viewState.title)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}