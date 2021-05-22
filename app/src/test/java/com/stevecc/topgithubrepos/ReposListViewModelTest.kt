package com.stevecc.topgithubrepos

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.*
import com.stevecc.topgithubrepos.api.search.Owner
import com.stevecc.topgithubrepos.api.search.Repository
import com.stevecc.topgithubrepos.reposlist.ReposListModel
import com.stevecc.topgithubrepos.reposlist.ReposListViewModel
import org.junit.runner.RunWith
import com.stevecc.topgithubrepos.api.search.RepositoryResults
import com.stevecc.topgithubrepos.reposlist.Intent
import com.stevecc.topgithubrepos.reposlist.State
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReposListViewModelTest {

    companion object {
        private val EMPTY_RESULTS = RepositoryResults(
            total_count = 0,
            incomplete_results = false,
            items = emptyList()
        )

        private val REPOSITORY_LIST = listOf(
            Repository(
                id = 0,
                name = "RepositoryName",
                full_name = "owner/RepositoryName",
                owner = Owner(
                    id = 0,
                    login = "owner"
                ),
                stargazers_count = 1
            )
        )

        private val REPOSITORY_RESULTS = RepositoryResults(
            total_count = 0,
            incomplete_results = false,
            items = REPOSITORY_LIST
        )

        private val NOT_FOUND_ERROR = Throwable(
            message = "Error fetching repositories"
        )
    }

    private lateinit var reposListViewModel: ReposListViewModel

    // Mocks
    private val reposListModel: ReposListModel = mock()

    private val savedState: SavedStateHandle = mock()

    @Before
    fun setup() {
        reposListViewModel = ReposListViewModel(reposListModel = reposListModel,
            savedState = savedState)

        whenever(reposListModel.top100Repositories()).doReturn(Single.just(REPOSITORY_RESULTS))
    }

    @Test
    fun emitsInitialState() {
        whenever(savedState.get<State>(ArgumentMatchers.anyString())).thenReturn(null)

        val states = reposListViewModel.states().test()

        states.assertValuesOnly(State.Initial)
    }

    @Test
    fun emitsLoadingAndEmptyContentState() {
        whenever(savedState.get<State>(ArgumentMatchers.anyString())).thenReturn(null)
        whenever(reposListModel.top100Repositories()).doReturn(Single.just(EMPTY_RESULTS))

        val states = reposListViewModel.states().test()
        reposListViewModel.accept(Intent.Startup)

        states.assertValuesOnly(State.Initial,
            State.Loading,
            State.Content.Empty)

        verify(reposListModel, times(1)).top100Repositories()
    }

    @Test
    fun emitsLoadingAndContentState() {
        whenever(savedState.get<State>(ArgumentMatchers.anyString())).thenReturn(null)

        val states = reposListViewModel.states().test()
        reposListViewModel.accept(Intent.Startup)

        states.assertValuesOnly(State.Initial,
            State.Loading,
            State.Content.RepositoryList(REPOSITORY_LIST))

        verify(reposListModel, times(1)).top100Repositories()
    }

    @Test
    fun emitsErrorState() {
        whenever(savedState.get<State>(ArgumentMatchers.anyString())).thenReturn(null)
        whenever(reposListModel.top100Repositories()).doReturn(Single.error(NOT_FOUND_ERROR))

        val states = reposListViewModel.states().test()
        reposListViewModel.accept(Intent.Startup)

        states.assertValuesOnly(State.Initial,
            State.Loading,
            State.Error(NOT_FOUND_ERROR.message))

        verify(reposListModel, times(1)).top100Repositories()
    }
}
