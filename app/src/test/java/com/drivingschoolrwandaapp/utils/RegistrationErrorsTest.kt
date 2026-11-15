package com.drivingschoolrwandaapp.utils

import org.junit.Assert.*
import org.junit.Test

class RegistrationErrorsTest {
    @Test fun `device conflicts must not claim the phone is registered`() {
        assertFalse(RegistrationErrors.isExistingAccount("This device is already registered"))
        assertFalse(RegistrationErrors.isExistingAccount("Account already exists on this device"))
    }
    @Test fun `missing accounts and unrelated errors are not duplicate accounts`() {
        assertFalse(RegistrationErrors.isExistingAccount("No registered account found"))
        assertFalse(RegistrationErrors.isExistingAccount("Request already processing"))
        assertFalse(RegistrationErrors.isExistingAccount(null))
    }
    @Test fun `explicit phone conflicts can offer login`() {
        assertTrue(RegistrationErrors.isExistingAccount("Phone number already registered"))
        assertTrue(RegistrationErrors.isExistingAccount("Account already exists"))
        assertTrue(RegistrationErrors.isExistingAccount("Phone is already in use"))
    }
}
