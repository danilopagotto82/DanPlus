package com.nuvio.tv.ui.screens.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Informação editorial opcional; NÃO representa uma fonte de vídeo. */
internal object OrdemCertaClient {
    private const val API = "https://ordem-certa-danplus.danilopagotto2013.workers.dev/v1/context"
    private val imdb = Regex("^tt[0-9]+$")
    data class Target(val id: String, val type: String, val name: String,
                      val season: Int? = null, val episode: Int? = null)
    data class Guide(val universe: String, val position: Int, val total: Int,
                     val isExcerpt: Boolean, val exactness: String,
                     val previous: Target?, val next: Target?,
                     val interruption: Target?, val instruction: String?,
                     val future: Target?, val warning: String?)

    fun parse(value: String): Guide? {
        val root = JSONObject(value)
        if (root.optString("status") != "ok") return null
        val matches = root.optJSONArray("options") ?: return null
        val option = (0 until matches.length())
            .mapNotNull { matches.optJSONObject(it) }
            .firstOrNull { it.optString("status") == "ok" } ?: return null
        val previous = target(option.optJSONObject("previous"))
        val next = target(option.optJSONObject("next"))
        val interruption = option.optJSONObject("interruption")
        val future = option.optJSONArray("future")?.optJSONObject(0)
        return Guide(
            universe = option.optString("universeName").take(70),
            position = option.optInt("position"),
            total = option.optInt("total"),
            isExcerpt = option.optBoolean("isExcerpt"),
            exactness = option.optString("exactness"),
            previous = previous, next = next,
            interruption = target(interruption?.optJSONObject("media")),
            instruction = interruption?.optString("message"),
            future = target(future),
            warning = option.optString("warning").takeIf { it.isNotBlank() }
        )
    }
    private fun target(o: JSONObject?): Target? {
        if (o == null) return null
        val id = o.optString("id")
        val type = o.optString("type")
        if (!imdb.matches(id) || type !in setOf("movie", "series")) return null
        val season = o.optInt("season").takeIf { it > 0 }
        val episode = o.optInt("episode").takeIf { it > 0 }
        return Target(id, type, o.optString("name").take(100), season, episode)
    }

    suspend fun fetch(type: String, rawId: String, season: Int?, episode: Int?): Guide? =
        withContext(Dispatchers.IO) {
            val id = rawId.substringBefore(':')
            if (!imdb.matches(id) || type !in setOf("movie", "series")) return@withContext null
            val query = buildString {
                append("?type=").append(type)
                append("&id=").append(URLEncoder.encode(id, "UTF-8"))
                if (type == "series" && season != null && season > 0) {
                    append("&season=").append(season)
                    if (episode != null && episode > 0) append("&episode=").append(episode)
                }
            }
            runCatching {
                (URL(API + query).openConnection() as HttpURLConnection).run {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                    try {
                        if (responseCode != 200) return@run null
                        val text = inputStream.bufferedReader().use { it.readText() }
                        if (text.length > 250_000) return@run null
                        parse(text)
                    } finally { disconnect() }
                }
            }.getOrNull()
        }
}

@Composable
internal fun OrdemCertaGuideCard(
    contentType: String,
    contentId: String,
    season: Int?,
    episode: Int?,
    onNavigate: (type: String, imdbId: String, season: Int?, episode: Int?) -> Unit
) {
    var guide by remember(contentType, contentId, season, episode) {
        mutableStateOf<OrdemCertaClient.Guide?>(null)
    }
    LaunchedEffect(contentType, contentId, season, episode) {
        guide = OrdemCertaClient.fetch(contentType, contentId, season, episode)
    }
    val info = guide ?: return
    val gold = Color(0xFFDCC17D)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE91A1B20), shape)
            .border(1.dp, Color(0xFF6D5B37), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "ORDEM CERTA  •  ${info.universe}  •  " +
                   (if (info.isExcerpt) "trecho ${info.position}/${info.total}" else "${info.position}/${info.total}"),
            color = gold, style = MaterialTheme.typography.labelMedium, maxLines = 1
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            info.previous?.let { previous ->
                Button(onClick = { onNavigate(previous.type, previous.id, previous.season, previous.episode) },
                       modifier = Modifier.weight(1f)) {
                    Text("◀  ${previous.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            info.next?.let { next ->
                Button(onClick = { onNavigate(next.type, next.id, next.season, next.episode) },
                       modifier = Modifier.weight(1f)) {
                    Text("${next.name}  ▶", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        info.interruption?.let { nextMovie ->
            Button(onClick = { onNavigate(nextMovie.type, nextMovie.id, nextMovie.season, nextMovie.episode) }) {
                Text((info.instruction ?: "Próximo título") + "  ${nextMovie.name}  •  Ver detalhes",
                     maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        info.future?.let { future ->
            Button(onClick = { onNavigate(future.type, future.id, future.season, future.episode) }) {
                Text("PREVISTO — posição provisória: ${future.name}  •  Ver detalhes",
                     maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (info.exactness == "season") {
            Text("Ordem por temporada. Este trecho não indica episódios exatos.",
                 color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
        }
    }
}
