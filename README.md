# Top Github Repos
## by Steve Caires (steve.caires@gmail.com)

An Android sample app displaying the top 100 starred GitHub repositories

### Build Tools & Versions Used

Android Studio Arctic Fox | 2020.3.1 Beta 1

Android gradle plugin version: 7.0.0-beta1

Gradle version: 7.0

Kotlin gradle plugin: 1.5.0

Requires Android 23+, tested on Android 30 emulator, and a Pixel 3a.

### Usage

The app can be built and deployed using the above versions of android studio and kotlin/gradle.

### Architectural Overview

My focus area for this app was to demonstrate a clean Model-View-Intent architecture for an Android app. I became a big fan of MVI and unidirectional data flow recently, as it (in my opinion) makes testing more straightforward and helps eliminate bugs that can occur in other architecture patterns (like MVP) caused by unanticipated interactions with the view, especially for complex screens. The MVI implementation here is a simplified representation of the MVI pattern.

The "stack" used in this app is :
* RxJava3 and RxRelay for reactive programming "glue"
* OkHttp and Retrofit for service code generation
* Moshi for marshaling JSON into kotlin data classes
* Hilt (and its underlying dagger2) for dependency injection
* Android Navigation for ViewModel support, and future navigation to a detail fragment or other destinations
* ConstraintLayout for responsive (basic) layouts

The app is a single activity, with Android Navigation used to initialize a nav graph, although there is only one destination (the list of repositories). A ViewModel is scoped to the list's destination on the navgraph (for simplicity), which together with the fragment, follows the unidirectional data flow Model-View-Intent pattern. The fragments send Intents (eg, to refresh a list or fetch a repository's top contributor), representing user actions (not to be confused with the Android Intent), to the ViewModel, which maps them into Changes (eg, calling the ReposListModel for a representation of a Repository or list of Contributors), which reduces them into States (eg, a State with a list of Repositories). The view (fragment, in this case) subscribes to a stream of States, each of which is the complete state needed to render the view. The user can perform actions on the rendered view (like requesting a top contributor, which happens as those list items are bound to the RecyclerView adapter) which sends an Intent to the ViewModel, and the cycle begins again.

The API layer is provided to the ViewModel via dependency injection. It consists of the ReposListModel, which wraps the RepositoryService and SearchService, both of which have their implementation generated by Retrofit. Because the top contributor for each repository is fetched via a separate request, the ReposListModel caches the top contributor for each repository.

This app is primarily for a phone layout, displaying a vertically scrolling list of the repositories. It displays on a tablet layout, but isn't intended for one (those are some wide RecyclerView cells!)
