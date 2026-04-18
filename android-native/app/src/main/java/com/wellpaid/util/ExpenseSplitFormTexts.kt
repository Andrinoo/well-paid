package com.wellpaid.util

import com.wellpaid.core.model.expense.ExpenseDto

data class ExpenseSplitFormTexts(
    val ownerShareText: String,
    val peerShareText: String,
    val usePercentSplit: Boolean,
    val ownerPercentText: String,
    val peerPercentDisplayText: String,
)

/**
 * Textos iniciais para o formulário ao editar uma despesa partilhada (dono).
 */
fun splitTextsForExpenseEdit(e: ExpenseDto): ExpenseSplitFormTexts {
    if (!e.isShared || !e.isMine) {
        return ExpenseSplitFormTexts("", "", false, "", "")
    }
    val total = e.amountCents
    val my = e.myShareCents ?: ((total + 1) / 2)
    val other = e.otherUserShareCents ?: (total - my)
    val ownerStr = centsToBrlInput(my)
    val peerStr = centsToBrlInput(other)
    if (e.splitMode == "percent" && total > 0) {
        val ob = e.ownerPercentBps
        if (ob != null) {
            val ot = ExpenseSplitFormMath.bpsToBrPercentText(ob)
            val pt = ExpenseSplitFormMath.bpsToBrPercentText(10000 - ob)
            return ExpenseSplitFormTexts(ownerStr, peerStr, true, ot, pt)
        }
        val derivedBps = ((my.toLong() * 10000L) / total).toInt().coerceIn(0, 10000)
        val ot = ExpenseSplitFormMath.bpsToBrPercentText(derivedBps)
        val pt = ExpenseSplitFormMath.bpsToBrPercentText(10000 - derivedBps)
        return ExpenseSplitFormTexts(ownerStr, peerStr, true, ot, pt)
    }
    return ExpenseSplitFormTexts(ownerStr, peerStr, false, "", "")
}
