/*
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.payment.api;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.killbill.billing.ErrorCode;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.core.DirectPaymentProcessor;
import org.killbill.billing.payment.core.PaymentMethodProcessor;
import org.killbill.billing.payment.core.PluginControlledPaymentProcessor;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.InternalCallContextFactory;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;

import com.google.common.base.Preconditions;

public class DefaultDirectPaymentApi implements DirectPaymentApi {

    private static final boolean SHOULD_LOCK_ACCOUNT = true;

    private final DirectPaymentProcessor directPaymentProcessor;
    private final PaymentMethodProcessor paymentMethodProcessor;
    private final PluginControlledPaymentProcessor pluginControlledPaymentProcessor;
    private final InternalCallContextFactory internalCallContextFactory;

    @Inject
    public DefaultDirectPaymentApi(final DirectPaymentProcessor directPaymentProcessor, final PaymentMethodProcessor paymentMethodProcessor, final PluginControlledPaymentProcessor pluginControlledPaymentProcessor, final InternalCallContextFactory internalCallContextFactory) {
        this.directPaymentProcessor = directPaymentProcessor;
        this.paymentMethodProcessor = paymentMethodProcessor;
        this.pluginControlledPaymentProcessor = pluginControlledPaymentProcessor;
        this.internalCallContextFactory = internalCallContextFactory;
    }

    @Override
    public DirectPayment createAuthorization(final Account account, final UUID paymentMethodId, @Nullable final UUID directPaymentId, final BigDecimal amount, final Currency currency, final String directPaymentExternalKey, final String directPaymentTransactionExternalKey,
                                             final Iterable<PluginProperty> properties, final CallContext callContext) throws PaymentApiException {

        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return directPaymentProcessor.createAuthorization(account, paymentMethodId, directPaymentId, amount, currency, directPaymentExternalKey, directPaymentTransactionExternalKey,
                                                          SHOULD_LOCK_ACCOUNT, properties, callContext, internalCallContext);
    }

    @Override
    public DirectPayment createCapture(final Account account, final UUID directPaymentId, final BigDecimal amount, final Currency currency, final String directPaymentTransactionExternalKey,
                                       final Iterable<PluginProperty> properties, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return directPaymentProcessor.createCapture(account, directPaymentId, amount, currency, directPaymentTransactionExternalKey,
                                                    SHOULD_LOCK_ACCOUNT, properties, callContext, internalCallContext);
    }

    @Override
    public DirectPayment createPurchase(final Account account, final UUID paymentMethodId, @Nullable final UUID directPaymentId, final BigDecimal amount, final Currency currency, final String directPaymentExternalKey, final String directPaymentTransactionExternalKey,
                                        final Iterable<PluginProperty> properties, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return directPaymentProcessor.createPurchase(account, paymentMethodId, directPaymentId, amount, currency, directPaymentExternalKey, directPaymentTransactionExternalKey,
                                                     SHOULD_LOCK_ACCOUNT, properties, callContext, internalCallContext);
    }

    @Override
    public DirectPayment createPurchaseWithPaymentControl(final Account account, @Nullable final UUID paymentMethodId, @Nullable final UUID directPaymentId, final BigDecimal amount, final Currency currency, final String directPaymentExternalKey, final String directPaymentTransactionExternalKey,
                                                          final Iterable<PluginProperty> properties, final PaymentOptions paymentOptions, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);

        // STEPH should throw PaymentApiException -- at least when coming from API; also add description.
        Preconditions.checkArgument(paymentMethodId != null || paymentOptions.isExternalPayment());
        final UUID nonNulPaymentMethodId = (paymentMethodId != null) ?
                                           paymentMethodId :
                                           paymentMethodProcessor.createOrGetExternalPaymentMethod(account, properties, callContext, internalCallContext);
        return pluginControlledPaymentProcessor.createPurchase(true, account, nonNulPaymentMethodId, directPaymentId, amount, currency, directPaymentExternalKey, directPaymentTransactionExternalKey,
                                                               properties, paymentOptions.getPaymentControlPluginName(), callContext, internalCallContext);

    }

    @Override
    public DirectPayment createVoid(final Account account, final UUID directPaymentId, final String directPaymentTransactionExternalKey, final Iterable<PluginProperty> properties,
                                    final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return directPaymentProcessor.createVoid(account, directPaymentId, directPaymentTransactionExternalKey,
                                                 SHOULD_LOCK_ACCOUNT, properties, callContext, internalCallContext);

    }

    @Override
    public DirectPayment createRefund(final Account account, final UUID directPaymentId, final BigDecimal amount, final Currency currency, final String directPaymentTransactionExternalKey, final Iterable<PluginProperty> properties,
                                      final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return directPaymentProcessor.createRefund(account, directPaymentId, amount, currency, directPaymentTransactionExternalKey,
                                                   SHOULD_LOCK_ACCOUNT, properties, callContext, internalCallContext);
    }

    @Override
    public DirectPayment createRefundWithPaymentControl(final Account account, final UUID directPaymentId, final BigDecimal amount, final Currency currency, final String directPaymentTransactionExternalKey, final Iterable<PluginProperty> properties,
                                                        final PaymentOptions paymentOptions, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return pluginControlledPaymentProcessor.createRefund(true, account, directPaymentId, amount, currency, directPaymentTransactionExternalKey,
                                                             properties, paymentOptions.getPaymentControlPluginName(), callContext, internalCallContext);

    }

    @Override
    public DirectPayment createCredit(final Account account, final UUID paymentMethodId, final UUID directPaymentId, final BigDecimal amount, final Currency currency,
                                      final String directPaymentExternalKey, final String directPaymentTransactionExternalKey,
                                      final Iterable<PluginProperty> properties, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        return directPaymentProcessor.createCredit(account, paymentMethodId, directPaymentId, amount, currency, directPaymentExternalKey, directPaymentTransactionExternalKey,
                                                   SHOULD_LOCK_ACCOUNT, properties, callContext, internalCallContext);

    }

    // STEPH API: should we only pass transactionExternalKey ? (notification may keep only one key; should we use kbTransactionId or externalKey ?)
    // DO we need to pass account (i guess this helps for context)
    @Override
    public void notifyPendingPaymentOfStateChanged(final Account account, String transactionExternalKey, final boolean isSuccess, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        directPaymentProcessor.notifyPendingPaymentOfStateChanged(account, transactionExternalKey, isSuccess, callContext, internalCallContext);
    }

    // STEPH API Is chargeback against a payment or transaction?; do we want to provide a new transactionExternalKey (probably) ?
    @Override
    public void notifyPaymentPaymentOfChargeback(final Account account, String paymentExternalKey, String chargebackExternalKey, BigDecimal amount, Currency currency, final CallContext callContext) throws PaymentApiException {
        final InternalCallContext internalCallContext = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);
        directPaymentProcessor.notifyPaymentPaymentOfChargeback(account, paymentExternalKey, chargebackExternalKey, amount, currency, callContext, internalCallContext);
    }

    @Override
    public List<DirectPayment> getAccountPayments(final UUID accountId, final TenantContext tenantContext) throws PaymentApiException {
        return directPaymentProcessor.getAccountPayments(accountId, internalCallContextFactory.createInternalTenantContext(accountId, tenantContext));
    }

    @Override
    public Pagination<DirectPayment> getPayments(final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) {
        return directPaymentProcessor.getPayments(offset, limit, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Pagination<DirectPayment> getPayments(final Long offset, final Long limit, final String pluginName, final Iterable<PluginProperty> properties, final TenantContext tenantContext) throws PaymentApiException {
        return directPaymentProcessor.getPayments(offset, limit, pluginName, properties, tenantContext, internalCallContextFactory.createInternalTenantContext(tenantContext));
    }

    @Override
    public DirectPayment getPayment(final UUID paymentId, final boolean withPluginInfo, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentApiException {
        final DirectPayment payment = directPaymentProcessor.getPayment(paymentId, withPluginInfo, properties, context, internalCallContextFactory.createInternalTenantContext(context));
        if (payment == null) {
            throw new PaymentApiException(ErrorCode.PAYMENT_NO_SUCH_PAYMENT, paymentId);
        }
        return payment;
    }

    @Override
    public DirectPayment getPaymentByExternalKey(String paymentExternalKey, final boolean withPluginInfo, Iterable<PluginProperty> properties, TenantContext tenantContext)
            throws PaymentApiException {
        return directPaymentProcessor.getPaymentByExternalKey(paymentExternalKey, withPluginInfo, properties, tenantContext, internalCallContextFactory.createInternalTenantContext(tenantContext));
    }

    @Override
    public Pagination<DirectPayment> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) {
        return directPaymentProcessor.searchPayments(searchKey, offset, limit, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Pagination<DirectPayment> searchPayments(final String searchKey, final Long offset, final Long limit, final String pluginName, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentApiException {
        return directPaymentProcessor.searchPayments(searchKey, offset, limit, pluginName, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Set<String> getAvailablePlugins() {
        return paymentMethodProcessor.getAvailablePlugins();
    }

    @Override
    public UUID addPaymentMethod(final String pluginName, final Account account,
                                 final boolean setDefault, final PaymentMethodPlugin paymentMethodInfo,
                                 final Iterable<PluginProperty> properties, final CallContext context)
            throws PaymentApiException {
        return paymentMethodProcessor.addPaymentMethod(pluginName, account, setDefault, paymentMethodInfo, properties,
                                                       context, internalCallContextFactory.createInternalCallContext(account.getId(), context));
    }

    @Override
    public List<PaymentMethod> getPaymentMethods(final Account account, final boolean withPluginInfo, final Iterable<PluginProperty> properties, final TenantContext context)
            throws PaymentApiException {
        return paymentMethodProcessor.getPaymentMethods(account, withPluginInfo, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public PaymentMethod getPaymentMethodById(final UUID paymentMethodId, final boolean includedDeleted, final boolean withPluginInfo, final Iterable<PluginProperty> properties, final TenantContext context)
            throws PaymentApiException {
        return paymentMethodProcessor.getPaymentMethodById(paymentMethodId, includedDeleted, withPluginInfo, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Pagination<PaymentMethod> getPaymentMethods(final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) {
        return paymentMethodProcessor.getPaymentMethods(offset, limit, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Pagination<PaymentMethod> getPaymentMethods(final Long offset, final Long limit, final String pluginName, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentApiException {
        return paymentMethodProcessor.getPaymentMethods(offset, limit, pluginName, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Pagination<PaymentMethod> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) {
        return paymentMethodProcessor.searchPaymentMethods(searchKey, offset, limit, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public Pagination<PaymentMethod> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final String pluginName, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentApiException {
        return paymentMethodProcessor.searchPaymentMethods(searchKey, offset, limit, pluginName, properties, context, internalCallContextFactory.createInternalTenantContext(context));
    }

    @Override
    public void deletedPaymentMethod(final Account account, final UUID paymentMethodId, final boolean deleteDefaultPaymentMethodWithAutoPayOff, final Iterable<PluginProperty> properties, final CallContext context)
            throws PaymentApiException {
        paymentMethodProcessor.deletedPaymentMethod(account, paymentMethodId, deleteDefaultPaymentMethodWithAutoPayOff, properties, context, internalCallContextFactory.createInternalCallContext(account.getId(), context));
    }

    @Override
    public void setDefaultPaymentMethod(final Account account, final UUID paymentMethodId, final Iterable<PluginProperty> properties, final CallContext context)
            throws PaymentApiException {
        paymentMethodProcessor.setDefaultPaymentMethod(account, paymentMethodId, properties, context, internalCallContextFactory.createInternalCallContext(account.getId(), context));
    }

    @Override
    public List<PaymentMethod> refreshPaymentMethods(final String pluginName, final Account account, final Iterable<PluginProperty> properties, final CallContext context)
            throws PaymentApiException {
        return paymentMethodProcessor.refreshPaymentMethods(pluginName, account, properties, context, internalCallContextFactory.createInternalCallContext(account.getId(), context));
    }

    @Override
    public List<PaymentMethod> refreshPaymentMethods(final Account account, final Iterable<PluginProperty> properties, final CallContext context)
            throws PaymentApiException {
        final InternalCallContext callContext = internalCallContextFactory.createInternalCallContext(account.getId(), context);

        final List<PaymentMethod> paymentMethods = new LinkedList<PaymentMethod>();
        for (final String pluginName : paymentMethodProcessor.getAvailablePlugins()) {
            paymentMethods.addAll(paymentMethodProcessor.refreshPaymentMethods(pluginName, account, properties, context, callContext));
        }

        return paymentMethods;
    }
}
