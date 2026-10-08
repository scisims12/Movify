package com.ivor.movify.presentation.update

import com.ivor.movify.presentation.components.CenteredListBox
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ivor.movify.R
import com.ivor.movify.data.remote.GithubReleaseDto
import com.ivor.movify.data.update.UpdateUiState
import com.ivor.movify.presentation.components.ExpressiveBackButton
import com.ivor.movify.ui.theme.ExpressiveShapes
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val REPO_URL = "https://github.com/scisims12/Movify"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateScreen(
    onBackClick: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val release = state.release()

    CenteredListBox(Modifier.fillMaxSize(), minGutter = 16.dp, maxContentWidth = 720.dp) { gutter ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                Column(modifier = Modifier.statusBarsPadding().padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveBackButton(onClick = onBackClick)
                        Box(Modifier.weight(1f))
                        if (state !is   UpdateUiState.Checking && state !is UpdateUiState.Downloading) {
                            IconButton(onClick = viewModel::checkForUpdate) {
                                Icon(Icons.Default.Refresh, contentDescription = "Check again")
                            }
                        }
                    }
                    Text(
                        text = "Updates",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .padding(top = 20.dp, start = 4.dp)
                            .semantics { heading() }
                    )
                    Text(
                        text = "You're on version ${viewModel.currentVersion}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            item(key = "status") {
                AnimatedContent(
                    targetState = state,
                    contentKey = { it::class },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "updateStatus"
                ) { current ->
                    when (current) {
                        UpdateUiState.Checking -> StatusCard(
                            tone = Tone.Neutral,
                            leading = { LoadingIndicator(modifier = Modifier.size(48.dp)) },
                            title = "Checking for updates",
                            body = "Looking at the latest release on GitHub."
                        )

                        is UpdateUiState.UpToDate -> StatusCard(
                            tone = Tone.Neutral,
                            leading = { AppMark() },
                            title = "You're up to date",
                            body = "Movify ${viewModel.currentVersion} is the latest version."
                        )

                        is UpdateUiState.NoReleaseYet -> StatusCard(
                            tone = Tone.Neutral,
                            leading = { AppMark() },
                            title = "No release published yet",
                            body = "Movify doesn't have a published GitHub release yet."
                        )

                        is UpdateUiState.Available -> StatusCard(
                            tone = Tone.Highlight,
                            leading = { StatusIcon(Icons.Default.NewReleases) },
                            badge = "NEW VERSION",
                            title = "Movify ${current.release.displayVersion()}",
                            body = listOfNotNull(
                                current.release.publishedDate()?.let { "Released $it" },
                                current.asset?.let { formatSize(it.size) }
                            ).joinToString("  ·  ")
                        ) {
                            if (current.asset != null) {
                                Button(
                                    onClick = { viewModel.download(current.release, current.asset) },
                                    shape = ExpressiveShapes.large,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Text("Download update", modifier = Modifier.padding(start = 10.dp), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // The release has no APK attached yet; the page itself still has the notes.
                                FilledTonalButton(
                                    onClick = { uriHandler.openUri(current.release.htmlUrl) },
                                    shape = ExpressiveShapes.large,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Open the release on GitHub") }
                            }
                        }

                        is UpdateUiState.Downloading -> StatusCard(
                            tone = Tone.Highlight,
                            leading = { StatusIcon(Icons.Default.Download) },
                            title = "Downloading ${current.release.displayVersion()}",
                            body = if (current.totalBytes > 0) {
                                "${(current.progress * 100).toInt()}%  ·  ${formatSize(current.downloadedBytes)} of ${formatSize(current.totalBytes)}"
                            } else {
                                "Starting download…"
                            }
                        ) {
                            LinearWavyProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
                            TextButton(onClick = viewModel::cancelDownload, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
                        }

                        is UpdateUiState.ReadyToInstall -> StatusCard(
                            tone = Tone.Highlight,
                            leading = { StatusIcon(Icons.Default.InstallMobile) },
                            title = "Ready to install",
                            body = if (viewModel.canInstall()) {
                                "Android will ask you to confirm the update. Your history, saved titles and downloads are kept."
                            } else {
                                "Allow Movify to install updates first, then come back and tap Install."
                            }
                        ) {
                            Button(
                                onClick = {
                                    if (viewModel.canInstall()) viewModel.install(current.apk)
                                    else context.startActivity(viewModel.installPermissionIntent())
                                },
                                shape = ExpressiveShapes.large,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            ) {
                                Text(if (viewModel.canInstall()) "Install" else "Allow installs", fontWeight = FontWeight.Bold)
                            }
                        }

                        is UpdateUiState.Failed -> StatusCard(
                            tone = Tone.Error,
                            leading = { StatusIcon(Icons.Default.CloudOff) },
                            title = "Something went wrong",
                            body = current.message
                        ) {
                            FilledTonalButton(onClick = viewModel::checkForUpdate, shape = ExpressiveShapes.large) { Text("Try again") }
                        }
                    }
                }
            }

            val notes = if (state is UpdateUiState.UpToDate && viewModel.currentVersion == "1.0.1") {
                MOVIFY_1_0_1_CHANGELOG
            } else {
                release?.body?.trim().orEmpty()
            }
            if (notes.isNotEmpty()) {
                item(key = "notes") {
                    Surface(
                        shape = ExpressiveShapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (state is UpdateUiState.UpToDate && viewModel.currentVersion == "1.0.1") "What's new in 1.0.1" else if (state is UpdateUiState.UpToDate) "What's in ${release?.displayVersion()}" else "What's new",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.semantics { heading() }
                            )
                            ReleaseNotes(notes)
                        }
                    }
                }
            }

            item(key = "links") {
                val links = listOfNotNull(
                    if (state is UpdateUiState.UpToDate && viewModel.currentVersion == "1.0.1") null else release?.let { Triple(Icons.Default.NewReleases, "This release on GitHub", it.htmlUrl) },
                    Triple(Icons.AutoMirrored.Filled.OpenInNew, "All releases", "$REPO_URL/releases"),
                    Triple(Icons.Default.Code, "Source code", REPO_URL)
                )
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    links.forEachIndexed { index, (icon, label, url) ->
                        SegmentedListItem(
                            onClick = { uriHandler.openUri(url) },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = links.size),
                            colors = ListItemDefaults.segmentedColors(),
                            leadingContent = { Icon(icon, contentDescription = null) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                        ) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private enum class Tone { Neutral, Highlight, Error }

@Composable
private fun StatusCard(
    tone: Tone,
    leading: @Composable () -> Unit,
    title: String,
    body: String,
    badge: String? = null,
    actions: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {}
) {
    val (container, content) = when (tone) {
        Tone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        Tone.Highlight -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Tone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = ExpressiveShapes.extraLarge,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading()
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    badge?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
            }
            if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}

@Composable
private fun StatusIcon(icon: ImageVector) {
    Surface(shape = ExpressiveShapes.large, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp)) }
    }
}

@Composable
private fun AppMark() {
    Box(modifier = Modifier.size(56.dp).clip(ExpressiveShapes.large)) {
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

/** Enough Markdown for GitHub release notes: headings, bullets, bold and inline code. */
@Composable
private fun ReleaseNotes(markdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        markdown.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Box(Modifier.size(2.dp))
                line.startsWith("#") -> Text(
                    text = inline(line.trimStart('#').trim()),
                    style = if (line.startsWith("###")) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row {
                    val indent = (line.length - line.trimStart().length) / 2
                    Text("•", modifier = Modifier.padding(start = (indent * 16).dp, end = 10.dp), color = MaterialTheme.colorScheme.primary)
                    Text(inline(line.trimStart().drop(2)), style = MaterialTheme.typography.bodyMedium)
                }
                else -> Text(inline(line), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun inline(text: String): AnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    return buildAnnotatedString {
        Regex("""\*\*(.+?)\*\*|`(.+?)`|\[(.+?)]\((.+?)\)""").let { pattern ->
            var cursor = 0
            pattern.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                val (bold, code, linkText) = Triple(match.groups[1]?.value, match.groups[2]?.value, match.groups[3]?.value)
                when {
                    bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                    code != null -> withStyle(SpanStyle(background = codeBackground, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) { append(code) }
                    linkText != null -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.Unspecified)) { append(linkText) }
                }
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
}

private fun UpdateUiState.release(): GithubReleaseDto? = when (this) {
    is UpdateUiState.UpToDate -> latest
    is UpdateUiState.Available -> release
    is UpdateUiState.Downloading -> release
    is UpdateUiState.ReadyToInstall -> release
    is UpdateUiState.Failed -> release
    UpdateUiState.Checking -> null
    UpdateUiState.NoReleaseYet -> null
}

private fun GithubReleaseDto.displayVersion(): String =
    Regex("""\d+(\.\d+)*""").find(tagName)?.value ?: tagName

private fun GithubReleaseDto.publishedDate(): String? = runCatching {
    OffsetDateTime.parse(publishedAt).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}.getOrNull()

private fun formatSize(bytes: Long): String =
    if (bytes >= 1_048_576) String.format(Locale.US, "%.1f MB", bytes / 1_048_576f) else "${bytes / 1024} KB"

private val MOVIFY_1_0_1_CHANGELOG = """
# Movify 1.0.1
The first major Movify release — redesigned, refined and built for a better movie, series and anime experience.

### HIGHLIGHTS

* **Hindi-first discovery**
  Movify now prioritizes Hindi and India-relevant movies and series on the Home screen, while keeping titles and metadata in English for a clean viewing experience.
* **Latest Hindi Movies**
  A dedicated section makes it easier to discover recent Hindi films.
* **Latest Hindi Series**
  Find newly released and popular Hindi series faster.
* **Popular in India**
  Discover movies and shows currently popular with Indian audiences.
* **Better content discovery**
  Improved Home recommendations and ranking for movies, series and anime.
* **English display metadata**
  Movie and series names, descriptions and metadata are displayed in English while Hindi content remains prioritized.

### STREAMING & PLAYBACK

* **Multi-source streaming**
  Movify searches supported source extensions to find available streams.
* **Quality selection**
  Choose from available video qualities depending on the source.
* **Subtitle support**
  Watch with available subtitle tracks from supported providers.
* **Multiple audio options**
  Use alternate audio or dubbed tracks when the selected source provides them.
* **Picture-in-Picture**
  Continue watching in a floating player when leaving the app.
* **Mini-player experience**
  Playback can continue while navigating through supported parts of the app.
* **Continue Watching**
  Resume movies and episodes from where you stopped.
* **Automatic progress tracking**
  Movify remembers your playback position automatically.

### DOWNLOADS

* **Offline playback**
  Download supported movies or episodes and watch them later.
* **Background downloads**
  Downloads can continue using Android's download service.
* **Download management**
  View and manage downloaded content from the Downloads section.
* **Retry and resume**
  Supported downloads can recover from interruptions.

### DISCOVERY

* **Trending content**
  Quickly discover movies and shows gaining popularity.
* **Top 10 this week**
  See popular titles in an easy-to-browse weekly ranking.
* **New episodes this week**
  Find recently released TV episodes.
* **Search**
  Search movies, TV series and anime with fast TMDB-powered results.
* **Detailed title pages**
  View descriptions, cast, seasons, episodes, trailers and related titles.

### LIBRARY

* **Saved titles**
  Keep movies and series you want to return to later.
* **Watch history**
  Quickly revisit previously watched content.
* **Continue Watching**
  Your active titles stay easy to access.
* **Local storage**
  History and progress are stored locally on the device.

### DESIGN

* **Movify branding**
  The application has been fully rebranded as Movify.
* **New application identity**
  Package, namespace and application ID now use: `com.ivor.movify`
* **Material 3 interface**
  Modern Android design with smooth cards, navigation and responsive layouts.
* **Improved Home layout**
  Home sections have been reorganized for easier discovery.
* **Phone and tablet support**
  Layouts adapt to different screen sizes where supported.

### TECHNICAL IMPROVEMENTS

* **TMDB integration**
  Movie, TV and anime metadata is powered by TMDB.
* **Real TMDB API support**
  The application now correctly loads metadata using the configured TMDB API key.
* **Extension-based sources**
  Streaming providers are handled through Movify's extension system.
* **Media3 playback**
  Playback uses Android Media3 / ExoPlayer.
* **Local Room database**
  Watch progress, history and library data are stored locally.
* **Improved networking**
  Retrofit and OkHttp handle metadata and network requests.
* **Background services**
  Downloads and media-related tasks use Android background services where required.
* **Improved error handling**
  Catalog and network failures are handled more clearly.

### APP EXPERIENCE

* **Faster navigation**
  Move easily between Home, Search, Saved, Downloads and History.
* **Cleaner details screens**
  Title information is organized more clearly.
* **Better Hindi content discovery**
  India-focused content is easier to find without changing the UI language.
* **English interface**
  Movify's interface and title metadata remain in English.

### FIRST RELEASE

Movify 1.0.1 marks the beginning of the Movify project.

This release focuses on:
- discovering movies, series and anime
- Hindi-first recommendations
- English metadata display
- streaming through supported extensions
- native Android playback
- offline downloads
- local watch history and progress
- modern Material 3 design
""".trimIndent()
