package com.stevecc.topgithubrepos.reposlist.destinations

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.stevecc.topgithubrepos.R
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

    private lateinit var views: TopReposListFragmentBinding
    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        views = TopReposListFragmentBinding.bind(view)
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
                views.contentView.isVisible = false
            }
            State.Loading -> {
                // display loading state
                views.loadingView.isVisible = true
                views.errorView.isVisible = false
                views.emptyView.isVisible = false
                views.contentView.isVisible = false
            }
            is State.Error -> {
                // display error state
                views.loadingView.isVisible = false
                views.errorView.isVisible = true
                views.emptyView.isVisible = false
                views.contentView.isVisible = false
            }
            is State.Content.Empty -> {
                // display empty state
                views.loadingView.isVisible = false
                views.errorView.isVisible = false
                views.emptyView.isVisible = true
                views.contentView.isVisible = false
                views.emptyView.text = "No repositories were found"
            }
            is State.Content.RepositoryList -> {
                // display content state
                views.loadingView.isVisible = false
                views.errorView.isVisible = false
                views.emptyView.isVisible = false
                views.contentView.isVisible = true
                views.contentView.text = "Repositories found: ${state.repositoryList.size}"
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
