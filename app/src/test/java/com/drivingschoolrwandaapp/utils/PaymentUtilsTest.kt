package com.drivingschoolrwandaapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Unit tests for [PaymentUtils].
 *
 * The util is heavily Android-framework coupled (permission checks, USSD dial
 * intents, toasts), so the framework statics are mocked with [mockStatic]:
 *  - [ContextCompat.checkSelfPermission] — drives granted / denied branches,
 *  - [ActivityCompat.requestPermissions] / [Fragment.requestPermissions] — the
 *    permission-request side effects,
 *  - [Toast.makeText] — the denial toast,
 *  - [android.net.Uri] and `new [Intent](...)` ([mockConstruction]) — the USSD
 *    dial, whose real constructors throw "not mocked" in JVM unit tests.
 *
 * The private static `ussdToCall` holder is reset via reflection in [setUp] so
 * tests are independent of execution order.
 */
class PaymentUtilsTest {

    private lateinit var fragment: Fragment
    private lateinit var activity: Activity
    private var contextCompatMock: MockedStatic<ContextCompat>? = null
    private var activityCompatMock: MockedStatic<ActivityCompat>? = null
    private var toastMock: MockedStatic<Toast>? = null
    private var uriMock: MockedStatic<Uri>? = null
    private var intentConstruction: MockedConstruction<Intent>? = null

    @Before
    fun setUp() {
        fragment = mock(Fragment::class.java)
        activity = mock(Activity::class.java)
        // Fragment mocks return null for requireContext()/getContext()/getString() by
        // default, which breaks `any(Class)` matchers (they never match null). Give
        // them real values so the permission and Toast stubs can match.
        val context = mock(Context::class.java)
        `when`(fragment.requireContext()).thenReturn(context)
        `when`(fragment.getContext()).thenReturn(context)
        `when`(fragment.getString(anyInt())).thenReturn("Permission denied")
        `when`(activity.getString(anyInt())).thenReturn("Permission denied")
        resetUssdToCall()
    }

    @After
    fun tearDown() {
        contextCompatMock?.close()
        activityCompatMock?.close()
        toastMock?.close()
        uriMock?.close()
        intentConstruction?.close()
        resetUssdToCall()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun resetUssdToCall() {
        try {
            val field = PaymentUtils::class.java.getDeclaredField("ussdToCall")
            field.isAccessible = true
            field.set(null, null)
        } catch (_: Exception) {
            // Static field may already be reset; never fail the test setup.
        }
    }

    private fun setUssdToCall(value: String?) {
        val field = PaymentUtils::class.java.getDeclaredField("ussdToCall")
        field.isAccessible = true
        field.set(null, value)
    }

    private fun stubPermission(result: Int) {
        contextCompatMock = mockStatic(ContextCompat::class.java)
        `when`(ContextCompat.checkSelfPermission(any(Context::class.java), any(String::class.java)))
            .thenReturn(result)
    }

    private fun stubToast(): Toast {
        val toast = mock(Toast::class.java)
        toastMock = mockStatic(Toast::class.java)
        `when`(Toast.makeText(any(Context::class.java), any(CharSequence::class.java), anyInt()))
            .thenReturn(toast)
        return toast
    }

    private fun stubDialFramework() {
        uriMock = mockStatic(Uri::class.java)
        `when`(Uri.parse(any(String::class.java))).thenReturn(mock(Uri::class.java))
        `when`(Uri.encode(any(String::class.java))).thenReturn("encoded")
        intentConstruction = mockConstruction(Intent::class.java) { mockIntent, _ ->
            // The production code always dials with ACTION_CALL; expose it on the mock
            // so tests can assert the intent action (a plain mock returns null).
            `when`(mockIntent.action).thenReturn("android.intent.action.CALL")
        }
    }

    private fun mockCard(): MaterialCardView = mock(MaterialCardView::class.java)

    private fun stubCardLookup(rootView: View, vararg cards: MaterialCardView) {
        val ids = intArrayOf(
            com.drivingschoolrwandaapp.R.id.payment_method_1,
            com.drivingschoolrwandaapp.R.id.payment_method_2,
            com.drivingschoolrwandaapp.R.id.payment_method_3
        )
        cards.forEachIndexed { index, card ->
            `when`(rootView.findViewById<View>(ids[index])).thenReturn(card)
        }
    }

    // ---------------------------------------------------------------------------
    // setupPaymentMethods — listener wiring (Fragment overload)
    // ---------------------------------------------------------------------------

    @Test
    fun `setupPaymentMethods wires a click listener on each card`() {
        val rootView = mock(View::class.java)
        val card1 = mockCard()
        val card2 = mockCard()
        val card3 = mockCard()
        stubCardLookup(rootView, card1, card2, card3)

        PaymentUtils.setupPaymentMethods(rootView, fragment, "500")

        verify(card1).setOnClickListener(any())
        verify(card2).setOnClickListener(any())
        verify(card3).setOnClickListener(any())
    }

    @Test
    fun `setupPaymentMethods does not crash when cards are missing`() {
        // Unstubbed findViewById → all three cards are null.
        val rootView = mock(View::class.java)

        PaymentUtils.setupPaymentMethods(rootView, fragment, "500")
        // Reaching here without exception is the assertion.
        PaymentUtils.setupPaymentMethods(rootView, activity, "500")
    }

    // ---------------------------------------------------------------------------
    // Click → dialUssd (Fragment overload)
    // ---------------------------------------------------------------------------

    @Test
    @Suppress("DEPRECATION") // The production dialUssd uses the deprecated Fragment.requestPermissions overload
    fun `clicking a card without permission requests CALL_PHONE`() {
        val rootView = mock(View::class.java)
        val card = mockCard()
        stubCardLookup(rootView, card)
        stubPermission(PackageManager.PERMISSION_DENIED)

        PaymentUtils.setupPaymentMethods(rootView, fragment, "500")
        val listenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(card).setOnClickListener(listenerCaptor.capture())
        listenerCaptor.value.onClick(card)

        val permissionsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
        val requestCodeCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify(fragment).requestPermissions(permissionsCaptor.capture(), requestCodeCaptor.capture())
        assertEquals(1, permissionsCaptor.value.size)
        assertEquals(Manifest.permission.CALL_PHONE, permissionsCaptor.value[0])
        assertEquals(456, requestCodeCaptor.value)
    }

    @Test
    fun `clicking a card with permission granted starts the USSD call`() {
        val rootView = mock(View::class.java)
        val card = mockCard()
        stubCardLookup(rootView, card)
        stubPermission(PackageManager.PERMISSION_GRANTED)
        stubDialFramework()

        PaymentUtils.setupPaymentMethods(rootView, fragment, "500")
        val listenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(card).setOnClickListener(listenerCaptor.capture())
        listenerCaptor.value.onClick(card)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(fragment).startActivity(intentCaptor.capture())
        assertEquals("android.intent.action.CALL", intentCaptor.value.action)
    }

    // ---------------------------------------------------------------------------
    // Click → dialUssd (Activity overload)
    // ---------------------------------------------------------------------------

    @Test
    fun `clicking a card from activity without permission requests via ActivityCompat`() {
        val rootView = mock(View::class.java)
        val card = mockCard()
        stubCardLookup(rootView, card)
        stubPermission(PackageManager.PERMISSION_DENIED)
        activityCompatMock = mockStatic(ActivityCompat::class.java)

        PaymentUtils.setupPaymentMethods(rootView, activity, "500")
        val listenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(card).setOnClickListener(listenerCaptor.capture())
        listenerCaptor.value.onClick(card)

        val permissionsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
        activityCompatMock!!.verify { ActivityCompat.requestPermissions(any(), permissionsCaptor.capture(), org.mockito.Mockito.eq(456)) }
        assertEquals(1, permissionsCaptor.value.size)
        assertEquals(Manifest.permission.CALL_PHONE, permissionsCaptor.value[0])
    }

    // ---------------------------------------------------------------------------
    // onRequestPermissionsResult — Fragment overload
    // ---------------------------------------------------------------------------

    @Test
    fun `onRequestPermissionsResult ignores unrelated request codes`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            999,
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(toast, never()).show()
        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult with denied result shows permission toast`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            456,
            intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        verify(toast).show()
        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult with granted result and no pending ussd does nothing`() {
        val toast = stubToast()
        setUssdToCall(null)

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            456,
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(toast, never()).show()
        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult with granted result dials the pending ussd`() {
        stubPermission(PackageManager.PERMISSION_GRANTED)
        stubDialFramework()
        setUssdToCall("*182*1*1*0785460748*500#")

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            456,
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(fragment).startActivity(intentCaptor.capture())
        assertEquals("android.intent.action.CALL", intentCaptor.value.action)
    }

    // ---------------------------------------------------------------------------
    // onRequestPermissionsResult — Activity overload
    // ---------------------------------------------------------------------------

    @Test
    fun `onRequestPermissionsResult activity overload shows toast when denied`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            activity,
            456,
            arrayOf(Manifest.permission.CALL_PHONE),
            intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        verify(toast).show()
        verify(activity, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult activity overload dials when granted`() {
        stubPermission(PackageManager.PERMISSION_GRANTED)
        stubDialFramework()
        setUssdToCall("*182*8*1*644209*500#")

        PaymentUtils.onRequestPermissionsResult(
            activity,
            456,
            arrayOf(Manifest.permission.CALL_PHONE),
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(activity).startActivity(intentCaptor.capture())
        assertEquals("android.intent.action.CALL", intentCaptor.value.action)
    }

    @Test
    fun `onRequestPermissionsResult activity overload ignores other request codes`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            activity,
            777,
            arrayOf(Manifest.permission.CALL_PHONE),
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(toast, never()).show()
        verify(activity, never()).startActivity(any())
    }
}
