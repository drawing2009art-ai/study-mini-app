package com.piptrack.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object DataTransfer {
    suspend fun exportJson(context: Context, uri: Uri, trades: List<Trade>) {
        val root = JSONObject()
        root.put("format", "piptrack-backup")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val array = JSONArray()
        trades.forEach { t ->
            array.put(JSONObject().apply {
                put("id", t.id); put("instrument", t.instrument); put("direction", t.direction)
                put("openedAt", t.openedAt); put("entryPrice", t.entryPrice)
                put("exitPrice", t.exitPrice ?: JSONObject.NULL); put("pipSize", t.pipSize)
                put("manualPips", t.manualPips ?: JSONObject.NULL); put("riskPips", t.riskPips ?: JSONObject.NULL)
                put("strategy", t.strategy); put("notes", t.notes); put("isClosed", t.isClosed)
                put("currentPrice", t.currentPrice ?: JSONObject.NULL); put("timeframe", t.timeframe)
                put("session", t.session); put("setupType", t.setupType); put("followedPlan", t.followedPlan ?: JSONObject.NULL)
                put("grossMoney", t.grossMoney ?: JSONObject.NULL); put("netMoney", t.netMoney ?: JSONObject.NULL)
                put("currency", t.currency); put("commission", t.commission ?: JSONObject.NULL)
                put("swap", t.swap ?: JSONObject.NULL); put("otherFees", t.otherFees ?: JSONObject.NULL)
                put("closedAt", t.closedAt ?: JSONObject.NULL); put("timezoneId", t.timezoneId)
            })
        }
        root.put("trades", array)
        context.contentResolver.openOutputStream(uri)?.use { out -> OutputStreamWriter(out).use { it.write(root.toString(2)) } }
            ?: error("Unable to open destination")
    }

    suspend fun exportCsv(context: Context, uri: Uri, trades: List<Trade>) {
        val header = "id,instrument,direction,openedAt,entryPrice,exitPrice,pipSize,resultPips,riskPips,strategy,session,timeframe,netMoney,currency,isClosed,notes"
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream).use { w ->
                w.appendLine(header)
                trades.forEach { t ->
                    val values = listOf(t.id, t.instrument, t.direction, t.openedAt, t.entryPrice,
                        t.exitPrice ?: "", t.pipSize, t.resultPips() ?: "", t.riskPips ?: "",
                        t.strategy, t.session, t.timeframe, t.netMoney ?: "", t.currency, t.isClosed, t.notes)
                    w.appendLine(values.joinToString(",") { csv(it.toString()) })
                }
            }
        } ?: error("Unable to open destination")
    }
    private fun csv(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    suspend fun importJson(context: Context, uri: Uri, dao: TradeDao) {
        val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: error("Unable to read backup")
        val root = JSONObject(content)
        require(root.optString("format") == "piptrack-backup") { "This is not a PipTrack backup" }
        val array = root.getJSONArray("trades")
        val imported = mutableListOf<Trade>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            fun nullableDouble(key: String) = if (o.isNull(key)) null else o.optDouble(key)
            imported += Trade(
                instrument = o.getString("instrument"), direction = o.getString("direction"),
                openedAt = o.getLong("openedAt"), entryPrice = o.getDouble("entryPrice"),
                exitPrice = nullableDouble("exitPrice"), pipSize = o.getDouble("pipSize"),
                manualPips = nullableDouble("manualPips"), riskPips = nullableDouble("riskPips"),
                strategy = o.optString("strategy"), notes = o.optString("notes"),
                isClosed = o.optBoolean("isClosed", true), currentPrice = nullableDouble("currentPrice"),
                timeframe = o.optString("timeframe"), session = o.optString("session"),
                setupType = o.optString("setupType"), grossMoney = nullableDouble("grossMoney"),
                netMoney = nullableDouble("netMoney"), currency = o.optString("currency"),
                commission = nullableDouble("commission"), swap = nullableDouble("swap"),
                otherFees = nullableDouble("otherFees"), closedAt = if (o.isNull("closedAt")) null else o.optLong("closedAt"),
                timezoneId = o.optString("timezoneId", java.util.TimeZone.getDefault().id)
            )
        }
        // Restore is additive; auto-generated IDs avoid overwriting current local records.
        dao.insertAll(imported)
    }
}
