package com.drivingschoolrwandaapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

/**
 * Unit tests for [PaymentUtils].
 *
 * The util is heavily Android-framework coupled (USSD dial intents, toasts),
 * so the framework statics are mocked with [mockStatic]:
 *  - [ContextCompat.checkSelfPermission] — drives granted / denied branches (kept
 *    for backward compatibility with existing test helpers),
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
    private var contextCompatMock: org.mockito.MockedStatic<ContextCompat>? = null
    private var toastMock: org.mockito.MockedStatic<Toast>? = null
    private var uriMock: org.mockito.MockedStatic<Uri>? = null
    private var intentConstruction: org.mockito.MockedConstruction<Intent>? = null

    @Before
    fun setUp() {
        fragment = mock(Fragment::class.java)
        activity = mock(Activity::class.java)
        // Fragment mocks return null for requireContext()/getContext()/getString() by
        // default, which breaks `any(Class)` matchers (they never match null). Give
        // them real values so the Toast stub can match.
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
        contextCompatMock = org.mockito.Mockito.mockStatic(ContextCompat::class.java)
        `when`(ContextCompat.checkSelfPermission(any(Context::class.java), any(String::class.java)))
            .thenReturn(result)
    }

    private fun stubToast(): Toast {
        val toast = mock(Toast::class.java)
        toastMock = org.mockito.Mockito.mockStatic(Toast::class.java)
        `when`(Toast.makeText(any(Context::class.java), any(CharSequence::class.java), anyInt()))
            .thenReturn(toast)
        return toast
    }

    private fun stubDialFramework() {
        uriMock = org.mockito.Mockito.mockStatic(Uri::class.java)
        `when`(Uri.parse(any(String::class.java))).thenReturn(mock(Uri::class.java))
        `when`(Uri.encode(any(String::class.java))).thenReturn("encoded")
        intentConstruction = org.mockito.Mockito.mockConstruction(Intent::class.java) { mockIntent, _ ->
            `when`(mockIntent.action).thenReturn(Intent.ACTION_DIAL)
            `when`(mockIntent.dataString).thenReturn("*182*8*1*644209*500#")
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
    // setupPaymentMethods — listener wiring
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
    fun `clicking a card opens the dialer with explicit intent`() {
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
        assertEquals(Intent.ACTION_DIAL, intentCaptor.value.action)
        assertEquals("*182*8*1*644209*500#", Uri.decode(intentCaptor.value.dataString))
    }

    @Test
    fun `clicking a card with permission granted opens the dialer`() {
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
        assertEquals(Intent.ACTION_DIAL, intentCaptor.value.action)
    }

    // ---------------------------------------------------------------------------
    // Click → dialUssd (Activity overload)
    // ---------------------------------------------------------------------------

    @Test
    fun `clicking a card from activity opens the dialer when permission is already granted`() {
        val rootView = mock(View::class.java)
        val card = mockCard()
        stubCardLookup(rootView, card)
        stubPermission(PackageManager.PERMISSION_GRANTED)
        stubDialFramework()

        PaymentUtils.setupPaymentMethods(rootView, activity, "500")
        val listenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(card).setOnClickListener(listenerCaptor.capture())
        listenerCaptor.value.onClick(card)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(activity).startActivity(intentCaptor.capture())
        assertEquals(Intent.ACTION_DIAL, intentCaptor.value.action)
    }

    // ---------------------------------------------------------------------------
    // onRequestPermissionsResult — both overloads now ignore legacy results
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
    fun `onRequestPermissionsResult fragment overload with unrelated code shows no toast`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            456,
            intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        verify(toast, never()).show()
        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult fragment overload with unrelated code is ignored`() {
        val toast = stubToast()
        setUssdToCall(null)

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            999,
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(toast, never()).show()
        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult fragment overload with granted permission is ignored`() {
        setUssdToCall("*182*1*1*0785460748*500#")

        PaymentUtils.onRequestPermissionsResult(
            fragment,
            456,
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult activity overload with unrelated code shows no toast`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            activity,
            456,
            arrayOf(android.Manifest.permission.CALL_PHONE),
            intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        verify(toast, never()).show()
        verify(activity, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult activity overload with granted permission is ignored`() {
        setUssdToCall("*182*8*1*644209*500#")

        PaymentUtils.onRequestPermissionsResult(
            activity,
            456,
            arrayOf(android.Manifest.permission.CALL_PHONE),
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(activity, never()).startActivity(any())
    }

    @Test
    fun `onRequestPermissionsResult activity overload ignores other request codes`() {
        val toast = stubToast()

        PaymentUtils.onRequestPermissionsResult(
            activity,
            777,
            arrayOf(android.Manifest.permission.CALL_PHONE),
            intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        verify(toast, never()).show()
        verify(activity, never()).startActivity(any())
    }
}
