package org.telegram.messenger;

import static org.telegram.messenger.MessagesController.findUpdatesAndRemove;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

// NOTE: All imports of com.android.billingclient.api have been removed.

import org.telegram.messenger.utils.BillingUtilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LoginActivity;
import org.telegram.ui.PremiumPreviewFragment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BillingController /* implements PurchasesUpdatedListener, BillingClientStateListener */ {
    public final static String PREMIUM_PRODUCT_ID = "telegram_premium";
    public final static QueryProductDetailsParams.Product PREMIUM_PRODUCT = QueryProductDetailsParams.Product.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .setProductId(PREMIUM_PRODUCT_ID)
            .build();

//    @Nullable
//    public static ProductDetails PREMIUM_PRODUCT_DETAILS;

    private static BillingController instance;

    public static boolean billingClientEmpty;

    private final Map<String, Consumer<BillingResult>> resultListeners = new HashMap<>();
    private final Set<String> requestingTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Integer> currencyExpMap = new HashMap<>();
//    private final BillingClient billingClient;
    private String lastPremiumTransaction;
    private String lastPremiumToken;
    private boolean isDisconnected;
    private Runnable onCanceled;

    public static BillingController getInstance() {
        if (instance == null) {
            instance = new BillingController(ApplicationLoader.applicationContext);
        }
        return instance;
    }

    private BillingController(Context ctx) {
//        billingClient = BillingClient.newBuilder(ctx)
//                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
//                .setListener(this)
//                .build();
    }

    public void setOnCanceled(Runnable onCanceled) {
        this.onCanceled = onCanceled;
    }

    public String getLastPremiumTransaction() {
        return lastPremiumTransaction;
    }

    public String getLastPremiumToken() {
        return lastPremiumToken;
    }

    public String formatCurrency(long amount, String currency) {
        return formatCurrency(amount, currency, getCurrencyExp(currency));
    }

    public String formatCurrency(long amount, String currency, int exp) {
        return formatCurrency(amount, currency, exp, false);
    }

    private static NumberFormat currencyInstance;
    private static NumberFormat currencyInstanceRounded;
    public String formatCurrency(long amount, String currency, int exp, boolean rounded) {
        if (currency == null || currency.isEmpty()) {
            return String.valueOf(amount);
        }
        if ("TON".equalsIgnoreCase(currency)) {
            return "TON " + (amount / 1_000_000_000.0);
        }
        if ("XTR".equalsIgnoreCase(currency)) {
            return "XTR " + LocaleController.formatNumber(amount, ',');
        }
        Currency cur = Currency.getInstance(currency);
        if (cur != null) {
            if (currencyInstance == null) {
                currencyInstance = NumberFormat.getCurrencyInstance();
            }
            currencyInstance.setCurrency(cur);
            if (rounded) {
                currencyInstance.setMaximumFractionDigits(0);
                currencyInstance.setMinimumFractionDigits(0);
                return currencyInstance.format(Math.round(amount / Math.pow(10, exp)));
            }
            final int defaultFractionDigits = cur.getDefaultFractionDigits();
            currencyInstance.setMinimumFractionDigits(defaultFractionDigits);
            currencyInstance.setMaximumFractionDigits(defaultFractionDigits);
            return currencyInstance.format(amount / Math.pow(10, exp));
        }
        return amount + " " + currency;
    }

    @SuppressWarnings("ConstantConditions")
    public int getCurrencyExp(String currency) {
        BillingUtilities.extractCurrencyExp(currencyExpMap);
        return currencyExpMap.getOrDefault(currency, 0);
    }

    public void startConnection() {
        if (isReady()) {
            return;
        }
        try {
            switchToInvoice(); // force invoice
            BillingUtilities.extractCurrencyExp(currencyExpMap);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void switchToInvoice() {
        if (billingClientEmpty) {
            return;
        }
        billingClientEmpty = true;
        NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.billingProductDetailsUpdated);
    }

    private void switchBackFromInvoice() {
        if (BuildVars.useInvoiceBilling()) {
            Log.d("030-bill", "force useInvoiceBilling");
            return;
        }
        if (!billingClientEmpty) {
            return;
        }
        billingClientEmpty = false;
        NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.billingProductDetailsUpdated);
    }

    public boolean isReady() {
        return billingClientEmpty;
    }

    public interface ProductDetailsResponseListenerLegacy {
        void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> list);
    }

    public void queryProductDetails(List<QueryProductDetailsParams.Product> products, ProductDetailsResponseListenerLegacy responseListener) {
        if (!isReady()) {
            throw new IllegalStateException("Billing: Controller should be ready for this call!");
        }
        // Billing client removed: immediately report invoice-only mode.
        if (responseListener != null) {
            responseListener.onProductDetailsResponse(
                    BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                            .setDebugMessage("Billing client removed")
                            .build(),
                    Collections.emptyList()
            );
        }
    }

//    /**
//     * {@link BillingClient#queryPurchasesAsync} returns only active subscriptions and not consumed purchases.
//     */
    public void queryPurchases(String productType, PurchasesResponseListener responseListener) {
        // Billing client removed: return OK with no purchases.
        if (responseListener != null) {
            responseListener.onQueryPurchasesResponse(
                    BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.OK)
                            .setDebugMessage("Billing client removed")
                            .build(),
                    Collections.emptyList()
            );
        }
    }

    public boolean startManageSubscription(Context ctx, String productId) {
        return false; // we no talk to googay
    }

    public void addResultListener(String productId, Consumer<BillingResult> listener) {
        resultListeners.put(productId, listener);
    }

    public void launchBillingFlow(Activity activity, AccountInstance accountInstance, TLRPC.InputStorePaymentPurpose paymentPurpose, List<BillingFlowParams.ProductDetailsParams> productDetails) {
        launchBillingFlow(activity, accountInstance, paymentPurpose, productDetails, null, false);
    }

    public void launchBillingFlow(Activity activity, AccountInstance accountInstance, TLRPC.InputStorePaymentPurpose paymentPurpose, List<BillingFlowParams.ProductDetailsParams> productDetails, BillingFlowParams.SubscriptionUpdateParams subscriptionUpdateParams, boolean checkedConsume) {
        if (!isReady() || activity == null) {
            return;
        }

        if ((paymentPurpose instanceof TLRPC.TL_inputStorePaymentGiftPremium || paymentPurpose instanceof TLRPC.TL_inputStorePaymentStarsTopup || paymentPurpose instanceof TLRPC.TL_inputStorePaymentStarsGift) && !checkedConsume) {
            FileLog.d("BillingController.launchBillingFlow, checking consumables");
            queryPurchases(BillingClient.ProductType.INAPP, (billingResult, list) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    FileLog.d("BillingController.launchBillingFlow, checked consumables: OK");
                    Runnable callback = () -> launchBillingFlow(activity, accountInstance, paymentPurpose, productDetails, subscriptionUpdateParams, true);

                    AtomicInteger productsToBeConsumed = new AtomicInteger(0);
                    List<String> productsConsumed = new ArrayList<>();
                    for (Purchase purchase : list) {
                        if (purchase.isAcknowledged()) {
                            for (BillingFlowParams.ProductDetailsParams params : productDetails) {
                                String productId = params.zza().getProductId();
                                if (purchase.getProducts().contains(productId)) {
                                    productsToBeConsumed.incrementAndGet();
                                    FileLog.d("BillingController.launchBillingFlow, consuming (noop) " + purchase.getPurchaseToken());
                                    productsConsumed.add(productId);
                                    if (productsToBeConsumed.get() == productsConsumed.size()) {
                                        callback.run();
                                    }
                                    break;
                                }
                            }
                        } else {
                            productsToBeConsumed.incrementAndGet();
                            onPurchasesUpdatedInternal(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(), Collections.singletonList(purchase), () -> {
                                productsConsumed.add(null);
                                if (productsToBeConsumed.get() == productsConsumed.size()) {
                                    callback.run();
                                }
                            });
                        }
                    }

                    if (productsToBeConsumed.get() == 0) {
                        callback.run();
                    }
                } else {
                    FileLog.d("BillingController.launchBillingFlow, checked consumables: " + billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                    launchBillingFlow(activity, accountInstance, paymentPurpose, productDetails, subscriptionUpdateParams, false);
                }
            });
            return;
        }
        if (checkedConsume) {
            FileLog.d("BillingController.launchBillingFlow, consumables checked, launching flow...");
        }

        Pair<String, String> payload = BillingUtilities.createDeveloperPayload(paymentPurpose, accountInstance);
        String obfuscatedAccountId = payload.first;
        String obfuscatedData = payload.second;

        // Billing client removed: cannot launch Google Play flow. Log and rely on invoice fallback.
        FileLog.d("Billing: Launch skipped (billing client removed), " + obfuscatedAccountId + ", " + obfuscatedData);
    }

    public void onPurchasesUpdated(@NonNull BillingResult billing, @Nullable List<Purchase> list) {
        onPurchasesUpdatedInternal(billing, list, null);
    }

    public void onPurchasesUpdatedInternal(@NonNull BillingResult billing, @Nullable List<Purchase> list, @Nullable Runnable onDone) {
        FileLog.d("Billing: Purchases updated: " + billing + ", " + list);
        if (billing.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            if (billing.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                PremiumPreviewFragment.sentPremiumBuyCanceled();
            }
            if (onCanceled != null) {
                onCanceled.run();
                onCanceled = null;
            }
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        if (list == null || list.isEmpty()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        AtomicInteger awaitingCount = new AtomicInteger(0);
        AtomicInteger doneCount = new AtomicInteger(0);
        lastPremiumTransaction = null;
        for (Purchase purchase : list) {
            if (purchase.getProducts().contains(PREMIUM_PRODUCT_ID)) {
                lastPremiumTransaction = purchase.getOrderId();
                lastPremiumToken = purchase.getPurchaseToken();
            }

            if (!requestingTokens.contains(purchase.getPurchaseToken())) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    Pair<AccountInstance, TLRPC.InputStorePaymentPurpose> opayload = BillingUtilities.extractDeveloperPayload(purchase);
                    if (opayload == null || opayload.first == null || opayload.second == null) {
                        FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is purchased, but failed to extract saved payload");
                        continue;
                    }
                    if (!purchase.isAcknowledged()) {
                        FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is purchased and not acknowledged: assigning (accountId=" + opayload.first.getCurrentAccount() + ") (purpose=" + opayload.second + ")");
                        requestingTokens.add(purchase.getPurchaseToken());

                        TLRPC.TL_payments_assignPlayMarketTransaction req = new TLRPC.TL_payments_assignPlayMarketTransaction();
                        req.receipt = new TLRPC.TL_dataJSON();
                        req.receipt.data = purchase.getOriginalJson();
                        req.purpose = opayload.second;

                        final AlertDialog[] progressDialog = new AlertDialog[1];
                        AndroidUtilities.runOnUIThread(() -> {
                            progressDialog[0] = new AlertDialog(ApplicationLoader.applicationContext, AlertDialog.ALERT_TYPE_SPINNER);
                            progressDialog[0].showDelayed(500);
                        });

                        awaitingCount.incrementAndGet();
                        AccountInstance acc = opayload.first;
                        int requestFlags = ConnectionsManager.RequestFlagFailOnServerErrorsExceptFloodWait | ConnectionsManager.RequestFlagInvokeAfter;
                        if (req.purpose instanceof TLRPC.TL_inputStorePaymentAuthCode) {
                            requestFlags |= ConnectionsManager.RequestFlagWithoutLogin;
                        }
                        acc.getConnectionsManager().sendRequest(req, (response, error) -> {
                            AndroidUtilities.runOnUIThread(() -> {
                                if (progressDialog[0] != null) {
                                    progressDialog[0].dismiss();
                                }
                            });

                            requestingTokens.remove(purchase.getPurchaseToken());

                            if (response instanceof TLRPC.Updates) {
                                FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is purchased and now assigned");

                                if (req.purpose instanceof TLRPC.TL_inputStorePaymentAuthCode) {
                                    for (TL_update.TL_updateSentPhoneCode u : findUpdatesAndRemove((TLRPC.Updates) response, TL_update.TL_updateSentPhoneCode.class)) {
                                        AndroidUtilities.runOnUIThread(() -> {
                                            LoginActivity fragment = LaunchActivity.findFragment(LoginActivity.class);
                                            if (fragment == null) {
                                                fragment = new LoginActivity(acc.getCurrentAccount());
                                                BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                                                if (lastFragment != null) {
                                                    lastFragment.presentFragment(fragment);
                                                }
                                            }
                                            fragment.open(((TLRPC.TL_inputStorePaymentAuthCode) req.purpose).phone_number, u.sent_code);
                                        });
                                    }
                                }

                                acc.getMessagesController().processUpdates((TLRPC.Updates) response, false);

                                for (String productId : purchase.getProducts()) {
                                    Consumer<BillingResult> listener = resultListeners.remove(productId);
                                    if (listener != null) {
                                        listener.accept(billing);
                                    }
                                }

                                consumeGiftPurchase(purchase, req.purpose, () -> {
                                    if (doneCount.incrementAndGet() == awaitingCount.get() && onDone != null) {
                                        onDone.run();
                                    }
                                });
                                BillingUtilities.cleanupPurchase(purchase);
                            } else {
                                FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is purchased and failed to assign: " + (error == null ? null : error.text));

                                if (onCanceled != null) {
                                    onCanceled.run();
                                    onCanceled = null;
                                }
                                if (error != null) {
                                    NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.billingConfirmPurchaseError, req, error);
                                }

                                AndroidUtilities.runOnUIThread(() -> {
                                    if (doneCount.incrementAndGet() == awaitingCount.get() && onDone != null) {
                                        onDone.run();
                                    }
                                });
                            }
                        }, requestFlags);
                    } else {
                        FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is purchased and acknowledged: consuming");
                        awaitingCount.incrementAndGet();
                        consumeGiftPurchase(purchase, opayload.second, () -> {
                            if (doneCount.incrementAndGet() == awaitingCount.get() && onDone != null) {
                                onDone.run();
                            }
                        });
                    }
                } else {
                    FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is (state=" + purchase.getPurchaseState() + "), (isAcknowledged=" + purchase.isAcknowledged() + ")");
                }
            } else {
                FileLog.d("BillingController.onPurchasesUpdatedInternal: " + purchase.getOrderId() + " purchase is already requesting...");
            }
        }
        if (awaitingCount.get() == 0 && onDone != null) {
            onDone.run();
        }
    }

    /**
     * All consumable purchases must be consumed. For us it is a gift.
     * Without confirmation the user will not be able to buy the product again.
     */
    public void consumeGiftPurchase(Purchase purchase, TLRPC.InputStorePaymentPurpose purpose, Runnable onDone) {
        if (purpose instanceof TLRPC.TL_inputStorePaymentGiftPremium ||
            purpose instanceof TLRPC.TL_inputStorePaymentPremiumGiftCode ||
            purpose instanceof TLRPC.TL_inputStorePaymentStarsTopup ||
            purpose instanceof TLRPC.TL_inputStorePaymentStarsGift ||
            purpose instanceof TLRPC.TL_inputStorePaymentPremiumGiveaway ||
            purpose instanceof TLRPC.TL_inputStorePaymentStarsGiveaway ||
            purpose instanceof TLRPC.TL_inputStorePaymentAuthCode
        ) {
            FileLog.d("BillingController consumeGiftPurchase (noop) " + purpose + " " + purchase.getOrderId() + " " + purchase.getPurchaseToken());
            if (onDone != null) {
                onDone.run();
            }
        }
    }

    /**
     * May occur in extremely rare cases.
     * For example when Google Play decides to update.
     */
    @SuppressWarnings("Convert2MethodRef")
    public void onBillingServiceDisconnected() {
        FileLog.d("Billing: Service disconnected");
        int delay = isDisconnected ? 15000 : 5000;
        isDisconnected = true;
        AndroidUtilities.runOnUIThread(() -> startConnection(), delay);
    }

    private ArrayList<Runnable> setupListeners = new ArrayList<>();
    public void whenSetuped(Runnable listener) {
        setupListeners.add(listener);
    }

    private int triesLeft = 0;

    public void onBillingSetupFinished(@NonNull BillingResult setupBillingResult) {
        FileLog.d("Billing: Setup finished with result " + setupBillingResult);
        if (setupBillingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            isDisconnected = false;
            triesLeft = 3;
            try {
                queryProductDetails(Collections.singletonList(PREMIUM_PRODUCT), this::onQueriedPremiumProductDetails);
            } catch (Exception e) {
                FileLog.e(e);
            }
            queryPurchases(BillingClient.ProductType.INAPP, this::onPurchasesUpdated);
            queryPurchases(BillingClient.ProductType.SUBS, this::onPurchasesUpdated);
            if (!setupListeners.isEmpty()) {
                for (int i = 0; i < setupListeners.size(); ++i) {
                    AndroidUtilities.runOnUIThread(setupListeners.get(i));
                }
                setupListeners.clear();
            }
        } else {
            if (!isDisconnected) {
                switchToInvoice();
            }
        }
    }

    private void onQueriedPremiumProductDetails(BillingResult billingResult, List<ProductDetails> list) {
//        FileLog.d("Billing: Query product details finished " + billingResult + ", " + list);
//        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//            for (ProductDetails details : list) {
//                if (details.getProductId().equals(PREMIUM_PRODUCT_ID)) {
//                    PREMIUM_PRODUCT_DETAILS = details;
//                }
//            }
//            if (PREMIUM_PRODUCT_DETAILS == null) {
//                switchToInvoice();
//            } else {
//                switchBackFromInvoice();
//                NotificationCenter.getGlobalInstance().postNotificationNameOnUIThread(NotificationCenter.billingProductDetailsUpdated);
//            }
//        } else
        {
            switchToInvoice();
            triesLeft--;
            if (triesLeft > 0) {
                long delay;
                if (triesLeft == 2) {
                    delay = 1000;
                } else {
                    delay = 10000;
                }
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        queryProductDetails(Collections.singletonList(PREMIUM_PRODUCT), this::onQueriedPremiumProductDetails);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }, delay);
            }
        }
    }

    public static String getResponseCodeString(int code) {
        switch (code) {
            case -3: return "SERVICE_TIMEOUT";
            case -2: return "FEATURE_NOT_SUPPORTED";
            case -1: return "SERVICE_DISCONNECTED";
            case 0: return "OK";
            case 1: return "USER_CANCELED";
            case 2: return "SERVICE_UNAVAILABLE";
            case 3: return "BILLING_UNAVAILABLE";
            case 4: return "ITEM_UNAVAILABLE";
            case 5: return "DEVELOPER_ERROR";
            case 6: return "ERROR";
            case 7: return "ITEM_ALREADY_OWNED";
            case 8: return "ITEM_NOT_OWNED";
            default: return "BILLING_UNKNOWN_ERROR";
        }
    }

    // Below: local minimal placeholder API to avoid dependency on
    // com.android.billingclient.api. These provide only what this file uses.

    public static class BillingClient {
        public static class ProductType {
            public static final String INAPP = "inapp";
            public static final String SUBS = "subs";
        }

        public static class BillingResponseCode {
            public static final int OK = 0;
            public static final int USER_CANCELED = 1;
            public static final int SERVICE_UNAVAILABLE = 2;
            public static final int BILLING_UNAVAILABLE = 3;
            public static final int ITEM_UNAVAILABLE = 4;
            public static final int DEVELOPER_ERROR = 5;
            public static final int ERROR = 6;
            public static final int ITEM_ALREADY_OWNED = 7;
            public static final int ITEM_NOT_OWNED = 8;
        }
    }

    public static class BillingResult {
        private final int responseCode;
        private final String debugMessage;

        private BillingResult(int responseCode, String debugMessage) {
            this.responseCode = responseCode;
            this.debugMessage = debugMessage == null ? "" : debugMessage;
        }

        public int getResponseCode() { return responseCode; }
        public String getDebugMessage() { return debugMessage; }

        public static Builder newBuilder() { return new Builder(); }
        public static class Builder {
            private int responseCode;
            private String debugMessage = "";
            public Builder setResponseCode(int code) { this.responseCode = code; return this; }
            public Builder setDebugMessage(String msg) { this.debugMessage = msg; return this; }
            public BillingResult build() { return new BillingResult(responseCode, debugMessage); }
        }

        @Override
        public String toString() {
            return "BillingResult{" + responseCode + ", '" + debugMessage + "'}";
        }
    }

    public interface ProductDetailsResponseListener {
        void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList);
    }

    public interface PurchasesResponseListener {
        void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases);
    }

    public static class ProductDetails {
        private final String productId;
        private final OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails;
        public ProductDetails(String productId) { this(productId, null); }
        public ProductDetails(String productId, OneTimePurchaseOfferDetails offer) {
            this.productId = productId;
            this.oneTimePurchaseOfferDetails = offer;
        }
        public String getProductId() { return productId; }
        public OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() { return oneTimePurchaseOfferDetails; }

        public static class OneTimePurchaseOfferDetails {
            private final String priceCurrencyCode;
            private final long priceAmountMicros;
            public OneTimePurchaseOfferDetails(String priceCurrencyCode, long priceAmountMicros) {
                this.priceCurrencyCode = priceCurrencyCode;
                this.priceAmountMicros = priceAmountMicros;
            }
            public String getPriceCurrencyCode() { return priceCurrencyCode; }
            public long getPriceAmountMicros() { return priceAmountMicros; }
        }
    }

    public static class QueryProductDetailsParams {
        public static Builder newBuilder() { return new Builder(); }
        private List<Product> productList;
        public static class Builder {
            private final QueryProductDetailsParams obj;
            public Builder setProductList(List<Product> list) { obj.productList = list; return this; }
            public Builder() {
                obj = new QueryProductDetailsParams();
            }
            public QueryProductDetailsParams build() { return obj; }
        }

        public static class Product {
            private String productType;
            private String productId;
            public static Product.Builder newBuilder() { return new Product.Builder(); }
            public String getProductId() { return productId; }
            public static class Builder {
                private final Product p = new Product();
                public Builder setProductType(String type) { p.productType = type; return this; }
                public Builder setProductId(String id) { p.productId = id; return this; }
                public Product build() { return p; }
            }
        }
    }

    public static class Purchase {
        public static class PurchaseState {
            public static final int UNSPECIFIED_STATE = 0;
            public static final int PURCHASED = 1;
            public static final int PENDING = 2;
        }

        private final String orderId;
        private final String purchaseToken;
        private final List<String> products;
        private final boolean acknowledged;
        private final int purchaseState;
        private final String originalJson;

        public Purchase(String orderId, String token, List<String> products, boolean acknowledged, int state, String originalJson) {
            this.orderId = orderId;
            this.purchaseToken = token;
            this.products = products == null ? Collections.emptyList() : products;
            this.acknowledged = acknowledged;
            this.purchaseState = state;
            this.originalJson = originalJson;
        }

        public boolean isAcknowledged() { return acknowledged; }
        public List<String> getProducts() { return products; }
        public String getPurchaseToken() { return purchaseToken; }
        public String getOrderId() { return orderId; }
        public int getPurchaseState() { return purchaseState; }
        public String getOriginalJson() { return originalJson; }
    }

    public static class BillingFlowParams {
//        public static Builder newBuilder() { return new Builder(); }
//        private String obfuscatedAccountId;
//        private String obfuscatedProfileId;
//        private List<ProductDetailsParams> list;
//        private SubscriptionUpdateParams subParams;
//        public static class Builder {
//            private BillingFlowParams obj;
//            public Builder() {
//                obj = new BillingFlowParams();
//            }
//            public Builder setObfuscatedAccountId(String s) { obj.obfuscatedAccountId = s; return this; }
//            public Builder setObfuscatedProfileId(String s) { obj.obfuscatedProfileId = s; return this; }
//            public Builder setProductDetailsParamsList(List<ProductDetailsParams> l) { obj.list = l; return this; }
//            public Builder setSubscriptionUpdateParams(SubscriptionUpdateParams p) { obj.subParams = p; return this; }
//            public BillingFlowParams build() { return obj; }
//        }
        public static class ProductDetailsParams {
            private ProductDetails details;
            public ProductDetailsParams(ProductDetails details) { this.details = details; }
            public ProductDetails zza() { return details; }
            public static Builder newBuilder() { return new Builder(); }
            public static class Builder {
                private final ProductDetailsParams p = new ProductDetailsParams(null);
                public Builder setProductDetails(ProductDetails details) { p.details = details; return this; }
                public ProductDetailsParams build() { return p; }
            }
        }
        public static class SubscriptionUpdateParams {}
    }
}
