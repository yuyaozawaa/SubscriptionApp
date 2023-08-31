package jp.yuya.ozawa.subscriptionapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.billingclient.api.*
import jp.yuya.ozawa.subscriptionapp.databinding.ActivitySubsBinding
import java.io.IOException
import java.util.concurrent.Executors

class Subs : AppCompatActivity() {
    var basePlanId: String? = null
    private var billingClient: BillingClient? = null

    private lateinit var binding: ActivitySubsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySubsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.card1.setBackgroundResource(R.drawable.stroke_change)
        binding.card2.setBackgroundResource(R.drawable.stroke_change)


        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        queryPurchase()

        if (PreferenceHelper.getBoolean("key_purchased_standard", false)) {
            binding.purchasedImageview.setImageResource(R.drawable.baseline_check_circle_24)
            binding.card1.setBackgroundResource(R.drawable.stroke_change_to)
        } else {
            binding.purchasedImageview.setImageResource(R.drawable.baseline_panorama_fish_eye_24)
            binding.card1.setBackgroundResource(R.drawable.stroke_change)
        }


        if (PreferenceHelper.getBoolean("key_purchased_pro", false)) {
            //            product.text = getString(R.string.billing_message_item)
            binding.purchasedProImageview2.setImageResource(R.drawable.baseline_check_circle_24)
            binding.card2.setBackgroundResource(R.drawable.stroke_change_to)
        } else {
            binding.purchasedProImageview2.setImageResource(R.drawable.baseline_panorama_fish_eye_24)
            binding.card2.setBackgroundResource(R.drawable.stroke_change)
        }


        binding.card1.setOnClickListener {
            subscribeProduct("standard")
        }
        binding.card2.setOnClickListener {
            subscribeProduct("pro")
        }


    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) { //購入が完了した時に呼び出される処理
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {//ユーザーが既に所有している商品を再度購入しようとしたとき
            Toast.makeText(this, R.string.billing_message_item, Toast.LENGTH_SHORT).show()
            when (basePlanId) {
                "standard" -> PreferenceHelper.setBoolean("key_purchased_standard", true)
                "pro" -> PreferenceHelper.setBoolean("key_purchased_pro", true)
            }
            reloadScreen()
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            Toast.makeText(this, R.string.billing_message_support, Toast.LENGTH_SHORT).show()
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, R.string.billing_message_cancel, Toast.LENGTH_SHORT).show()
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
            Toast.makeText(this, R.string.billing_message_unavailable, Toast.LENGTH_SHORT).show()
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR) {
            Toast.makeText(this, R.string.billing_message_network, Toast.LENGTH_SHORT).show()
        } else {
            val message = getString(R.string.billing_message_error) + billingResult.debugMessage
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun handlePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val listener = ConsumeResponseListener { billingResult, s ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            }
        }
        billingClient!!.consumeAsync(consumeParams, listener)
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {  //購入の状態を確認
            if (!verifyvalidSignature(
                    purchase.originalJson,
                    purchase.signature
                )
            ) { //ID、商品のID、購入のタイムスタンプ,デジタル署名,改ざんされていないかのチェック
                Toast.makeText(this, R.string.billing_message_invalid, Toast.LENGTH_SHORT).show()
                return
            }

            if (!purchase.isAcknowledged) { //購入がまだアプリによって確認されていない場合
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)  //Token購入を一意に識別
                    .build()
                billingClient!!.acknowledgePurchase(    //購入が完了したことをGoogle Playに通知
                    acknowledgePurchaseParams,
                    acknowledgePurchaseResponseListener
                )
            } else {
                Toast.makeText(this, R.string.billing_message_item, Toast.LENGTH_SHORT).show()
                when (basePlanId) {
                    "standard" -> PreferenceHelper.setBoolean("key_purchased_standard", true)
                    "pro" -> PreferenceHelper.setBoolean("key_purchased_pro", true)
                }
                reloadScreen()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Toast.makeText(this, R.string.billing_message_pending, Toast.LENGTH_SHORT).show()
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Toast.makeText(this, R.string.billing_message_unspecified, Toast.LENGTH_SHORT).show()
        }
    }

    var acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {     //購入の確認が成功した
            when (basePlanId) {
                "standard" -> {
                    if (!PreferenceHelper.getBoolean("key_purchased_pro", false)) {
                        PreferenceHelper.setBoolean("key_purchased_standard", true)
                    }
                }
                "pro" -> {
                    if (!PreferenceHelper.getBoolean("key_purchased_standard", false)) {
                        PreferenceHelper.setBoolean("key_purchased_pro", true)
                    }
                }
            }
            reloadScreen()
        }
    }

    private fun verifyvalidSignature(signedData: String, signature: String): Boolean {
        return try {
            val security = Security()
            val base64key = getString(R.string.key_base64key)
            security.verifyPurchase(base64key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }


    private fun reloadScreen() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        queryPurchase()
    }
    fun queryPurchase() {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute {
                    billingClient?.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    ) { billingResult, purchasesList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            purchasesList?.forEach { purchase ->
                                var product =
                                    purchase.accountIdentifiers?.obfuscatedProfileId ?: null
                                when (product) {
                                    "standard" -> {
                                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                            val acknowledgePurchaseParams =
                                                AcknowledgePurchaseParams.newBuilder()
                                                    .setPurchaseToken(purchase.purchaseToken)
                                                    .build()

                                            billingClient?.acknowledgePurchase(
                                                acknowledgePurchaseParams,
                                                acknowledgePurchaseResponseListener
                                            )
                                            PreferenceHelper.setBoolean(
                                                "key_purchased_standard",
                                                true
                                            )
                                            binding.purchasedImageview.apply {
                                                setImageResource(R.drawable.baseline_check_circle_24)
                                                binding.card1.setBackgroundResource(R.drawable.stroke_change_to)
                                            }
                                            PreferenceHelper.setBoolean("key_purchased_pro", false)
                                            binding.purchasedProImageview2.apply {
                                                setImageResource(R.drawable.baseline_panorama_fish_eye_24)
                                                binding.card1.setBackgroundResource(R.drawable.stroke_change)
                                            }
                                        }
                                    }
                                    "pro" -> {
                                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                            val acknowledgePurchaseParams =
                                                AcknowledgePurchaseParams.newBuilder()
                                                    .setPurchaseToken(purchase.purchaseToken)
                                                    .build()

                                            billingClient?.acknowledgePurchase(
                                                acknowledgePurchaseParams,
                                                acknowledgePurchaseResponseListener
                                            )
                                            PreferenceHelper.setBoolean("key_purchased_pro", true)
                                            binding.purchasedProImageview2.apply {
                                                setImageResource(R.drawable.baseline_check_circle_24)
                                                binding.card2.setBackgroundResource(R.drawable.stroke_change_to)
                                            }
                                            PreferenceHelper.setBoolean(
                                                "key_purchased_standard",
                                                false
                                            )
                                            binding.purchasedImageview.apply {
                                                setImageResource(R.drawable.baseline_panorama_fish_eye_24)
                                                binding.card2.setBackgroundResource(R.drawable.stroke_change)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                runOnUiThread {
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    fun subscribeProduct(sku: String) {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productList = listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("subsc")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                    billingClient!!.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
                        for (productDetails in productDetailsList) {        //製品詳細情報のリスト
                            val offerToken =
                                productDetails.subscriptionOfferDetails?.find { it.basePlanId == sku }
                            val productDetailsParamsList = listOf(
                                offerToken?.let {
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(it.offerToken)
                                        .build()
                                }
                            )
                            val billingFlowProductParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .setObfuscatedAccountId(sku) // obfuscatedProfileId を設定するには、obfuscatedAccountIdも指定しておく必要がある
                                .setObfuscatedProfileId(sku)
                                .build()
                            val billingResult =
                                billingClient!!.launchBillingFlow(this@Subs, billingFlowProductParams)
                            basePlanId = sku
                        }
                    }
                }
            }

        })
    }


    override fun onDestroy() {
        super.onDestroy()
        if (billingClient != null) {
            billingClient!!.endConnection()
        }
    }


}