package com.stevecc.topgithubrepos.reposlist

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.stevecc.topgithubrepos.api.search.Repository
import com.stevecc.topgithubrepos.api.search.RepositoryResults
import com.stevecc.topgithubrepos.reposlist.ChangeOrEffect.Change
import com.stevecc.topgithubrepos.reposlist.ChangeOrEffect.Effect
import com.stevecc.topgithubrepos.reposlist.Intent.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ReposListViewModel @Inject constructor(
    private val reposListModel: ReposListModel,
    private val savedState: SavedStateHandle
) : ViewModel(), Consumer<Intent> {

    private val compositeDisposable = CompositeDisposable()

    // PublishRelay emits only subsequent items after subscription.
    private val intentsRelay: PublishRelay<Intent> = PublishRelay.create()
    private val effectsRelay: PublishRelay<Effect> = PublishRelay.create()
    // BehaviorRelay emits the most recent observed item and all subsequent items.
    private val statesRelay: BehaviorRelay<State> = BehaviorRelay.create()

    init {
        with(compositeDisposable) {
            add(intentsRelay
                .doOnNext {
                    Log.d("ReposListViewModel","INTENT $it")
                }
                .flatMap(::map)
                .scan(initialState(), ::reduce)
                .distinctUntilChanged()
                .subscribe(statesRelay))

            effectsRelay
                .doOnNext {
                    Log.d("ReposListViewModel", "EFFECT $it")
                }

            add(statesRelay.subscribe {
                Log.d("ReposListViewModel","STATE $it")
                if (it is State.Content) {
                    // Save the state using the saved state handle, outside of the ViewModel's lifecycle.
                    savedState.set("savedState", it)
                }
            })
        }
    }

    /*
     * Lifecycle function called when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    /*
     * Accept the given Intent, and map it into a Change or Effect.
     */
    override fun accept(intent: Intent) {
        intentsRelay.accept(intent)
    }

    /*
     * Provide a stream of States that can be consumed by a View.
     */
    fun states(): Observable<State> {
        // When subscribed to, apply the Initial state first to the relay.
        return statesRelay.apply { accept(initialState()) }
    }

    /*
     * Provide a stream of Effects, transient actions (eg, a message to the user)
     */
    fun effects(): Observable<Effect> {
        return effectsRelay
    }

    /*
     *
     */
    private fun initialState(): State {
        return savedState.get<State?>("savedState") ?: State.Initial
    }

    /*
     * Map Intents into Changes or Effects
     */
    private fun map(intent: Intent): Observable<ChangeOrEffect> {
        return when (intent) {
            Startup -> {
                reposListModel.top100Repositories()
                    .toObservable()
                    .map<ChangeOrEffect> {
                        Change.RepositoryListLoaded(it)
                    }
                    .startWithItem(Change.Loading)
                    .onErrorReturn {
                        Change.Error(it)
                    }
            }
        }
    }

    /*
     * Reduce Changes with the previous State into a new State
     */
    private fun reduce(previousState: State, change: ChangeOrEffect): State {
        return when (change) {
            // Deliver Effects to the effectsRelay, and emit the previous State
            is Effect -> previousState.also { effectsRelay.accept(change) }
            is Change -> {
                when (previousState) {
                    State.Initial,
                    State.Loading,
                    is State.Content -> {
                        when (change) {
                            is Change.Loading -> {
                                State.Loading
                            }
                            is Change.RepositoryListLoaded -> {
                                if (change.repositoryResults.items.isEmpty()) {
                                    State.Content.Empty
                                } else {
                                    State.Content.RepositoryList(
                                        repositoryList = change.repositoryResults.items
                                    )
                                }
                            }
                            is Change.Error -> {
                                State.Error(errorMessage = change.throwable.message)
                            }
                        }
                    }
                    is State.Error -> {
                        when (change) {
                            is Change.Loading -> {
                                State.Loading
                            }
                            else -> {
                                previousState
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
 * Intents are events delivered to the ViewModel, usually initiated by the user
 */
sealed class Intent {
    object Startup: Intent()
}

/*
 * Changes are events that are reduced into States, usually triggered by an Intent
 */
sealed class ChangeOrEffect {
    sealed class Change : ChangeOrEffect() {
        object Loading: Change()
        data class Error(val throwable: Throwable): Change()
        data class RepositoryListLoaded(val repositoryResults: RepositoryResults): Change()
    }

    sealed class Effect: ChangeOrEffect() {
        // TODO: Add Effects (eg, navigation)
    }
}

/*
 * States are a 'snapshot' of the entire state the View should consume, which are reduced from Changes.
 */
sealed class State : Parcelable {
    @Parcelize
    object Initial : State()
    @Parcelize
    object Loading: State()

    sealed class Content: State() {
        @Parcelize
        data class RepositoryList(
            val repositoryList: List<Repository>
        ): Content()
        @Parcelize
        object Empty: Content()
    }

    @Parcelize
    data class Error(val errorMessage: String?): State()
}
