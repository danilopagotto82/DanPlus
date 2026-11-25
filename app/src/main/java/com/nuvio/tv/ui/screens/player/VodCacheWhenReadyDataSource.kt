package com.nuvio.tv.ui.screens.player

import android.net.Uri
import androidx.media3.common.ByteBufferDataReader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.nio.ByteBuffer

/** An open keeps its chosen source until close, even if disk becomes ready mid-read. */
@UnstableApi
internal class VodCacheWhenReadyDataSource(
    private val upstream: DataSource.Factory,
    private val readyCache: () -> DataSource?
) : DataSource, ByteBufferDataReader {
    private val listeners = LinkedHashSet<TransferListener>()
    private var source: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        check(source == null)
        val selected = readyCache() ?: upstream.createDataSource()
        source = selected
        listeners.forEach(selected::addTransferListener)
        return selected.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(source).read(buffer, offset, length)

    override fun supportsByteBufferRead(): Boolean =
        (source as? ByteBufferDataReader)?.supportsByteBufferRead() == true

    override fun read(buffer: ByteBuffer, length: Int): Int =
        (source as ByteBufferDataReader).read(buffer, length)

    override fun getUri(): Uri? = source?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = source?.responseHeaders.orEmpty()

    override fun addTransferListener(transferListener: TransferListener) {
        if (listeners.add(transferListener)) source?.addTransferListener(transferListener)
    }

    override fun close() {
        val selected = source
        source = null
        selected?.close()
    }
}
