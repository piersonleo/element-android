package com.vcard.vchat.mesh

import com.vcard.vchat.mesh.data.MeshCurrency
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

enum class CurrencyEnum(val prefix: String){
    MeshGold(Constants.MeshGoldCurrency),
    MeshCoin(Constants.MeshCoinCurrency)
}

object TxnFee {

    fun calculateTotalFee(currencyEnum: CurrencyEnum, amount: Long): BigInteger{
       val feeTable = getCurrency(currencyEnum)

        var sum = BigInteger("0")

        var meshFee = getPercentageAmount(amount, feeTable.meshFeePercentage)

        if (meshFee > feeTable.meshFeeCap){
            meshFee = feeTable.meshFeeCap
        }

        // If calculated fee is lower than the minimum fee, set it to minimum fee..
        if (meshFee < feeTable.meshFeeMinimum){
            meshFee = feeTable.meshFeeMinimum
        }

        sum = sum.add(meshFee)

        var communityFee = getPercentageAmount(amount, feeTable.communityFeePercentage)

        if (communityFee > feeTable.communityFeeCap){
            communityFee = feeTable.communityFeeCap
        }

        // If calculated fee is lower than the minimum fee, set it to minimum fee..
        if (communityFee < feeTable.communityFeeMinimum){
            communityFee = feeTable.communityFeeMinimum
        }

        sum = sum.add(communityFee)

        var nodeFee = getPercentageAmount(amount, feeTable.nodeFeePercentage)

        if (nodeFee > feeTable.nodeFeeCap){
            nodeFee = feeTable.nodeFeeCap
        }

        // If calculated fee is lower than the minimum fee, set it to minimum fee..
        if (nodeFee < feeTable.nodeFeeMinimum){
            nodeFee = feeTable.nodeFeeMinimum
        }

        sum = sum.add(nodeFee)

        return sum
    }

    fun calculateTotalFeeBigInt(currencyEnum: CurrencyEnum, amount: BigInteger): BigInteger{
        val feeTable = getCurrency(currencyEnum)

        var sum = BigInteger("0")

        var meshFee = getPercentageAmountBigInt(amount, feeTable.meshFeePercentage)

        if (meshFee > feeTable.meshFeeCap){
            meshFee = feeTable.meshFeeCap
        }

        // If calculated fee is lower than the minimum fee, set it to minimum fee..
        if (meshFee < feeTable.meshFeeMinimum){
            meshFee = feeTable.meshFeeMinimum
        }

        sum = sum.add(meshFee)

        var communityFee = getPercentageAmountBigInt(amount, feeTable.communityFeePercentage)

        if (communityFee > feeTable.communityFeeCap){
            communityFee = feeTable.communityFeeCap
        }

        // If calculated fee is lower than the minimum fee, set it to minimum fee..
        if (communityFee < feeTable.communityFeeMinimum){
            communityFee = feeTable.communityFeeMinimum
        }

        sum = sum.add(communityFee)

        var nodeFee = getPercentageAmountBigInt(amount, feeTable.nodeFeePercentage)

        if (nodeFee > feeTable.nodeFeeCap){
            nodeFee = feeTable.nodeFeeCap
        }

        // If calculated fee is lower than the minimum fee, set it to minimum fee..
        if (nodeFee < feeTable.nodeFeeMinimum){
            nodeFee = feeTable.nodeFeeMinimum
        }

        sum = sum.add(nodeFee)

        return sum
    }

    private fun getPercentageAmount(amount: Long, percentage: BigInteger): BigInteger {
        val amountBigInt = BigInteger.valueOf(amount)

        val percentageDivider = BigInteger("10000")

        return amountBigInt.multiply(percentage).divide(percentageDivider)
    }


    private fun getPercentageAmountBigInt(amount: BigInteger, percentage: BigInteger): BigInteger {

        val percentageDivider = BigInteger("10000")

        return amount.multiply(percentage).divide(percentageDivider)
    }


    private fun getCurrency(currencyEnum: CurrencyEnum): MeshCurrency{
        return when (currencyEnum){
            CurrencyEnum.MeshGold -> MeshCurrency(
                    Constants.MeshGoldCurrency,
                    BigInteger("9"),
                    BigInteger("333333000"),
                    BigInteger("1000"),
                    BigInteger("9"),
                    BigInteger("333334000"),
                    BigInteger("1000"),
                    BigInteger("9"),
                    BigInteger("333333000"),
                    BigInteger("1000")

            )
            CurrencyEnum.MeshCoin -> MeshCurrency(
                    Constants.MeshCoinCurrency,
                    BigInteger("10"),
                    BigInteger("500000000"),
                    BigInteger("1000"),
                    BigInteger("10"),
                    BigInteger("500000000"),
                    BigInteger("1000"),
                    BigInteger("10"),
                    BigInteger("500000000"),
                    BigInteger("1000")
            )
        }
    }
}
