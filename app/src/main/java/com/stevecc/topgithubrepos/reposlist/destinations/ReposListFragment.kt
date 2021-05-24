package com.stevecc.topgithubrepos.reposlist.destinations

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay3.PublishRelay
import com.stevecc.topgithubrepos.R
import com.stevecc.topgithubrepos.api.repository.Contributor
import com.stevecc.topgithubrepos.api.search.Repository
import com.stevecc.topgithubrepos.databinding.RepositoryListItemBinding
import com.stevecc.topgithubrepos.databinding.TopReposListFragmentBinding
import com.stevecc.topgithubrepos.reposlist.ChangeOrEffect.Effect
import com.stevecc.topgithubrepos.reposlist.ChangeOrEffect.Effect.TopContributorsLoaded
import com.stevecc.topgithubrepos.reposlist.Intent
import com.stevecc.topgithubrepos.reposlist.ReposListViewModel
import com.stevecc.topgithubrepos.reposlist.State
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

/*
 * Fragment displaying a list of GitHub repositories and the top contributor.
 */
class ReposListFragment : Fragment(R.layout.top_repos_list_fragment),
    RepositoryAdapter.IntentsTarget {
    private val reposListViewModel: ReposListViewModel by hiltNavGraphViewModels(R.id.main_graph)
    private val repositoryAdapter = RepositoryAdapter(this).apply {
        setHasStableIds(true)
    }
    private val intentsPublishRelay = PublishRelay.create<Intent>()

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

            add(intentsPublishRelay
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(reposListViewModel::accept))
        }

        reposListViewModel.accept(Intent.Startup)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onIntent(intent: Intent) {
        intentsPublishRelay.accept(intent)
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
            is TopContributorsLoaded -> {
                repositoryAdapter.setTopContributors(effect.repositoryId, effect.topContributors)
            }
            // TODO: handle error loading top contributor case (clear progress) - eg in case of rate limiting.
            else -> Unit
        }
    }
}

private class RepositoryAdapter constructor(val intentsTarget: IntentsTarget): RecyclerView.Adapter<RepositoryAdapter.ViewHolder>() {
    private val items: MutableList<Repository> = emptyList<Repository>().toMutableList()
    private val topContributorsMap = emptyMap<Int, List<Contributor>>().toMutableMap()
    private val repositoryIdToPositionMap = emptyMap<Int, Int>().toMutableMap()

    interface IntentsTarget {
        fun onIntent(intent: Intent)
    }

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
        if (topContributorsMap.containsKey(repository.id)) {
            holder.views.repositoryTopContributor.isVisible = true
            holder.views.repositoryTopContributorProgress.isVisible = false

            val topContributors = topContributorsMap[repository.id]!!

            with(holder.views.repositoryTopContributor) {
                doOnLayout {
                    // get width of view from layoutlistener
                    val textViewWidth = it.width
                    var numberOfContributorsSuccessfullyTested = 0
                    var textToMeasure: String
                    var topContributorTextWidth: Float
                    var contributorsToTest: Int
                    // Progressively test longer and longer lengths of contributor strings
                    // When one exceeds the bounds of the view, break (to use the previously successful string)
                    for ((index) in topContributors.withIndex()) {
                        contributorsToTest = index + 1
                        textToMeasure = topContributorsStringWithOverflow(topContributors, contributorsToTest)
                        topContributorTextWidth = holder.views.repositoryTopContributor.paint.measureText(textToMeasure)

                        if (topContributorTextWidth < textViewWidth) {
                            // if measuretext of contributors is less than total length, it can be displayed
                            numberOfContributorsSuccessfullyTested = contributorsToTest
                        } else {
                            // otherwise, we cannot display this number of controbutors
                            break
                        }
                    }

                    // TODO: better handling for the edge case where numberOfContributorsSuccessfullyTested is zero
                    text = topContributorsStringWithOverflow(topContributors, numberOfContributorsSuccessfullyTested)
                }
            }
        } else {
            holder.views.repositoryTopContributor.isVisible = false
            holder.views.repositoryTopContributorProgress.isVisible = true
            // Trigger a fetch of the top contributor for the repository
            intentsTarget.onIntent(Intent.FetchTopContributorsForRepository(repository))
        }
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setRepositories(repositoryList: List<Repository>) {
        items.clear()
        repositoryList.forEach {
            items.add(it)
            repositoryIdToPositionMap[it.id] = items.size - 1
        }
        // Because we're replacing the entire adapter, it's expected to notify that all items were changed for now.
        notifyDataSetChanged()
    }

    fun setTopContributors(repositoryId: Int, topContributors: List<Contributor>) {
        topContributorsMap[repositoryId] = topContributors
        repositoryIdToPositionMap[repositoryId]?.let { notifyItemChanged(it) }
    }

    private fun topContributorsStringWithOverflow(allContributors: List<Contributor>, numContributorsToDisplay: Int): String {
        val stringBuilder = StringBuilder()

        if (numContributorsToDisplay == 0) {
            stringBuilder.append("${allContributors.size} contributors")
            return stringBuilder.toString()
        }

        allContributors.take(numContributorsToDisplay).withIndex().forEach {
            if (it.index > 0) {
                stringBuilder.append(", ")
            }
            stringBuilder.append(it.value.login)
        }

        if (numContributorsToDisplay < allContributors.size) {
            stringBuilder.append(" and ${allContributors.size - numContributorsToDisplay} more")
        }

        return stringBuilder.toString()
    }
}
