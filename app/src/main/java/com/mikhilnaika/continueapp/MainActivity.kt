package com.mikhilnaika.continueapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mikhilnaika.continueapp.core.ads.ConsentManager
import com.mikhilnaika.continueapp.core.audio.ArcadeAudio
import com.mikhilnaika.continueapp.core.audio.LocalArcadeAudio
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import com.mikhilnaika.continueapp.core.design.ContinueTheme
import com.mikhilnaika.continueapp.core.design.enableArcadeEdgeToEdge
import com.mikhilnaika.continueapp.core.ui.LocalHaptics
import com.mikhilnaika.continueapp.core.util.Haptics
import com.mikhilnaika.continueapp.navigation.ContinueNavHost
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _onboardingComplete = MutableStateFlow<Boolean?>(null)
    val onboardingComplete: StateFlow<Boolean?> = _onboardingComplete

    init {
        viewModelScope.launch {
            userPreferencesRepository.isOnboardingComplete.collect { complete ->
                _onboardingComplete.value = complete
            }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Injected rather than constructed per screen, so the HAPTICS setting in YOU actually
     * reaches the vibrator — see [Haptics] for what was wrong before.
     */
    @Inject
    lateinit var haptics: Haptics

    /** Same arrangement as [haptics]: the SOUND and MUSIC toggles live inside it. */
    @Inject
    lateinit var audio: ArcadeAudio

    /**
     * Google requires the consent status to be refreshed on **every** launch, not once — it
     * can change server-side when a vendor list or policy changes, so a one-time check would
     * drift out of compliance silently.
     */
    @Inject
    lateinit var consentManager: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableArcadeEdgeToEdge()

        // Fired before setContent and deliberately not awaited. Outside the EEA/UK/CH this
        // shows nothing at all; inside it, Google's form appears over the app once. Either
        // way the pile must not wait on it — a compliance check has no business sitting on
        // the cold-start path (docs/05-TECH-ARCHITECTURE.md: cold start < 1.5s), and every
        // part of the app except the ad surfaces works regardless of the answer.
        consentManager.requestConsentInfo(this)

        // One-shot: consumed once, and not re-applied after a rotation recreates the Activity.
        var openFriendId: Long? = if (savedInstanceState == null && intent.hasExtra(EXTRA_OPEN_FRIEND_ID)) {
            intent.getLongExtra(EXTRA_OPEN_FRIEND_ID, 0L)
        } else {
            null
        }

        setContent {
            val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
            onboardingComplete?.let { complete ->
                ContinueTheme {
                    CompositionLocalProvider(LocalHaptics provides haptics, LocalArcadeAudio provides audio) {
                        ContinueNavHost(
                            onboardingComplete = complete,
                            openFriendId = openFriendId,
                            onOpenFriendHandled = { openFriendId = null },
                        )
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Set by the friend-link sheet. Only ever carries a local database row id, and is only
         * used to pick a screen — an app firing it at us can do nothing but open FRIENDS.
         */
        const val EXTRA_OPEN_FRIEND_ID = "com.mikhilnaika.continueapp.OPEN_FRIEND_ID"
    }
}
