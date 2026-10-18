package com.privatednsguard.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.privatednsguard.data.repository.DnsGuardRepository

class DnsAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "PrivateDNS Guard Anti-Uninstall Protection Activated", Toast.LENGTH_SHORT).show()
        DnsGuardRepository.getInstance(context).refreshState()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling this will allow PrivateDNS Guard to be uninstalled, potentially removing your DNS security."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Anti-Uninstall Protection Deactivated", Toast.LENGTH_SHORT).show()
        DnsGuardRepository.getInstance(context).refreshState()
    }
}
