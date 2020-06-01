package com.example.jetnews.ui

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.ui.JetnewsWorkflow.Rendering
import com.example.jetnews.ui.JetnewsWorkflow.State
import com.example.jetnews.ui.interests.InterestsWorkflow
import com.squareup.workflow.*
import com.squareup.workflow.ui.backstack.BackStackScreen
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JetnewsWorkflow(
    private val postsRepository: PostsRepository,
    private val interestsWorkflow: InterestsWorkflow
) : StatefulWorkflow<Unit, State, Nothing, BackStackScreen<Rendering>>() {

    data class State(
        val currentScreen: Screen = Screen.Home,
        val isLoading: Boolean = true,
        val posts: List<Post>? = null,
        val favorites: List<String> = emptyList(),
        val selectedTopics: List<String> = emptyList()
    )

    data class Rendering(
        val title: String,
        val drawerRendering: DrawerRendering? = null,
        val bodyScreen: Any
    )

    data class DrawerRendering(
        val items: List<String>, // TODO icons
        val onSelected: (index: Int) -> Unit
    )

    object LoadingRendering

    data class HomeRendering(
        val posts: List<Post>
    )

    override fun initialState(props: Unit, snapshot: Snapshot?): State = State()

    override fun render(
        props: Unit,
        state: State,
        context: RenderContext<State, Nothing>
    ): BackStackScreen<Rendering> {
        val drawer = if (!state.isLoading && state.currentScreen !is Screen.Article) {
            DrawerRendering(
                items = listOf("Home", "Interests"),
                onSelected = { TODO() }
            )
        } else null

        if (state.isLoading || state.posts == null) {
            context.runningWorker(postsWorker(), key = "load posts") { result ->
                action {
                    nextState = nextState.copy(
                        posts = (result as Result.Success).data,
                        isLoading = false
                    )
                }
            }
        }

        return when (state.currentScreen) {
            Screen.Home -> BackStackScreen(
                Rendering(
                    title = "Jetnews",
                    drawerRendering = drawer,
                    bodyScreen = state.posts?.let { HomeRendering(it) } ?: LoadingRendering
                )
            )
            Screen.Interests -> BackStackScreen(
                Rendering(
                    title = "Interests",
                    drawerRendering = drawer,
                    bodyScreen = context.renderChild(interestsWorkflow)
                )
            )
            is Screen.Article -> BackStackScreen(
                Rendering(
                    title = "TODO",
                    bodyScreen = TODO()
                )
            )
        }
    }

    override fun snapshotState(state: State): Snapshot = Snapshot.EMPTY

    private fun postsWorker(): Worker<Result<List<Post>>> = Worker.from {
        suspendCoroutine { continuation ->
            postsRepository.getPosts(continuation::resume)
        }
    }
}
