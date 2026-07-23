package com.circuitflip.flipledger

import com.circuitflip.flipledger.domain.model.AcquisitionSource
import com.circuitflip.flipledger.domain.model.AuthDraft
import com.circuitflip.flipledger.domain.model.CostDraft
import com.circuitflip.flipledger.domain.model.CostType
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceCategory
import com.circuitflip.flipledger.domain.model.DeviceDraft
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.model.SaleDraft
import com.circuitflip.flipledger.domain.model.SalesChannel
import com.circuitflip.flipledger.domain.model.WorkspaceType
import com.circuitflip.flipledger.domain.util.FormValidation
import com.circuitflip.flipledger.domain.util.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormValidationTest {

    @Test
    fun signUpRequiresIdentityAndStrongPassword() {
        val errors = FormValidation.auth(AuthDraft(), isSignUp = true)

        assertTrue("name" in errors)
        assertTrue("email" in errors)
        assertTrue("password" in errors)

        val weakPassword = FormValidation.auth(
            AuthDraft(name = "Jordan Rivera", email = "jordan@example.com", password = "onlyletters"),
            isSignUp = true,
        )
        assertEquals("Password must include at least one number.", weakPassword["password"])
    }

    @Test
    fun signInAcceptsExistingPasswordCompositionButRejectsMissingFields() {
        assertTrue(
            FormValidation.auth(
                AuthDraft(email = "jordan@example.com", password = "abcdefgh"),
                isSignUp = false,
            ).isEmpty(),
        )
        val errors = FormValidation.auth(AuthDraft(email = "not-an-email"), isSignUp = false)
        assertTrue("email" in errors)
        assertTrue("password" in errors)
    }

    @Test
    fun optionalPhoneIsValidatedWhenEntered() {
        val errors = FormValidation.auth(
            AuthDraft(
                name = "Jordan Rivera",
                email = "jordan@example.com",
                password = "password1",
                phone = "call-me",
            ),
            isSignUp = true,
        )

        assertTrue("phone" in errors)
        assertTrue(
            FormValidation.auth(
                AuthDraft(
                    name = "Jordan Rivera",
                    email = "jordan@example.com",
                    password = "password1",
                    phone = "+1 (555) 123-4567",
                ),
                isSignUp = true,
            ).isEmpty(),
        )
    }

    @Test
    fun setupValidatesBusinessAndPartnerFields() {
        assertTrue("businessName" in FormValidation.setupBusinessName(" "))
        assertTrue(
            "partnerName" in FormValidation.setupPreferences(
                WorkspaceType.PARTNER,
                partnerName = "",
                splitYou = 60,
                categoryPref = "mixed",
            ),
        )
        assertTrue(
            FormValidation.setupPreferences(
                WorkspaceType.SOLO,
                partnerName = "",
                splitYou = 60,
                categoryPref = "phones",
            ).isEmpty(),
        )
    }

    @Test
    fun deviceValidationCoversEveryWizardStep() {
        val empty = FormValidation.device(DeviceDraft(date = "invalid"))
        assertTrue("category" in empty)
        assertTrue("model" in empty)
        assertTrue("price" in empty)
        assertTrue("date" in empty)
        assertTrue("source" in empty)

        val detailErrors = FormValidation.deviceStep3(
            validDeviceDraft().copy(
                identifierLast4 = "12A",
                storage = "x".repeat(FormValidation.MAX_STORAGE_LENGTH + 1),
            ),
        )
        assertTrue("identifier" in detailErrors)
        assertTrue("storage" in detailErrors)
        assertTrue(FormValidation.device(validDeviceDraft()).isEmpty())
    }

    @Test
    fun deviceRejectsFutureDateAndExcessivePrice() {
        val errors = FormValidation.deviceStep2(
            validDeviceDraft().copy(
                price = "1000000000",
                date = "2999-01-01",
            ),
        )

        assertTrue("price" in errors)
        assertTrue("date" in errors)
    }

    @Test
    fun costValidatesTypeAmountDatesAndNote() {
        val errors = FormValidation.cost(
            CostDraft(
                type = null,
                amount = "0",
                date = "2023-12-31",
                note = "x".repeat(FormValidation.MAX_NOTE_LENGTH + 1),
            ),
            purchaseDate = "2024-01-01",
        )

        assertTrue("type" in errors)
        assertTrue("amount" in errors)
        assertEquals("Cost date cannot be before the device purchase date.", errors["date"])
        assertTrue("note" in errors)

        assertTrue(
            FormValidation.cost(
                CostDraft(type = CostType.PARTS, amount = "19.99", date = "2024-01-02"),
                purchaseDate = "2024-01-01",
            ).isEmpty(),
        )
    }

    @Test
    fun saleValidatesRequiredFieldsDatesAndEveryFee() {
        val required = FormValidation.sale(validDevice(), SaleDraft(date = "2023-12-31"))
        assertTrue("price" in required)
        assertTrue("date" in required)
        assertTrue("channel" in required)

        val feeErrors = FormValidation.sale(
            validDevice(),
            validSaleDraft().copy(
                platformFee = "1.234",
                paymentFee = "oops",
                shipping = "1000000000",
                packaging = "2..0",
                otherFee = "3.999",
            ),
        )
        assertEquals(
            setOf("platformFee", "paymentFee", "shipping", "packaging", "otherFee"),
            feeErrors.keys,
        )
        assertTrue(FormValidation.sale(validDevice(), validSaleDraft()).isEmpty())
    }

    @Test
    fun moneyParsingIsExactAndRejectsOverflow() {
        assertEquals(105050L, Money.parseToCentsOrNull("$1,050.50"))
        assertEquals(101L, Money.parseToCentsOrNull("1.01"))
        assertNull(Money.parseToCentsOrNull("92233720368547759"))
        assertFalse(Money.parseToCentsOrNull("1.234") != null)
    }

    private fun validDeviceDraft() = DeviceDraft(
        category = DeviceCategory.PHONE,
        model = "iPhone 15 Pro",
        price = "650.00",
        date = "2024-01-01",
        source = AcquisitionSource.LOCAL,
        identifierLast4 = "4821",
        storage = "256GB",
        lock = LockStatus.UNLOCKED,
    )

    private fun validDevice() = Device(
        id = "device-1",
        category = DeviceCategory.PHONE,
        model = "iPhone 15 Pro",
        identifier = "IMEI ●●●●4821",
        condition = null,
        storage = "256GB",
        lock = LockStatus.UNLOCKED,
        purchasePriceCents = 65_000,
        source = AcquisitionSource.LOCAL,
        purchaseDate = "2024-01-01",
        costs = emptyList(),
        status = DeviceStatus.PURCHASED,
        daysHeld = 0,
    )

    private fun validSaleDraft() = SaleDraft(
        price = "800",
        date = "2024-01-05",
        channel = SalesChannel.IN_PERSON,
        platformFee = "",
        paymentFee = "0",
        shipping = "10.50",
        packaging = "",
        otherFee = "",
    )
}
