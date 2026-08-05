package com.drivingschoolrwandaapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

/**
 * Unit tests for [NetworkUtils].
 *
 * [NetworkUtils.isNetworkAvailable] resolves the [ConnectivityManager] from the
 * [Context] and reports whether the active network is connected. These tests
 * cover every branch: missing service, missing active network, disconnected,
 * and connected.
 */
class NetworkUtilsTest {

    @Test
    fun `isNetworkAvailable true when active network is connected`() {
        val context = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val networkInfo = mock(NetworkInfo::class.java)
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        `when`(networkInfo.isConnected).thenReturn(true)

        assertTrue(NetworkUtils.isNetworkAvailable(context))
    }

    @Test
    fun `isNetworkAvailable false when active network is not connected`() {
        val context = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        val networkInfo = mock(NetworkInfo::class.java)
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        `when`(networkInfo.isConnected).thenReturn(false)

        assertFalse(NetworkUtils.isNetworkAvailable(context))
    }

    @Test
    fun `isNetworkAvailable false when no active network`() {
        val context = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        `when`(connectivityManager.activeNetworkInfo).thenReturn(null)

        assertFalse(NetworkUtils.isNetworkAvailable(context))
    }

    @Test
    fun `isNetworkAvailable false when connectivity service is unavailable`() {
        val context = mock(Context::class.java)
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null)

        assertFalse(NetworkUtils.isNetworkAvailable(context))
    }
}
