package com.stevecc.topgithubrepos.reposlist.destinations

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stevecc.topgithubrepos.R
import com.stevecc.topgithubrepos.api.search.Repository
import com.stevecc.topgithubrepos.databinding.RepositoryListItemBinding
import com.stevecc.topgithubrepos.databinding.TopReposListFragmentBinding
import com.stevecc.topgithubrepos.reposlist.ChangeOrEffect.Effect
import com.stevecc.topgithubrepos.reposlist.Intent
import com.stevecc.topgithubrepos.reposlist.ReposListViewModel
import com.stevecc.topgithubrepos.reposlist.State
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

/*
 * Fragment displaying a list of GitHub repositories and the top contributor.
 */
class ReposListFragment : Fragment(R.layout.top_repos_list_fragment) {
    private val reposListViewModel: ReposListViewModel by hiltNavGraphViewModels(R.id.main_graph)
    private val repositoryAdapter = RepositoryAdapter().apply {
        setHasStableIds(true)
    }

    private lateinit var views: TopReposListFragmentBinding
    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        views = TopReposListFragmentBinding.bind(view)

        views.repoRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), RecyclerView.VERTICAL, false)
        views.repoRecyclerView.addItemDecoration(
            DividerItemDecoration(context, RecyclerView.VERTICAL)
        )
        views.repoRecyclerView.adapter = repositoryAdapter
    }

    override fun onStart() {
        super.onStart()

        with(compositeDisposable) {
            add(reposListViewModel.states()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::render))

            add(reposListViewModel.effects()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::handleEffects))
        }

        reposListViewModel.accept(Intent.Startup)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    /*
     * Render the State into a representation for this View
     */
    private fun render(state: State) {
        when (state) {
            State.Initial -> {
                // display nothing
                views.loadingView.isVisible = false
                views.errorView.isVisible = false
                views.emptyView.isVisible = false
                views.repoRecyclerView.isVisible = false
            }
            State.Loading -> {
                // display loading state
                views.loadingView.isVisible = true
                views.errorView.isVisible = false
                views.emptyView.isVisible = false
                views.repoRecyclerView.isVisible = false
            }
            is State.Error -> {
                // display error state
                views.loadingView.isVisible = false
                views.errorView.isVisible = true
                views.emptyView.isVisible = false
                views.repoRecyclerView.isVisible = false
            }
            is State.Content.Empty -> {
                // display empty state
                views.loadingView.isVisible = false
                views.errorView.isVisible = false
                views.emptyView.isVisible = true
                views.repoRecyclerView.isVisible = false
                views.emptyView.text = getString(R.string.repository_list_empty)
            }
            is State.Content.RepositoryList -> {
                // display content state
                views.loadingView.isVisible = false
                views.errorView.isVisible = false
                views.emptyView.isVisible = false
                views.repoRecyclerView.isVisible = true
                repositoryAdapter.setRepositories(state.repositoryList)
            }
        }
    }

    /*
     * Handle any transient Effects relevant to this View
     */
    private fun handleEffects(effect: Effect) {
        when (effect) {
            // TODO: handle effects
            else -> Unit
        }
    }
}

private class RepositoryAdapter: RecyclerView.Adapter<RepositoryAdapter.ViewHolder>() {
    private val items: MutableList<Repository> = emptyList<Repository>().toMutableList()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var views: RepositoryListItemBinding = RepositoryListItemBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.repository_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repository = items[position]

        holder.views.repositoryNameLabel.text = repository.full_name

        holder.views.repositoryStars.text = "${repository.stargazers_count}"
        // If no top contributor available, show progress bar
        if (false) {
            holder.views.repositoryTopContributor.isVisible = true
            holder.views.repositoryTopContributorProgress.isVisible = false
            // TODO: Trigger fetch of top contributor
        } else {
            holder.views.repositoryTopContributor.isVisible = false
            holder.views.repositoryTopContributorProgress.isVisible = true
        }
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setRepositories(repositoryList: List<Repository>) {
        items.clear()
        items.addAll(repositoryList)
        // Because we're replacing the entire adapter, it's expected to notify that all items were changed for now.
        notifyDataSetChanged()
    }
}
