/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

public abstract class TestUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    public static Properties loadProperties(final String fileName) throws IOException {
        final String propertiesAsString = toString(fileName);
        final Properties properties = new Properties();
        properties.load(new StringReader(propertiesAsString));
        return properties;
    }

    public static OSGIKillbillLogService buildLogService() {
        final OSGIKillbillLogService logService = Mockito.mock(OSGIKillbillLogService.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                logger.info(String.valueOf(invocation.getArguments()[1]));
                return null;
            }
        }).when(logService).log(Mockito.anyInt(), Mockito.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                logger.info(String.valueOf(invocation.getArguments()[1]), (Throwable) invocation.getArguments()[2]);
                return null;
            }
        }).when(logService).log(Mockito.anyInt(), Mockito.anyString(), Mockito.<Throwable>any());

        return logService;
    }

    public static String toString(final String resourceName) throws IOException {
        final InputStream inputStream = Resources.getResource(resourceName).openStream();
        try {
            return new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    public static OSGIKillbillAPI buildOSGIKillbillAPI(final Account account, final Payment payment, @Nullable final PaymentMethod paymentMethod) throws AccountApiException, PaymentApiException {
        final OSGIKillbillAPI killbillApi = Mockito.mock(OSGIKillbillAPI.class);

        final AccountUserApi accountUserApi = Mockito.mock(AccountUserApi.class);
        Mockito.when(accountUserApi.getAccountById(Mockito.eq(account.getId()), Mockito.<TenantContext>any())).thenReturn(account);
        Mockito.when(killbillApi.getAccountUserApi()).thenReturn(accountUserApi);

        final PaymentApi paymentApi = Mockito.mock(PaymentApi.class);
        Mockito.when(paymentApi.getPayment(Mockito.eq(payment.getId()), Mockito.anyBoolean(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<TenantContext>any())).thenReturn(payment);
        Mockito.when(killbillApi.getPaymentApi()).thenReturn(paymentApi);

        if (paymentMethod != null) {
            Mockito.when(paymentApi.getPaymentMethodById(Mockito.eq(paymentMethod.getId()), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<TenantContext>any())).thenReturn(paymentMethod);
        }

        return killbillApi;
    }

    public static Account buildAccount(final Currency currency, final String country) {
        return buildAccount(currency, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString().substring(0, 16), country);
    }

    public static Account buildAccount(final Currency currency, final String address1, final String address2, final String city, final String stateOrProvince, final String postalCode, final String country) {
        final Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(account.getExternalKey()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(account.getName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(account.getFirstNameLength()).thenReturn(4);
        Mockito.when(account.getEmail()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(account.getBillCycleDayLocal()).thenReturn(2);
        Mockito.when(account.getCurrency()).thenReturn(currency);
        Mockito.when(account.getPaymentMethodId()).thenReturn(UUID.randomUUID());
        Mockito.when(account.getTimeZone()).thenReturn(DateTimeZone.getDefault());
        // Return language tag to be able to use Locale.forLanguageTag
        Mockito.when(account.getLocale()).thenReturn("en-US");
        Mockito.when(account.getAddress1()).thenReturn(address1);
        Mockito.when(account.getAddress2()).thenReturn(address2);
        Mockito.when(account.getCompanyName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(account.getCity()).thenReturn(city);
        Mockito.when(account.getStateOrProvince()).thenReturn(stateOrProvince);
        Mockito.when(account.getPostalCode()).thenReturn(postalCode);
        Mockito.when(account.getCountry()).thenReturn(country);
        Mockito.when(account.getPhone()).thenReturn(UUID.randomUUID().toString().substring(0, 25));
        Mockito.when(account.isMigrated()).thenReturn(true);
        Mockito.when(account.isNotifiedForInvoices()).thenReturn(true);
        Mockito.when(account.getCreatedDate()).thenReturn(new DateTime(2016, 1, 22, 10, 56, 47, DateTimeZone.UTC));
        Mockito.when(account.getUpdatedDate()).thenReturn(new DateTime(2016, 1, 22, 10, 56, 48, DateTimeZone.UTC));
        return account;
    }

    public static Payment buildPayment(final UUID accountId, final UUID paymentMethodId, final Currency currency) {
        final Payment payment = Mockito.mock(Payment.class);
        Mockito.when(payment.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(payment.getExternalKey()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(payment.getAccountId()).thenReturn(accountId);
        Mockito.when(payment.getPaymentMethodId()).thenReturn(paymentMethodId);
        Mockito.when(payment.getPaymentNumber()).thenReturn(1);
        Mockito.when(payment.getCapturedAmount()).thenReturn(new BigDecimal("199999"));
        Mockito.when(payment.getRefundedAmount()).thenReturn(new BigDecimal("199998"));
        Mockito.when(payment.getCurrency()).thenReturn(currency);
        Mockito.when(payment.getTransactions()).thenReturn(new LinkedList<PaymentTransaction>());
        Mockito.when(payment.getCreatedDate()).thenReturn(new DateTime(2016, 1, 22, 10, 56, 56, DateTimeZone.UTC));
        return payment;
    }

    public static PaymentTransaction buildPaymentTransaction(final Payment payment, final TransactionType transactionType, final Currency currency) {
        final PaymentTransaction paymentTransaction = Mockito.mock(PaymentTransaction.class);
        Mockito.when(paymentTransaction.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(paymentTransaction.getTransactionType()).thenReturn(transactionType);
        Mockito.when(paymentTransaction.getAmount()).thenReturn(new BigDecimal("199999"));
        Mockito.when(paymentTransaction.getCurrency()).thenReturn(currency);
        Mockito.when(paymentTransaction.getEffectiveDate()).thenReturn(new DateTime(2016, 1, 22, 10, 56, 56, DateTimeZone.UTC));
        Mockito.when(paymentTransaction.getExternalKey()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(paymentTransaction.getTransactionStatus()).thenReturn(TransactionStatus.SUCCESS);

        payment.getTransactions().add(paymentTransaction);

        return paymentTransaction;
    }

    public static PaymentMethod buildPaymentMethod(final UUID accountId, final UUID paymentMethodId, final String pluginName) {
        final PaymentMethod paymentMethod = Mockito.mock(PaymentMethod.class);
        Mockito.when(paymentMethod.getId()).thenReturn(paymentMethodId);
        Mockito.when(paymentMethod.getExternalKey()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(paymentMethod.isActive()).thenReturn(true);
        Mockito.when(paymentMethod.getPluginName()).thenReturn(pluginName);
        return paymentMethod;
    }

    public static Invoice buildInvoice(final Account account) {
        final Invoice invoice = Mockito.mock(Invoice.class);
        Mockito.when(invoice.getId()).thenReturn(UUID.randomUUID());
        // To work-around org.mockito.exceptions.misusing.UnfinishedStubbingException
        final UUID accountId = account.getId();
        final Currency currency = account.getCurrency();
        Mockito.when(invoice.getAccountId()).thenReturn(accountId);
        Mockito.when(invoice.getInvoiceDate()).thenReturn(new LocalDate());
        Mockito.when(invoice.getCurrency()).thenReturn(currency);
        Mockito.when(invoice.getInvoiceItems()).thenReturn(new LinkedList<InvoiceItem>());
        return invoice;
    }

    public static InvoiceItem buildInvoiceItem(final Invoice invoice, final InvoiceItemType invoiceItemType, final BigDecimal amount, @Nullable final UUID linkedItemID) {
        final InvoiceItem invoiceItem = Mockito.mock(InvoiceItem.class);
        Mockito.when(invoiceItem.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getInvoiceItemType()).thenReturn(invoiceItemType);
        // To work-around org.mockito.exceptions.misusing.UnfinishedStubbingException
        final UUID accountId = invoice.getAccountId();
        final UUID invoiceId = invoice.getId();
        final Currency currency = invoice.getCurrency();
        Mockito.when(invoiceItem.getAccountId()).thenReturn(accountId);
        Mockito.when(invoiceItem.getInvoiceId()).thenReturn(invoiceId);
        Mockito.when(invoiceItem.getLinkedItemId()).thenReturn(linkedItemID);
        Mockito.when(invoiceItem.getBundleId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getSubscriptionId()).thenReturn(UUID.randomUUID());
        Mockito.when(invoiceItem.getPlanName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(invoiceItem.getPhaseName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(invoiceItem.getUsageName()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(invoiceItem.getAmount()).thenReturn(amount);
        Mockito.when(invoiceItem.getCurrency()).thenReturn(currency);

        return invoiceItem;
    }

    /* ====================================================================== */

    public static final List<PluginProperty> toProperties(final Map<String, String> properties) {
        final List<PluginProperty> list = new ArrayList<PluginProperty>();
        for (final Entry<String, String> entry : properties.entrySet()) {
            list.add(new PluginProperty(entry.getKey(), entry.getValue(), false));
        }
        return list;
    }

    public static Map<String, String> buildPluginPropertiesMap(final String additionalData) {
        if (additionalData == null) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(additionalData, new TypeReference<Map<String, ?>>(){});
        } catch (final IOException exception) {
            throw new IllegalArgumentException("Malformed JSON: " + additionalData, exception);
        }
    }

    public static List<PluginProperty> buildPluginProperties(@Nullable final String additionalData) {
        if (additionalData == null) {
            return Collections.emptyList();
        }
        return PluginProperties.buildPluginProperties(buildPluginPropertiesMap(additionalData));
    }
}
