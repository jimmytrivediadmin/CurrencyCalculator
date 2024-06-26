package jimmytrivedi.`in`.currencyconvertor.domain.remote.data.exchangerate

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExchangeRateResponse(
    val error: Boolean = false,
    val status: Int?,
    val message: String?,
    var data: Data?,
) : Parcelable