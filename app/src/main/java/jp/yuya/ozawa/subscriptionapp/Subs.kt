package jp.yuya.ozawa.subscriptionapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.billingclient.api.*
import jp.yuya.ozawa.subscriptionapp.databinding.ActivityMainBinding
import jp.yuya.ozawa.subscriptionapp.databinding.ActivitySubsBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class Subs : AppCompatActivity() {
    var productId = -1
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

        binding.purchasedImageview.apply {
            if (PreferenceHelper.getBoolean("key_purchased_standard", false)) {
                //            product.text = getString(R.string.billing_message_item)
                setImageResource(R.drawable.baseline_check_circle_24)
                binding.card1.setBackgroundResource(R.drawable.stroke_change_to)
            } else {
                setImageResource(R.drawable.baseline_panorama_fish_eye_24)
                binding.card1.setBackgroundResource(R.drawable.stroke_change)
            }
        }
        binding.purchasedProImageview2.apply {
            if (PreferenceHelper.getBoolean("key_purchased_pro", false)) {
                //            product.text = getString(R.string.billing_message_item)
                setImageResource(R.drawable.baseline_check_circle_24)
                binding.card2.setBackgroundResource(R.drawable.stroke_change_to)
            } else {
                setImageResource(R.drawable.baseline_panorama_fish_eye_24)
                binding.card2.setBackgroundResource(R.drawable.stroke_change)
            }
        }

        binding.card1.setOnClickListener {
            val index = binding.cardContainer.indexOfChild(binding.card1)
            subscribeProduct(index)
        }
        binding.card2.setOnClickListener {
            val index = binding.cardContainer.indexOfChild(binding.card2)
            subscribeProduct(index)
        }


    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) { //購入が完了した時に呼び出される処理
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {//ユーザーが既に所有している商品を再度購入しようとしたとき
            Toast.makeText(this, R.string.billing_message_item, Toast.LENGTH_SHORT).show()
            when (productId) {
                0 -> PreferenceHelper.setBoolean("key_purchased_standard", true)
                1 -> PreferenceHelper.setBoolean("key_purchased_pro", true)
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
            if (!verifyvalidSignature(purchase.originalJson, purchase.signature)) { //ID、商品のID、購入のタイムスタンプ,デジタル署名,改ざんされていないかのチェック
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
                Toast.makeText(this,  R.string.billing_message_item, Toast.LENGTH_SHORT).show()
                when (productId) {
                    0 -> PreferenceHelper.setBoolean("key_purchased_standard", true)
                    1 -> PreferenceHelper.setBoolean("key_purchased_pro", true)
                }
                reloadScreen()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Toast.makeText(this,  R.string.billing_message_pending, Toast.LENGTH_SHORT).show()
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Toast.makeText(this, R.string.billing_message_unspecified, Toast.LENGTH_SHORT).show()
        }
    }

    var acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {     //購入の確認が成功した
            when (productId) {
                0 -> PreferenceHelper.setBoolean("key_purchased_standard", true)
                1 -> PreferenceHelper.setBoolean("key_purchased_pro", true)
            }
            reloadScreen()
        }
    }

    private fun verifyvalidSignature(signedData: String, signature: String): Boolean {
        return try {
            val security = Security()
           val base64key =getString(R.string.key_base64key)
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

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchasesList) {
                    val sku = getSkuFromPurchaseJson(purchase.originalJson)
                    when (sku) {
                        "standard" -> {
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.purchaseToken)
                                    .build()

                                billingClient?.acknowledgePurchase(
                                    acknowledgePurchaseParams,
                                    acknowledgePurchaseResponseListener
                                )
                                PreferenceHelper.setBoolean("key_purchased_standard", true)
                                binding.purchasedImageview.apply {
                                    setImageResource(R.drawable.baseline_check_circle_24)
                                    binding.card1.setBackgroundResource(R.drawable.stroke_change_to)
                                }
                                PreferenceHelper.setBoolean("key_purchased_pro", false)
                                binding.purchasedImageview.apply {
                                    setImageResource(R.drawable.baseline_panorama_fish_eye_24)
                                    binding.card1.setBackgroundResource(R.drawable.stroke_change)
                                }
                            }
                        }
                        "pro" -> {
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
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
                                PreferenceHelper.setBoolean("key_purchased_standard", false)
                                binding.purchasedProImageview2.apply {
                                    setImageResource(R.drawable.baseline_panorama_fish_eye_24)
                                    binding.card2.setBackgroundResource(R.drawable.stroke_change)}
                            }
                        }
                    }
                }
            }
        }
    }
    fun getSkuFromPurchaseJson(purchaseJson: String): String? {
        return try {
            val jsonObject = JSONObject(purchaseJson)
            jsonObject.getString("productId")
        } catch (e: JSONException) {
            null
        }
    }
    fun subscribeProduct(index : Int) {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productList = listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("testsub")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                    billingClient!!.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
                        for (productDetails in productDetailsList) {        //製品詳細情報のリスト
                            val offerToken =
                                productDetails.subscriptionOfferDetails?.get(index)?.offerToken
                            val productDetailsParamsList = listOf(
                                offerToken?.let {
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(it)
                                        .build()
                                }
                            )
                            val billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build()
                            val billingResult =
                                billingClient!!.launchBillingFlow(this@Subs, billingFlowParams)
                            productId = index
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
