package tw.nekomimi.nekogram.utils

import okhttp3.Request


fun Request.Builder.applyUserAgent(): Request.Builder {

    header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1")

    return this

}