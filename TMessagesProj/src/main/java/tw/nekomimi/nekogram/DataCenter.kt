package tw.nekomimi.nekogram

import android.graphics.Color
import org.telegram.tgnet.ConnectionsManager

object DataCenter {

    @JvmStatic
    fun applyOfficalDataCanter(account: Int) {

        if (ConnectionsManager.getInstance(account).isTestBackend) {
            ConnectionsManager.getInstance(account).switchBackend(false)
        }

        ConnectionsManager.native_cleanUp(account, true)

    }

    @JvmStatic
    fun applyTestDataCenter(account: Int) {

        if (!ConnectionsManager.getInstance(account).isTestBackend) {
            ConnectionsManager.getInstance(account).switchBackend(false)
        }

    }

    @JvmStatic
    fun toggleCustomDataCenter(account: Int) {
        ConnectionsManager.getInstance(account).switchCustomBackend(false)
    }

}