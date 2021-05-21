package com.stevecc.topgithubrepos.reposlist.destinations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.stevecc.topgithubrepos.R
import com.stevecc.topgithubrepos.api.search.SearchService
import com.stevecc.topgithubrepos.databinding.TopReposListFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

/*
 * Fragment displaying a list of GitHub repositories and the top contributor.
 */
@AndroidEntryPoint
class ReposListFragment : Fragment(R.layout.top_repos_list_fragment) {
    @Inject
    lateinit var searchService: SearchService

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
        compositeDisposable.add(searchService.repositories(searchQuery = "stars:>0")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    // Success
                    views.label.text = "Repos: ${it.total_count}"
                },
                {
                    // Error
                    views.label.text = "Error loading repos: ${it.message}"
                }
            ))
    }
}
