package org.egility.android.tools

import android.net.wifi.WifiConfiguration
import java.lang.reflect.InvocationTargetException
import java.net.InetAddress


object IpObject {
    
    // https://stackoverflow.com/questions/10278461/how-to-configure-a-static-ip-address-netmask-gateway-programmatically-on-andro

/*
    @Throws(SecurityException::class, IllegalArgumentException::class, NoSuchFieldException::class, IllegalAccessException::class)
    fun setIpAssignment(assign: String, wifiConf: WifiConfiguration) {
        setEnumField(wifiConf, assign, "ipAssignment")
    }

    @Throws(SecurityException::class, IllegalArgumentException::class, NoSuchFieldException::class, IllegalAccessException::class, NoSuchMethodException::class, ClassNotFoundException::class, InstantiationException::class, InvocationTargetException::class)
    fun setIpAddress(addr: InetAddress, prefixLength: Int, wifiConf: WifiConfiguration) {
        val linkProperties = getField(wifiConf, "linkProperties") ?: return
        val laClass = Class.forName("android.net.LinkAddress")
        val laConstructor =
            laClass.getConstructor(*arrayOf<Class<*>>(InetAddress::class.java, Int::class.javaPrimitiveType!!))
        val linkAddress = laConstructor.newInstance(addr, prefixLength)

        val mLinkAddresses = getDeclaredField(linkProperties, "mLinkAddresses") as ArrayList<*>
        mLinkAddresses.clear()
        mLinkAddresses.add(linkAddress)
    }

    @Throws(SecurityException::class, IllegalArgumentException::class, NoSuchFieldException::class, IllegalAccessException::class, ClassNotFoundException::class, NoSuchMethodException::class, InstantiationException::class, InvocationTargetException::class)
    fun setGateway(gateway: InetAddress, wifiConf: WifiConfiguration) {
        val linkProperties = getField(wifiConf, "linkProperties") ?: return
        val routeInfoClass = Class.forName("android.net.RouteInfo")
        val routeInfoConstructor = routeInfoClass.getConstructor(*arrayOf(InetAddress::class.java))
        val routeInfo = routeInfoConstructor.newInstance(gateway)

        val mRoutes = getDeclaredField(linkProperties, "mRoutes") as ArrayList<*>
        mRoutes.clear()
        mRoutes.add(routeInfo)
    }

    @Throws(SecurityException::class, IllegalArgumentException::class, NoSuchFieldException::class, IllegalAccessException::class)
    fun setDNS(dns: InetAddress, wifiConf: WifiConfiguration) {
        val linkProperties = getField(wifiConf, "linkProperties") ?: return

        val mDnses = getDeclaredField(linkProperties, "mDnses") as ArrayList<InetAddress>
        mDnses.clear() //or add a new dns address , here I just want to replace DNS1
        mDnses.add(dns)
    }

    @Throws(SecurityException::class, NoSuchFieldException::class, IllegalArgumentException::class, IllegalAccessException::class)
    fun getField(obj: Any, name: String): Any? {
        val f = obj.javaClass.getField(name)
        return f.get(obj)
    }

    @Throws(SecurityException::class, NoSuchFieldException::class, IllegalArgumentException::class, IllegalAccessException::class)
    fun getDeclaredField(obj: Any, name: String): Any {
        val f = obj.javaClass.getDeclaredField(name)
        f.isAccessible = true
        return f.get(obj)
    }

    @Throws(SecurityException::class, NoSuchFieldException::class, IllegalArgumentException::class, IllegalAccessException::class)
    private fun setEnumField(obj: Any, value: String, name: String) {
        val f = obj.javaClass.getField(name)
        f.set(obj, Enum.valueOf<Enum<*>>(f.type as Class<Enum<*>>, value))
    }
    
 */
    
}